package com.ms_square.debugoverlay_ext_timber;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.ms_square.debugoverlay.modules.BaseDataModule;
import com.ms_square.debugoverlay.modules.LogcatLine;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import timber.log.Timber;

class TimberDataModule extends BaseDataModule<LogcatLine> {

    private static final int LINE_UPDATED = Integer.MAX_VALUE - 100 + 1;

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("MM-dd", Locale.US);
    private static final DateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);

    private final Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message m) {
            if (m.what == LINE_UPDATED) {
                latestLine = (LogcatLine) m.obj;
                notifyObservers();
            }
        }
    };

    private final Timber.Tree overlayLogTree = new Timber.DebugTree() {
        @Override
        protected void log(final int priority, final String tag, final String message, Throwable t) {
            if (started) {
                handler.obtainMessage(LINE_UPDATED, new LogcatLine(getCurrentDate(), getCurrentTime(),
                        0, 0, getLogcatLinePriority(priority), tag, message)).sendToTarget();
            } else {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        handler.obtainMessage(LINE_UPDATED, new LogcatLine(getCurrentDate(), getCurrentTime(),
                                0, 0, getLogcatLinePriority(priority), tag, message)).sendToTarget();
                    }
                });
            }
        }
    };

    private LogcatLine latestLine;

    private boolean started;

    /**
     * Pass BuildConfig.DEBUG not to plant a tree in the release build.
     * @param isDebug - should be your BuildConfig.DEBUG
     */
    public TimberDataModule(boolean isDebug) {
        super();
        if (isDebug) {
            // try not to miss logs which are emitted during the application start up
            Timber.plant(overlayLogTree);
        }
    }

    @Override
    public void start() {
        started = true;
        if (!Timber.forest().contains(overlayLogTree)) {
            Timber.plant(overlayLogTree);
        }
    }

    @Override
    public void stop() {
        Timber.uproot(overlayLogTree);
        handler.removeMessages(LINE_UPDATED);
        started = false;
    }

    @Override
    protected LogcatLine getLatestData() {
        return latestLine;
    }

    private static String getCurrentDate() {
        return DATE_FORMAT.format(new Date());
    }

    private static String getCurrentTime() {
        return TIME_FORMAT.format(new Date());
    }

    private static LogcatLine.Priority getLogcatLinePriority(int priority) {
        if (Log.VERBOSE == priority) {
            return LogcatLine.Priority.VERBOSE;
        } else if (Log.DEBUG == priority) {
            return LogcatLine.Priority.DEBUG;
        } else if (Log.INFO == priority) {
            return LogcatLine.Priority.INFO;
        } else if (Log.WARN == priority) {
            return LogcatLine.Priority.WARNING;
        } else if (Log.ERROR == priority) {
            return LogcatLine.Priority.ERROR;
        } else if (Log.ASSERT == priority) {
            return LogcatLine.Priority.ASSERT;
        } else {
            return LogcatLine.Priority.SILENT;
        }
    }
}
