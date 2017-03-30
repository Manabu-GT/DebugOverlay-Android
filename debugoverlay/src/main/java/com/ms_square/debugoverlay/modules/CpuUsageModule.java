package com.ms_square.debugoverlay.modules;

import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.support.annotation.NonNull;
import android.util.Log;

import com.ms_square.debugoverlay.OverlayViewDelegateFactory;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

public class CpuUsageModule extends BaseOverlayModule<CpuUsageModule.CpuUsage> {

    private static final String TAG = CpuUsageModule.class.getSimpleName();

    public static final int DEFAULT_INTERVAL = 1000; // 1000ms

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final AtomicReference<CpuUsage> cpuUsage = new AtomicReference<>();

    private ReaderThread cpuReaderThread;

    public CpuUsageModule() {
        this(DEFAULT_INTERVAL);
    }

    public CpuUsageModule(int interval) {
        super(interval);
    }

    public CpuUsageModule(@NonNull OverlayViewDelegateFactory<CpuUsage> viewDelegateFactory) {
        this(DEFAULT_INTERVAL, viewDelegateFactory);
    }

    public CpuUsageModule(int interval, @NonNull OverlayViewDelegateFactory<CpuUsage> viewDelegateFactory) {
        super(interval, viewDelegateFactory);
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

    private final Runnable notifyObserversRunnable = new Runnable() {
        @Override
        public void run() {
            notifyObservers();
        }
    };

    @Override
    protected OverlayViewDelegateFactory<CpuUsage> getDefaultFactory() {
        return new CpuUsageViewDelegate.Factory();
    }

    @Override
    protected CpuUsage getLatestData() {
        return cpuUsage.get();
    }

    class ReaderThread extends Thread {

        private BufferedReader totalCpuReader;

        private BufferedReader myPidCpuReader;

        /* Jiffy is a unit of CPU time. */
        private long totalJiffies;

        private long totalJiffiesBefore;

        private long jiffies;

        private long jiffiesBefore;

        private long jiffiesMyPid;

        private long jiffiesMyPidBefore;

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                openCpuReaders();
                read();
                closeCpuReaders();
                try {
                    Thread.currentThread().sleep(getInterval());
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        public void cancel() {
            interrupt();
        }

        private void openCpuReaders() {
            if (totalCpuReader == null) {
                try {
                    totalCpuReader = new BufferedReader(new FileReader("/proc/stat"));
                } catch (FileNotFoundException e) {
                    Log.w(TAG, "Could not open '/proc/stat' - " + e.getMessage());
                }
            }
            if (myPidCpuReader == null) {
                try {
                    myPidCpuReader = new BufferedReader(new FileReader("/proc/" + Process.myPid() + "/stat"));
                } catch (FileNotFoundException e) {
                    Log.w(TAG, "Could not open '/proc/" + Process.myPid() + "/stat' - " + e.getMessage());
                }
            }
        }

        // Ref... Section 1.8 in https://www.kernel.org/doc/Documentation/filesystems/proc.txt and manpage of proc
        private void read() {
            if (totalCpuReader != null) {
                try {
                    String[] cpuData = totalCpuReader.readLine().split("[ ]+", 9);
                    Log.d(TAG, "CPU total:" + Arrays.toString(cpuData));
                    // add user, nice, and system
                    jiffies = Long.parseLong(cpuData[1]) + Long.parseLong(cpuData[2]) + Long.parseLong(cpuData[3]);
                    // ignore 'iowait' value since it is not reliable
                    totalJiffies = jiffies + Long.parseLong(cpuData[4]) + Long.parseLong(cpuData[6]) + Long.parseLong(cpuData[7]);
                } catch (IOException ie) {
                    Log.w(TAG, "Failed reading total cpu data - " + ie.getMessage());
                }
            }

            if (myPidCpuReader != null) {
                try {
                    String[] cpuData = myPidCpuReader.readLine().split("[ ]+", 18);
                    Log.d(TAG, "CPU for mypid:" + Arrays.toString(cpuData));
                    // add utime, stime, cutime, and cstime
                    jiffiesMyPid = Long.parseLong(cpuData[13]) + Long.parseLong(cpuData[14])
                            + Long.parseLong(cpuData[15]) + Long.parseLong(cpuData[16]);
                } catch (IOException ie) {
                    Log.w(TAG, "Failed reading my pid cpu data - " + ie.getMessage());
                }
            }

            if (totalJiffiesBefore > 0) {
                long totalDiff = totalJiffies - totalJiffiesBefore;
                long jiffiesDiff = jiffies - jiffiesBefore;
                long jiffiesMyPidDiff = jiffiesMyPid - jiffiesMyPidBefore;

                cpuUsage.set(new CpuUsage(getPercentInRange(100f * jiffiesDiff / totalDiff),
                        getPercentInRange(100f * jiffiesMyPidDiff / totalDiff)));

                handler.post(notifyObserversRunnable);
            }

            totalJiffiesBefore = totalJiffies;
            jiffiesBefore = jiffies;
            jiffiesMyPidBefore = jiffiesMyPid;
        }

        private void closeCpuReaders() {
            try {
                if (totalCpuReader != null) {
                    totalCpuReader.close();
                    totalCpuReader = null;
                }
                if (myPidCpuReader != null) {
                    myPidCpuReader.close();
                    myPidCpuReader = null;
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

    public static class CpuUsage {
        private final double total;
        private final double myPid;

        public CpuUsage(double total, double myPid) {
            this.total = total;
            this.myPid = myPid;
        }

        public double getTotal() {
            return total;
        }

        public double getMyPid() {
            return myPid;
        }
    }
}
