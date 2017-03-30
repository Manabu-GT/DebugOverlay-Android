package com.ms_square.debugoverlay.modules;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;

import com.ms_square.debugoverlay.OverlayViewDelegateFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicReference;

public class LogcatModule extends BaseOverlayModule<LogcatLine> {

    private static final String TAG = LogcatModule.class.getSimpleName();

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

    public LogcatModule() {
        super(0);
    }

    public LogcatModule(@NonNull OverlayViewDelegateFactory<LogcatLine> viewDelegateFactory) {
        super(0, viewDelegateFactory);
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
    protected OverlayViewDelegateFactory<LogcatLine> getDefaultFactory() {
        return new LogcatViewDelegate.Factory();
    }

    @Override
    protected LogcatLine getLatestData() {
        return latestLine;
    }

    class ReaderThread extends Thread {

        private AtomicReference<Process> logcatProcess = new AtomicReference<>();
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
            try {
                logcatProcess.compareAndSet(null, Runtime.getRuntime().exec(new String[]{"logcat", "-v", "threadtime"}));
            } catch (IOException e) {
                Log.w(TAG, "Can not execute logcat - " + e.getMessage());
            }
        }

        private void openLogcatReader() {
            Process process = logcatProcess.get();
            if (process != null) {
                if (logcatReader == null) {
                    logcatReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                }
            }
        }

        private void closeLogcatProcess() {
            Process process = logcatProcess.getAndSet(null);
            if (process != null) {
                process.destroy();
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
                process = Runtime.getRuntime().exec(new String[] {"logcat", "-c"});
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
