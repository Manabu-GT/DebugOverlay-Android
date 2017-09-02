package com.ms_square.debugoverlay.modules;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static java.lang.Runtime.getRuntime;

class LogcatDataModule extends BaseDataModule<LogcatLine> {

    private static final String TAG = "LogcatDataModule";

    private static final int LINE_UPDATED = 10000;

    private final Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message m) {
            if (m.what == LINE_UPDATED) {
                latestLine = (LogcatLine) m.obj;
                notifyObservers();
            }
        }
    };

    private ReaderThread logcatReaderThread;

    private LogcatLine latestLine;

    public LogcatDataModule() {
        super(0);
    }

    @Override
    public void start() {
        if (logcatReaderThread == null || !logcatReaderThread.isAlive()) {
            logcatReaderThread = new ReaderThread();
            logcatReaderThread.start();
        }
    }

    @Override
    public void stop() {
        handler.removeMessages(LINE_UPDATED);
        if (logcatReaderThread != null) {
            logcatReaderThread.cancel();
            try {
                logcatReaderThread.join();
            } catch (InterruptedException ignore) {}
            logcatReaderThread = null;
        }
    }

    @Override
    protected LogcatLine getLatestData() {
        return latestLine;
    }

    class ReaderThread extends Thread {

        private Process logcatProcess;
        private BufferedReader logcatReader;

        @Override
        public void run() {
            try {
                clearLogcatBuffer();
                openLogcatProcess();
                openLogcatReader();
                while (!Thread.currentThread().isInterrupted()) {
                    String logLine;
                    try {
                        logLine = logcatReader.readLine();
                    } catch (IOException ie) {
                        Log.w(TAG, "Failed reading logcat message; allow thread to exit - " + ie.getMessage());
                        break;
                    }
                    if (logLine != null && logLine.length() > 0 && !logLine.startsWith("--------- beginning of ")) {
                        handler.obtainMessage(LINE_UPDATED, new LogcatLine(logLine)).sendToTarget();
                    }
                }
            } finally {
                closeLogcatProcess();
                closeLogcatReader();
            }
        }

        public void cancel() {
            interrupt();
            closeLogcatProcess();
        }

        private void openLogcatProcess() {
            synchronized (this) {
                if (logcatProcess == null) {
                    try {
                        logcatProcess = Runtime.getRuntime().exec(new String[]{"logcat", "-v", "threadtime"});
                    } catch (IOException e) {
                        Log.w(TAG, "Can not execute logcat - " + e.getMessage());
                    }
                }
            }
        }

        private void closeLogcatProcess() {
            synchronized (this) {
                if (logcatProcess != null) {
                    logcatProcess.destroy();
                    logcatProcess = null;
                }
            }
        }

        private void openLogcatReader() {
            synchronized (this) {
                if (logcatProcess != null) {
                    if (logcatReader == null) {
                        logcatReader = new BufferedReader(new InputStreamReader(logcatProcess.getInputStream()));
                    }
                }
            }
        }

        private void closeLogcatReader() {
            try {
                if (logcatReader != null) {
                    logcatReader.close();
                    logcatReader = null;
                }
            } catch (IOException ignore) {}
        }

        private void clearLogcatBuffer() {
            Process process = null;
            try {
                process = getRuntime().exec(new String[] {"logcat", "-c"});
                process.waitFor();
            } catch (InterruptedException | IOException e) {
                Log.w(TAG, "Clearing logcat buffer failed - " + e.getMessage());
            } finally {
                if (process != null) {
                    process.destroy();
                }
            }
        }
    }
}
