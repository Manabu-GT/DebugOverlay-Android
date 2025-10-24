package com.ms_square.debugoverlay;

import android.app.NotificationChannel;
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
import android.os.Build;
import android.os.IBinder;
import android.content.pm.ServiceInfo;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.Collections;
import java.util.List;

public class DebugOverlayService extends Service {

    private static final String TAG = "DebugOverlayService";

    private static final String NOTIFICATION_CHANNEL_ID = "com.ms_square.debugoverlay";
    private static final int NOTIFICATION_ID = Integer.MAX_VALUE - 100;

    private static final String ACTION_SHOW_SUFFIX = ".debugoverlay_ACTION_SHOW";
    private static final String ACTION_HIDE_SUFFIX = ".debugoverlay_ACTION_HIDE";

    private final IBinder binder = new LocalBinder();

    private DebugOverlay.Config config;

    private List<OverlayModule<?>> overlayModules = Collections.emptyList();

    private OverlayViewManager overlayViewManager;

    private NotificationManager notificationManager;

    private boolean modulesStarted;

    private String actionShow = "";
    private String actionHide = "";

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
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        createNotificationChannel();

        String packageName = getPackageName();
        actionShow = packageName + ACTION_SHOW_SUFFIX;
        actionHide = packageName + ACTION_HIDE_SUFFIX;

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(actionShow);
        intentFilter.addAction(actionHide);
        ContextCompat.registerReceiver(this, receiver, intentFilter, ContextCompat.RECEIVER_EXPORTED);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        config = intent.getParcelableExtra(DebugOverlay.KEY_CONFIG);
        // no need to restart this service
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(receiver);
        cancelNotification();
        stopModules();
        overlayViewManager.hideDebugSystemOverlay();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        stopSelf();
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcastSync(new Intent(DebugOverlay.ACTION_UNBIND));
    }

    public void setOverlayModules(@NonNull List<OverlayModule<?>> overlayModules) {
        this.overlayModules = overlayModules;
    }

    void setOverlayViewManager(@NonNull OverlayViewManager overlayViewManager) {
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
            for (OverlayModule<?> overlayModule : overlayModules) {
                overlayModule.start();
            }
            modulesStarted = true;
        }
    }

    public void stopModules() {
        if (modulesStarted) {
            for (OverlayModule<?> overlayModule : overlayModules) {
                overlayModule.stop();
            }
            modulesStarted = false;
        }
    }

    public void updateNotification() {
        showNotification();
    }

    private void createNotificationChannel() {
        if (notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null) {
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                    getString(R.string.debugoverlay_notification_channel_name), NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void showNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(getString(R.string.debugoverlay_notification_big_text)))
                .setSmallIcon(R.drawable.debugoverlay_ic_notification)
                .setLargeIcon(getAppIcon(this))
                .setOngoing(true)
                .setContentTitle(getString(R.string.debugoverlay_notification_title, getAppName(this), getAppVersion(this)))
                .setContentText(getString(R.string.debugoverlay_notification_small_text))
                .setContentIntent(getLaunchIntent());
        if (overlayViewManager.isSystemOverlayShown()) {
            builder.addAction(R.drawable.debugoverlay_ic_action_pause, getString(R.string.debugoverlay_notification_action_hide),
                    getBroadcastIntent(actionHide));
        } else {
            builder.addAction(R.drawable.debugoverlay_ic_action_play, getString(R.string.debugoverlay_notification_action_show),
                    getBroadcastIntent(actionShow));
        }

        // show the notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, builder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, builder.build());
        }
    }

    private void cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID);
    }

    private PendingIntent getBroadcastIntent(String action) {
        Intent intent = new Intent(action);
        return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private PendingIntent getLaunchIntent() {
        PendingIntent pendingIntent = null;
        if (config.getActivityName() != null) {
            try {
                Intent intent = new Intent(this, Class.forName(config.getActivityName()));
                pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            } catch (ClassNotFoundException ne) {
                Log.w(TAG, config.getActivityName() + " was not found - " + ne.getMessage());
            }
        }
        return pendingIntent;
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (actionShow.equals(action)) {
                overlayViewManager.showDebugSystemOverlay();
                startModules();
                // update notification
                showNotification();
            } else if (actionHide.equals(action)) {
                stopModules();
                overlayViewManager.hideDebugSystemOverlay();
                // update notification
                showNotification();
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
