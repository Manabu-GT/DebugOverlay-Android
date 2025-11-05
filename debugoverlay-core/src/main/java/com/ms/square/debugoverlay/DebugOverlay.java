package com.ms.square.debugoverlay;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class DebugOverlay {

    private static final String TAG = "DebugOverlay";

    static final String KEY_CONFIG = "com.ms_square.debugoverlay.extra.CONFIG";

    static final String ACTION_UNBIND = "com.ms_square.debugoverlay.ACTION_UNBIND";

    static boolean DEBUG = false;

    private final Application application;

    private final Config config;

    private DebugOverlayService overlayService;

    private OverlayViewManager overlayViewManager;

    private ActivityLifecycleHandler activityLifecycleHandler;

    private boolean installed;

    private boolean unBindRequestReceived;

    private DebugOverlay(Application application, Config config) {
        this.application = application;
        this.config = config;
    }

    /**
     * Convenience method to create the default {@link DebugOverlay} instance.
     * <p>
     * This instance is automatically initialized with the following default settings.
     * <ul>
     *     <li>Overlay is placed at BOTTOM_START (bottom left)</li>
     *     <li>Overlay's background color is black of opacity 25%</li>
     *     <li>Overlay's textColor is white.</li>
     *     <li>Overlay's textSize is 12sp.</li>
     *     <li>Overlay's textAlpha is 1 (opaque).</li>
     *     <li>Overlay is placed on System window layer.</li>
     *     <li>Notification is shown to control(show/hide) the overlay.</li>
     *     <li>Activity to start when the fore-mentioned notification is tapped is null; thus does nothing when tapped.</li>
     * </ul>
     * <p>
     * If these settings do not meet the requirements of your application you can construct your own
     * with full control over the configuration by using {@link DebugOverlay.Builder} to create a
     * {@link DebugOverlay} instance.
     *
     * @param application
     * @return
     */
    public static DebugOverlay with(@NonNull Application application) {
        return new Builder(application).build();
    }

    /**
     * Control whether the DebugOverlay's internal debugging logs are turned on.
     * If enabled, you will see output in logcat as the components of DebugOverlay operates.
     */
    public static void enableDebugLogging(boolean enabled) {
        DEBUG = enabled;
    }

    /**
     * Tells whether the DebugOverlay's internal debugging logs are turned on.
     * @return true if the DebugOverlay's internal debugging logs are enabled.
     */
    public static boolean isDebugLoggingEnabled() {
        return DEBUG;
    }

    @MainThread
    public void install() {
        if (installed) {
            throw new IllegalStateException("install() can be called only once!");
        }
        if (!isMainProcess(application)) {
            // just return early without any work if it's not running in the main app process.
            return;
        }

        overlayViewManager = new OverlayViewManager(application, config);

        startAndBindDebugOverlayService();

        activityLifecycleHandler = new ActivityLifecycleHandler();
        application.registerActivityLifecycleCallbacks(activityLifecycleHandler);

        installed = true;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    void uninstall() {
        unbindFromDebugOverlayService();
        application.stopService(DebugOverlayService.createIntent(application));
        application.unregisterActivityLifecycleCallbacks(activityLifecycleHandler);
        installed = false;
    }

    private void startAndBindDebugOverlayService() {
        // start & bind DebugOverlayService
        Intent intent = new Intent(application, DebugOverlayService.class);
        intent.putExtra(KEY_CONFIG, config);
        application.startService(intent);
        bindToDebugOverlayService();
    }

    private void bindToDebugOverlayService() {
        boolean bound = application.bindService(DebugOverlayService.createIntent(application),
                serviceConnection, Context.BIND_AUTO_CREATE);
        if (!bound) {
            throw new RuntimeException("Could not bind the DebugOverlayService");
        }
        LocalBroadcastManager.getInstance(application).registerReceiver(receiver, new IntentFilter(ACTION_UNBIND));
    }

    private void unbindFromDebugOverlayService() {
        if (overlayService != null) {
            application.unbindService(serviceConnection);
            overlayService = null;
        }
        LocalBroadcastManager.getInstance(application).unregisterReceiver(receiver);
    }

    final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (DEBUG) {
                Log.i(TAG, "DebugOverlayService is connected");
            }
            // We've bound to DebugOverlayService, cast the IBinder and get DebugOverlayService instance
            DebugOverlayService.LocalBinder binder = (DebugOverlayService.LocalBinder) service;
            overlayService = binder.getService();
            overlayService.setOverlayViewManager(overlayViewManager);
            overlayService.startModules();
        }
        // This is called when the connection with the service has been
        // unexpectedly disconnected -- that is, its process crashed.
        // So, this is not called when the client unbinds.
        @Override
        public void onServiceDisconnected(ComponentName name) {}
    };

    // returns true if the current process is the main process (matches the initial application pid),
    // ; otherwise false.
    private static boolean isMainProcess(Application application) {
        String mainProcessName = application.getPackageName();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return mainProcessName.equals(Application.getProcessName());
        }
        String currentProcessName = getProcessName(application);
        if (currentProcessName == null) {
            // treat the process as main when the name cannot be determined.
            return true;
        }
        return mainProcessName.equals(currentProcessName);
    }

    // a fallback way to get the current process name on older android OSs, should get a
    // name like "com.package.name"(main process name) or "com.package.name:remote"
    @Nullable
    private static String getProcessName(Application application) {
        int myPid = android.os.Process.myPid();
        ActivityManager am = (ActivityManager) application.getSystemService(Context.ACTIVITY_SERVICE);
        @Nullable
        List<ActivityManager.RunningAppProcessInfo> infos = am.getRunningAppProcesses();
        if (infos != null) {
            for(ActivityManager.RunningAppProcessInfo info : infos) {
                if (info.pid == myPid) {
                    return info.processName;
                }
            }
        }
        // may never return null
        return null;
    }

    final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_UNBIND.equals(intent.getAction())) {
                if (DEBUG) {
                    Log.d(TAG, "DebugOverlayService unbind request received");
                }
                unBindRequestReceived = true;
                unbindFromDebugOverlayService();
            }
        }
    };

    public static class Builder {

        private final Application application;


        private boolean allowSystemLayer;

        private boolean showNotification;

        private String activityName;

        public Builder(@NonNull Application application) {
            this.application = application;

            // default values
            this.allowSystemLayer = true;
            this.showNotification = true;
        }

        public Builder allowSystemLayer(boolean allowSystemLayer) {
            this.allowSystemLayer = allowSystemLayer;
            return this;
        }

        public Builder notification(boolean show) {
            this.showNotification = show;
            return this;
        }

        public Builder notification(boolean show, @Nullable String activityName) {
            this.showNotification = show;
            this.activityName = activityName;
            return this;
        }

        public DebugOverlay build() {
            if (!allowSystemLayer) {
                if (showNotification) {
                    Log.w(TAG, "if systemLayer is not allowed, notification is not supported; thus don't show notification.");
                    showNotification = false;
                }
            }
            return new DebugOverlay(application,
                    new Config(allowSystemLayer, showNotification, activityName));
        }
    }

    static class Config implements Parcelable {

        private final boolean allowSystemLayer;

        private final boolean showNotification;

        private final String activityName;

        public Config(boolean allowSystemLayer, boolean showNotification, String activityName) {
            this.allowSystemLayer = allowSystemLayer;
            this.showNotification = showNotification;
            this.activityName = activityName;
        }

        public boolean isAllowSystemLayer() {
            return allowSystemLayer;
        }

        public boolean isShowNotification() {
            return showNotification;
        }

        public String getActivityName() {
            return activityName;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeByte(this.allowSystemLayer ? (byte) 1 : (byte) 0);
            dest.writeByte(this.showNotification ? (byte) 1 : (byte) 0);
            dest.writeString(this.activityName);
        }

        protected Config(Parcel in) {
            this.allowSystemLayer = in.readByte() != 0;
            this.showNotification = in.readByte() != 0;
            this.activityName = in.readString();
        }

        public static final Parcelable.Creator<Config> CREATOR = new Parcelable.Creator<>() {
          @Override
          public Config createFromParcel(Parcel source) {
            return new Config(source);
          }

          @Override
          public Config[] newArray(int size) {
            return new Config[size];
          }
        };
    }

    class ActivityLifecycleHandler implements Application.ActivityLifecycleCallbacks {

        private Map<Activity, OverlayViewManager.OverlayViewAttachStateChangeListener> attachStateChangeListeners;

        private int numRunningActivities;

        public ActivityLifecycleHandler() {
            if (!config.isAllowSystemLayer()) {
                attachStateChangeListeners = new WeakHashMap<>();
            }
        }

        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            if (DEBUG) {
                Log.i(TAG, "onCreate():" + activity.getClass().getSimpleName());
            }
            if (!config.isAllowSystemLayer()) {
                OverlayViewManager.OverlayViewAttachStateChangeListener listener =
                        overlayViewManager.createAttachStateChangeListener();
                activity.getWindow().getDecorView().addOnAttachStateChangeListener(listener);
                attachStateChangeListeners.put(activity, listener);
            }
        }

        @Override
        public void onActivityStarted(Activity activity) {
            if (DEBUG) {
                Log.i(TAG, "onStart():" + activity.getClass().getSimpleName());
            }
            incrementNumRunningActivities();
        }

        @Override
        public void onActivityResumed(Activity activity) {
            if (DEBUG) {
                Log.i(TAG, "onResume():" + activity.getClass().getSimpleName());
            }
            if (config.isAllowSystemLayer()) {
                if (overlayViewManager.isOverlayPermissionRequested() &&
                        OverlayViewManager.canDrawOnSystemLayer(activity, OverlayViewManager.getWindowTypeForOverlay(true))) {
                    overlayViewManager.showDebugSystemOverlay();
                    if (overlayService != null) {
                        overlayService.updateNotification();
                    }
                }
            } else {
                OverlayViewManager.OverlayViewAttachStateChangeListener listener = attachStateChangeListeners.get(activity);
                if (listener != null) {
                    listener.onActivityResumed();
                }
            }
        }

        @Override
        public void onActivityPaused(Activity activity) {
            if (DEBUG) {
                Log.i(TAG, "onPause():" + activity.getClass().getSimpleName());
            }
        }

        @Override
        public void onActivityStopped(Activity activity) {
            if (DEBUG) {
                Log.i(TAG, "onStop():" + activity.getClass().getSimpleName());
            }
            decrementNumRunningActivities();
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
            if (DEBUG) {
                Log.i(TAG, "onSaveInstanceState():" + activity.getClass().getSimpleName());
            }
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
            if (DEBUG) {
                Log.i(TAG, "onDestroy():" + activity.getClass().getSimpleName());
            }
            if (attachStateChangeListeners != null) {
                attachStateChangeListeners.remove(activity);
            }
        }

        private void incrementNumRunningActivities() {
            if (numRunningActivities == 0) {
                // app is in foreground
                if (config.isAllowSystemLayer()) {
                    if (overlayService == null && unBindRequestReceived) {
                        // service already un-bound by a explicit request, but restart here since it is now in foreground
                        startAndBindDebugOverlayService();
                        unBindRequestReceived = false;
                    }
                } else {
                    // restart modules since it may have been stopped
                    if (overlayService != null) {
                        overlayService.startModules();
                    }
                }
            }
            numRunningActivities++;
        }

        private void decrementNumRunningActivities() {
            numRunningActivities--;
            if (numRunningActivities <= 0) {
                numRunningActivities = 0;
                // apps is in background
                if (!config.isAllowSystemLayer() && overlayService != null) {
                    overlayService.stopModules();
                }
            }
        }
    }
}
