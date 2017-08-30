package com.ms_square.debugoverlay;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.ColorInt;
import android.support.annotation.FloatRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.ms_square.debugoverlay.modules.CpuUsageModule;
import com.ms_square.debugoverlay.modules.FpsModule;
import com.ms_square.debugoverlay.modules.MemInfoModule;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class DebugOverlay {

    private static final String TAG = "DebugOverlay";

    public static final Position DEFAULT_POSITION = Position.BOTTOM_START;
    public static final int DEFAULT_BG_COLOR = Color.parseColor("#40000000");
    public static final int DEFAULT_TEXT_COLOR = Color.WHITE;
    public static final float DEFAULT_TEXT_SIZE = 12f; // 12sp
    public static final float DEFAULT_TEXT_ALPHA = 1f;

    static final String KEY_CONFIG = "com.ms_square.debugoverlay.extra.CONFIG";

    static final String ACTION_UNBIND = "com.ms_square.debugoverlay.ACTION_UNBIND";

    static boolean DEBUG = false;

    private final Application application;

    private final List<OverlayModule> overlayModules;

    private final Config config;

    private DebugOverlayService overlayService;

    private OverlayViewManager overlayViewManager;

    private ActivityLifecycleHandler activityLifecycleHandler;

    private boolean installed;

    private boolean unBindRequestReceived;

    private DebugOverlay(Application application, List<OverlayModule> overlayModules, Config config) {
        this.application = application;
        this.overlayModules = overlayModules;
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

    public void install() {
        if (installed) {
            throw new IllegalStateException("install() can be called only once!");
        }

        overlayViewManager = new OverlayViewManager(application, config);
        overlayViewManager.setOverlayModules(overlayModules);

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
            overlayService.setOverlayModules(overlayModules);
            overlayService.setOverlayViewManager(overlayViewManager);
            overlayService.startModules();
        }
        // This is called when the connection with the service has been
        // unexpectedly disconnected -- that is, its process crashed.
        // So, this is not called when the client unbinds.
        @Override
        public void onServiceDisconnected(ComponentName name) {}
    };

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

        private List<OverlayModule> overlayModules;

        private Position position;

        @ColorInt
        private int bgColor;

        @ColorInt
        private int textColor;

        private float textSize;

        private float textAlpha;

        private boolean allowSystemLayer;

        private boolean showNotification;

        private String activityName;

        public Builder(@NonNull Application application) {
            this.application = application;

            // default values
            this.position = DEFAULT_POSITION;
            this.bgColor = DEFAULT_BG_COLOR;
            this.textColor = DEFAULT_TEXT_COLOR;
            this.textSize = DEFAULT_TEXT_SIZE;
            this.textAlpha = DEFAULT_TEXT_ALPHA;
            this.allowSystemLayer = true;
            this.showNotification = true;
            this.overlayModules = new ArrayList<>();
        }

        public Builder modules(@NonNull List<OverlayModule> overlayModules) {
            if (overlayModules.size() <= 0) {
                throw new IllegalArgumentException("Module list cat not be empty");
            }
            this.overlayModules = overlayModules;
            return this;
        }

        public Builder modules(@NonNull OverlayModule overlayModule, OverlayModule... other) {
            this.overlayModules.clear();
            this.overlayModules.add(overlayModule);
            for (OverlayModule otherModule : other) {
                if (otherModule != null) {
                    this.overlayModules.add(otherModule);
                }
            }
            return this;
        }

        public Builder position(Position position) {
            this.position = position;
            return this;
        }

        public Builder bgColor(@ColorInt int color) {
            this.bgColor = color;
            return this;
        }

        public Builder textColor(@ColorInt int color) {
            this.textColor = color;
            return this;
        }

        public Builder textSize(float size) {
            this.textSize = size;
            return this;
        }

        public Builder textAlpha(@FloatRange(from=0.0, to=1.0) float alpha) {
            this.textAlpha = alpha;
            return this;
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
            if (overlayModules.size() == 0) {
                overlayModules.add(new CpuUsageModule());
                overlayModules.add(new MemInfoModule(application));
                overlayModules.add(new FpsModule());
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Removes any CpuUsageModule if a device is running Android O and above
                Iterator<OverlayModule> iterator = overlayModules.iterator();
                while (iterator.hasNext()) {
                    if (iterator.next() instanceof CpuUsageModule) {
                        iterator.remove();
                    }
                }
            }

            return new DebugOverlay(application, overlayModules,
                    new Config(position, bgColor, textColor, textSize, textAlpha, allowSystemLayer,
                            showNotification, activityName));
        }
    }

    static class Config implements Parcelable {

        private final Position position;

        @ColorInt
        private final int bgColor;

        @ColorInt
        private final int textColor;

        private final float textSize;

        private final float textAlpha;

        private final boolean allowSystemLayer;

        private final boolean showNotification;

        private final String activityName;

        public Config(Position position, @ColorInt int bgColor, @ColorInt int textColor, float textSize,
                      float textAlpha, boolean allowSystemLayer, boolean showNotification, String activityName) {
            this.position = position;
            this.bgColor = bgColor;
            this.textColor = textColor;
            this.textSize = textSize;
            this.textAlpha = textAlpha;
            this.allowSystemLayer = allowSystemLayer;
            this.showNotification = showNotification;
            this.activityName = activityName;
        }

        public Position getPosition() {
            return position;
        }

        public int getBgColor() {
            return bgColor;
        }

        public int getTextColor() {
            return textColor;
        }

        public float getTextSize() {
            return textSize;
        }

        public float getTextAlpha() {
            return textAlpha;
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
            dest.writeInt(this.position == null ? -1 : this.position.ordinal());
            dest.writeInt(this.bgColor);
            dest.writeInt(this.textColor);
            dest.writeFloat(this.textSize);
            dest.writeFloat(this.textAlpha);
            dest.writeByte(this.allowSystemLayer ? (byte) 1 : (byte) 0);
            dest.writeByte(this.showNotification ? (byte) 1 : (byte) 0);
            dest.writeString(this.activityName);
        }

        protected Config(Parcel in) {
            int tmpPosition = in.readInt();
            this.position = tmpPosition == -1 ? null : Position.values()[tmpPosition];
            this.bgColor = in.readInt();
            this.textColor = in.readInt();
            this.textSize = in.readFloat();
            this.textAlpha = in.readFloat();
            this.allowSystemLayer = in.readByte() != 0;
            this.showNotification = in.readByte() != 0;
            this.activityName = in.readString();
        }

        public static final Parcelable.Creator<Config> CREATOR = new Parcelable.Creator<Config>() {
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
