package com.ms.square.debugoverlay.modules;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.system.Os;
import android.system.OsConstants;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

class CpuUsageDataModule extends BaseDataModule<CpuUsage> {

    private static final String TAG = "CpuUsageDataModule";

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final AtomicReference<CpuUsage> cpuUsage = new AtomicReference<>();

    private final long numCpuCores = Os.sysconf(OsConstants._SC_NPROCESSORS_CONF);

    /**
     * A way for user-space applications to find out the granularity of the system's software clock,
     * which is maintained by the kernel and measures time in units called "jiffies."
     * It basically returns # of ticks per second.
     */
    private final long ticksPerSecond = Os.sysconf(OsConstants._SC_CLK_TCK);

    private ReaderThread cpuReaderThread;

    private final int interval;

    public CpuUsageDataModule(int interval) {
        this.interval = interval;
    }

    @Override
    public void start() {
        if (cpuReaderThread == null) {
            cpuReaderThread = new ReaderThread();
            cpuReaderThread.start();
        }
    }

    @Override
    public void stop() {
        handler.removeCallbacks(notifyObserversRunnable);
        if (cpuReaderThread != null) {
            cpuReaderThread.cancel();
            try {
                cpuReaderThread.join();
            } catch (InterruptedException ignore) {}
            cpuReaderThread = null;
        }
    }

    private final Runnable notifyObserversRunnable = this::notifyObservers;

    @Override
    protected CpuUsage getLatestData() {
        return cpuUsage.get();
    }

    private class ReaderThread extends Thread {

        private BufferedReader myProcessCpuReader;

        // time since the application started in seconds.
        private double processStartTimeSec;

        private double processTime1Sec;
        private double processTime2Sec;

        private double myCpuTime1Sec;

        private double myCpuTime2Sec;

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                openCpuReader();
                read();
                closeCpuReader();
                try {
                    Thread.currentThread().sleep(interval);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        public void cancel() {
            interrupt();
        }

        private void openCpuReader() {
            if (myProcessCpuReader == null) {
                try {
                    myProcessCpuReader = new BufferedReader(new FileReader("/proc/self/stat"));
                } catch (FileNotFoundException e) {
                    Log.w(TAG, "Could not open '/proc/self/stat' - " + e.getMessage());
                }
            }
        }

        // Ref... Section 1.8 in https://www.kernel.org/doc/Documentation/filesystems/proc.txt and manpage of proc
        private void read() {
            if (myProcessCpuReader != null) {
                try {
                    String[] cpuData = myProcessCpuReader.readLine().split(" +", 23);
                    // add utime, stime, cutime, and cstime
                    double cpuTimeTicks = Double.parseDouble(cpuData[13]) + Double.parseDouble(cpuData[14])
                            + Double.parseDouble(cpuData[15]) + Double.parseDouble(cpuData[16]);
                    myCpuTime2Sec = cpuTimeTicks / ticksPerSecond;
                    // only set once since it won't change over time.
                    if (processStartTimeSec == 0) {
                        // starttime — the time the process started after system boot, measured in clock ticks.
                        processStartTimeSec = Double.parseDouble(cpuData[21]) / ticksPerSecond;
                    }
                    processTime2Sec = SystemClock.elapsedRealtime() / 1000.0;
                } catch (IOException ie) {
                    Log.w(TAG, "Failed reading my pid cpu data - " + ie.getMessage());
                }
            }

            if (processStartTimeSec > 0 && myCpuTime1Sec > 0 && processTime1Sec > 0) {
                double upTimeSec = SystemClock.elapsedRealtime() / 1000.0;
                double processTimeSec = upTimeSec - processStartTimeSec;
                // total avg usage percent for this application
                double totalAvgUsagePercent = (100 * (myCpuTime2Sec / processTimeSec)) / numCpuCores;

                double myCpuTimeDiffSec = myCpuTime2Sec - myCpuTime1Sec;
                double processTimeDiffSec = processTime2Sec - processTime1Sec;
                // relative avg usage percent for this application during the interval.
                double relAvgUsagePercent = (100 * (myCpuTimeDiffSec / processTimeDiffSec)) / numCpuCores;

                cpuUsage.set(new CpuUsage(getPercentInRange(totalAvgUsagePercent),
                        getPercentInRange(relAvgUsagePercent)));

                handler.post(notifyObserversRunnable);
            }
            myCpuTime1Sec = myCpuTime2Sec;
            processTime1Sec = processTime2Sec;
        }

        private void closeCpuReader() {
            try {
                if (myProcessCpuReader != null) {
                    myProcessCpuReader.close();
                    myProcessCpuReader = null;
                }
            } catch (IOException ignore) {}
        }
    }

    private static double getPercentInRange(double percent) {
        if (percent > 100f) {
            return 100f;
        } else if (percent < 0f){
            return 0f;
        }
        return percent;
    }
}
