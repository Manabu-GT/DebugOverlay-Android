package com.ms_square.debugoverlay;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.Collections;
import java.util.List;

public class DebugOverlayService extends Service {

    private static final String TAG = DebugOverlayService.class.getSimpleName();

    private static final int NOTIFICATION_ID = 10000;

    private static final String ACTION_SHOW = "com.ms_square.debugoverlay.ACTION_SHOW";
    private static final String ACTION_HIDE = "com.ms_square.debugoverlay.ACTION_HIDE";

    private final IBinder binder = new LocalBinder();

    private DebugOverlay.Config config;

    private List<OverlayModule> overlayModules = Collections.EMPTY_LIST;

    private OverlayViewManager overlayViewManager;

    private NotificationManager notificationManager;

    private boolean modulesStarted;

    public static Intent createIntent(Context context) {
        return new Intent(context, DebugOverlayService.class);
    }

    public class LocalBinder extends Binder {
        DebugOverlayService getService() {
            // Return this instance of DebugOverlayService so clients can call public methods
            return DebugOverlayService.this;
        }
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate() called");
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_SHOW);
        intentFilter.addAction(ACTION_HIDE);
        registerReceiver(receiver, intentFilter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand() called");
        config = intent.getParcelableExtra(DebugOverlay.KEY_CONFIG);
        // no need to restart this service
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy() called");
        unregisterReceiver(receiver);
        cancelNotification();
        stopModules();
        overlayViewManager.hideDebugSystemOverlay();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind() called");
        return binder;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Log.d(TAG, "onTaskRemoved() called");
        stopSelf();
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcastSync(new Intent(DebugOverlay.ACTION_UNBIND));
    }

    public void setOverlayModules(@NonNull List<OverlayModule> overlayModules) {
        this.overlayModules = overlayModules;
    }

    public void setOverlayViewManager(@NonNull OverlayViewManager overlayViewManager) {
        this.overlayViewManager = overlayViewManager;
        if (config.isAllowSystemLayer()) {
            overlayViewManager.showDebugSystemOverlay();
            if (config.isShowNotification()) {
                showNotification();
            }
        }
    }

    public void startModules() {
        if (!modulesStarted) {
            for (OverlayModule overlayModule : overlayModules) {
                overlayModule.start();
            }
            modulesStarted = true;
            Log.d(TAG, "Started modules");
        }
    }

    public void stopModules() {
        if (modulesStarted) {
            for (OverlayModule overlayModule : overlayModules) {
                overlayModule.stop();
            }
            modulesStarted = false;
            Log.d(TAG, "Stopped modules");
        }
    }

    public void updateNotification() {
        showNotification();
    }

    private void showNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(getString(R.string.debug_notification_big_text)))
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(getAppIcon(this))
                .setOngoing(true)
                .setContentTitle(getString(R.string.debug_notification_title, getAppName(this), getAppVersion(this)))
                .setContentText(getString(R.string.debug_notification_small_text))
                .setContentIntent(getNotificationIntent(null));
        if (overlayViewManager.isSystemOverlayShown()) {
            builder.addAction(R.drawable.ic_action_pause, getString(R.string.debug_notification_action_hide),
                    getNotificationIntent(ACTION_HIDE));
        } else {
            builder.addAction(R.drawable.ic_action_play, getString(R.string.debug_notification_action_show),
                    getNotificationIntent(ACTION_SHOW));
        }

        // show the notification
        startForeground(NOTIFICATION_ID, builder.build());
    }

    private void cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID);
    }

    private PendingIntent getNotificationIntent(String action) {
        if (action == null) {
            PendingIntent pendingIntent = null;
            if (config.getActivityName() != null) {
                try {
                    Intent intent = new Intent(this, Class.forName(config.getActivityName()));
                    pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                } catch (ClassNotFoundException ne) {
                    Log.w(TAG, config.getActivityName() + " was not found - " + ne.getMessage());
                }
            }
            return pendingIntent;
        } else {
            Intent intent = new Intent(action);
            return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        }
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case ACTION_SHOW: {
                    startModules();
                    overlayViewManager.showDebugSystemOverlay();
                    // update notification
                    showNotification();
                    break;
                }
                case ACTION_HIDE: {
                    stopModules();
                    overlayViewManager.hideDebugSystemOverlay();
                    // update notification
                    showNotification();
                    break;
                }
            }
        }
    };

    @Nullable
    private static Bitmap getAppIcon(@NonNull Context context) {
        Drawable drawable = null;
        try {
            drawable = context.getPackageManager().getApplicationIcon(context.getPackageName());
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Package Not found:" + context.getPackageName());
        }
        return (drawable instanceof BitmapDrawable) ? ((BitmapDrawable) drawable).getBitmap() : null;
    }

    @NonNull
    private static String getAppName(@NonNull Context context) {
        PackageManager packageManager = context.getPackageManager();
        ApplicationInfo applicationInfo = null;
        try {
            applicationInfo = packageManager.getApplicationInfo(context.getPackageName(), 0);
        } catch (final PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Package Not found:" + context.getPackageName());
        }
        return applicationInfo != null ? packageManager.getApplicationLabel(applicationInfo).toString() : "Unknown";
    }

    @NonNull
    private static String getAppVersion(@NonNull Context context) {
        String version = "Unknown";
        try {
            version = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException ex) {
            Log.w(TAG, "Package Not found:" + context.getPackageName());
        }
        return version;
    }
}
