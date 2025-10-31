package com.ms.square.debugoverlay.modules;

import static java.lang.Double.parseDouble;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.ms.square.debugoverlay.DebugOverlay;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

class CpuFreqDataModule extends BaseDataModule<List<CpuFreq>> {

    private static final String TAG = "CpuFreqDataModule";

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final AtomicReference<List<CpuFreq>> cpuFreqList = new AtomicReference<>();

    private final Runnable notifyObserversRunnable = this::notifyObservers;

    /**
     * The cpufreq values are given in kHz.
     * https://www.kernel.org/doc/Documentation/cpu-freq/user-guide.txt
     */
    private final Runnable cpuFreqReadRunnable = new Runnable() {

        Map<File, CpuFreq> cachedFrequencies = new LinkedHashMap<>();

        @Override
        public void run() {
            if (cachedFrequencies.isEmpty()) {
                File[] cpuFiles = getCpuFiles();
                if (cpuFiles != null) {
                    for (File cpuFile : cpuFiles) {
                        double minFreq = -1f;
                        double maxFreq = -1f;
                        BufferedReader minFreqReader = null;
                        BufferedReader maxFreqReader = null;
                        try {
                            minFreqReader = new BufferedReader(new FileReader(cpuFile.getAbsolutePath() + "/cpufreq/cpuinfo_min_freq"));
                            maxFreqReader = new BufferedReader(new FileReader(cpuFile.getAbsolutePath() + "/cpufreq/cpuinfo_max_freq"));
                            minFreq = parseDouble(minFreqReader.readLine());
                            maxFreq = parseDouble(maxFreqReader.readLine());

                            if (DebugOverlay.isDebugLoggingEnabled()) {
                                Log.d(TAG, cpuFile.getName() + " minFreq(kHz):" + minFreq);
                                Log.d(TAG, cpuFile.getName() + " maxFreq(kHz):" + maxFreq);
                            }
                        } catch (IOException ie) {
                            Log.w(TAG, "Error reading the min/max cpufreq", ie);
                        } finally {
                            if (minFreqReader != null) {
                                try {
                                    minFreqReader.close();
                                } catch (IOException ignore) {}
                            }
                            if (maxFreqReader != null) {
                                try {
                                    maxFreqReader.close();
                                } catch (IOException ignore) {}
                            }
                        }
                        cachedFrequencies.put(cpuFile, new CpuFreq(cpuFile.getName(), minFreq, -1f, maxFreq));
                    }
                }
            }

            List<CpuFreq> newCpuFreqList = new ArrayList<>();

            for (File cpuFile : cachedFrequencies.keySet()) {
                CpuFreq cached = cachedFrequencies.get(cpuFile);
                double curFreq = -1f;
                BufferedReader curFreqReader = null;
                try {
                    curFreqReader = new BufferedReader(new FileReader(cpuFile.getAbsolutePath() + "/cpufreq/scaling_cur_freq"));
                    curFreq = parseDouble(curFreqReader.readLine());

                    if (DebugOverlay.isDebugLoggingEnabled()) {
                        Log.d(TAG, cpuFile.getName() + " curFreq(kHz):" + curFreq);
                    }

                } catch (IOException ie) {
                    Log.w(TAG, "Error reading the current cpufreq", ie);
                } finally {
                    if (curFreqReader != null) {
                        try {
                            curFreqReader.close();
                        } catch (IOException ignore) {}
                    }
                }

                newCpuFreqList.add(new CpuFreq(cached.getCpuName(), cached.getMinFreq(), curFreq, cached.getMaxFreq()));
            }

            cpuFreqList.set(Collections.unmodifiableList(newCpuFreqList));

            handler.post(notifyObserversRunnable);
        }
    };

    private ScheduledExecutorService executorService;

    private final int interval;

    public CpuFreqDataModule(int interval) {
        this.interval = interval;
    }

    @Override
    public void start() {
        if (executorService == null) {
            executorService = Executors.newSingleThreadScheduledExecutor();
            executorService.scheduleWithFixedDelay(cpuFreqReadRunnable, 0, interval, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void stop() {
        handler.removeCallbacks(notifyObserversRunnable);
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
                    Log.w(TAG, "ExecutorService did not terminate");
                }
            } catch (InterruptedException ie) {
                // Preserve interrupt status
                Thread.currentThread().interrupt();
            }
            executorService = null;
        }
    }

    @Override
    protected List<CpuFreq> getLatestData() {
        return cpuFreqList.get();
    }

    private static File[] getCpuFiles() {
        File dir = new File("/sys/devices/system/cpu/");
        return dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return Pattern.matches("cpu[0-9]+", file.getName());
            }
        });
    }
}
