package com.ms.square.debugoverlay

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Parcelable
import android.util.Log
import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.ms.square.debugoverlay.internal.OverlayViewManager
import kotlinx.parcelize.Parcelize
import java.util.WeakHashMap

class DebugOverlay private constructor(
    private val application: Application,
    private val config: Config
) {

    private var overlayService: DebugOverlayService? = null
    private var overlayViewManager: OverlayViewManager? = null
    private var activityLifecycleHandler: ActivityLifecycleHandler? = null
    private var installed = false
    private var unBindRequestReceived = false

    @MainThread
    fun install() {
        check(!installed) { "install() can be called only once!" }

        if (!isMainProcess(application)) {
            // Just return early without any work if it's not running in the main app process
            return
        }

        overlayViewManager = OverlayViewManager(application, config)
        startAndBindDebugOverlayService()

        activityLifecycleHandler = ActivityLifecycleHandler().also {
            application.registerActivityLifecycleCallbacks(it)
        }

        installed = true
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    internal fun uninstall() {
        unbindFromDebugOverlayService()
        application.stopService(DebugOverlayService.createIntent(application))
        activityLifecycleHandler?.let {
            application.unregisterActivityLifecycleCallbacks(it)
        }
        installed = false
    }

    private fun startAndBindDebugOverlayService() {
        // Start & bind DebugOverlayService
        val intent = Intent(application, DebugOverlayService::class.java).apply {
            putExtra(KEY_CONFIG, config)
        }
        application.startService(intent)
        bindToDebugOverlayService()
    }

    private fun bindToDebugOverlayService() {
        val bound = application.bindService(
            DebugOverlayService.createIntent(application),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
        check(bound) { "Could not bind the DebugOverlayService" }

        LocalBroadcastManager.getInstance(application)
            .registerReceiver(receiver, IntentFilter(ACTION_UNBIND))
    }

    private fun unbindFromDebugOverlayService() {
        overlayService?.let {
            application.unbindService(serviceConnection)
            overlayService = null
        }
        LocalBroadcastManager.getInstance(application).unregisterReceiver(receiver)
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            if (DEBUG) {
                Log.i(TAG, "DebugOverlayService is connected")
            }
            // We've bound to DebugOverlayService, cast the IBinder and get DebugOverlayService instance
            val binder = service as DebugOverlayService.LocalBinder
            overlayService = binder.service.apply {
                overlayViewManager?.let { setOverlayViewManager(it) }
                startModules()
            }
        }

        // This is called when the connection with the service has been
        // unexpectedly disconnected -- that is, its process crashed.
        // So, this is not called when the client unbinds.
        override fun onServiceDisconnected(name: ComponentName) {}
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ACTION_UNBIND == intent.action) {
                if (DEBUG) {
                    Log.d(TAG, "DebugOverlayService unbind request received")
                }
                unBindRequestReceived = true
                unbindFromDebugOverlayService()
            }
        }
    }

    @Parcelize
    data class Config(
        val allowSystemLayer: Boolean,
        val showNotification: Boolean,
        val activityName: String?
    ) : Parcelable

    class Builder(private val application: Application) {
        private var allowSystemLayer: Boolean = true
        private var showNotification: Boolean = true
        private var activityName: String? = null

        fun build(): DebugOverlay {
            var finalShowNotification = showNotification

            if (!allowSystemLayer && showNotification) {
                Log.w(TAG, "if systemLayer is not allowed, notification is not supported; thus don't show notification.")
                finalShowNotification = false
            }

            return DebugOverlay(
                application,
                Config(allowSystemLayer, finalShowNotification, activityName)
            )
        }
    }

    inner class ActivityLifecycleHandler : Application.ActivityLifecycleCallbacks {
        private val attachStateChangeListeners: MutableMap<Activity, OverlayViewManager.OverlayViewAttachStateChangeListener>? =
            if (!config.allowSystemLayer) WeakHashMap() else null

        private var numRunningActivities = 0

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            if (DEBUG) {
                Log.i(TAG, "onCreate():${activity.javaClass.simpleName}")
            }
            if (!config.allowSystemLayer) {
                val listener = overlayViewManager?.createAttachStateChangeListener()
                listener?.let {
                    activity.window.decorView.addOnAttachStateChangeListener(it)
                    attachStateChangeListeners?.put(activity, it)
                }
            }
        }

        override fun onActivityStarted(activity: Activity) {
            if (DEBUG) {
                Log.i(TAG, "onStart():${activity.javaClass.simpleName}")
            }
            incrementNumRunningActivities()
        }

        override fun onActivityResumed(activity: Activity) {
            if (DEBUG) {
                Log.i(TAG, "onResume():${activity.javaClass.simpleName}")
            }
            if (config.allowSystemLayer) {
                overlayViewManager?.let { manager ->
                    if (manager.isOverlayPermissionRequested() &&
                        OverlayViewManager.canDrawOnSystemLayer(activity, OverlayViewManager.getWindowTypeForOverlay(true))
                    ) {
                        manager.showDebugSystemOverlay()
                        overlayService?.updateNotification()
                    }
                }
            } else {
                attachStateChangeListeners?.get(activity)?.onActivityResumed()
            }
        }

        override fun onActivityPaused(activity: Activity) {
            if (DEBUG) {
                Log.i(TAG, "onPause():${activity.javaClass.simpleName}")
            }
        }

        override fun onActivityStopped(activity: Activity) {
            if (DEBUG) {
                Log.i(TAG, "onStop():${activity.javaClass.simpleName}")
            }
            decrementNumRunningActivities()
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
            if (DEBUG) {
                Log.i(TAG, "onSaveInstanceState():${activity.javaClass.simpleName}")
            }
        }

        override fun onActivityDestroyed(activity: Activity) {
            if (DEBUG) {
                Log.i(TAG, "onDestroy():${activity.javaClass.simpleName}")
            }
            attachStateChangeListeners?.remove(activity)
        }

        private fun incrementNumRunningActivities() {
            if (numRunningActivities == 0) {
                // App is in foreground
                if (config.allowSystemLayer) {
                    if (overlayService == null && unBindRequestReceived) {
                        // Service already un-bound by an explicit request, but restart here since it is now in foreground
                        startAndBindDebugOverlayService()
                        unBindRequestReceived = false
                    }
                } else {
                    // Restart modules since they may have been stopped
                    overlayService?.startModules()
                }
            }
            numRunningActivities++
        }

        private fun decrementNumRunningActivities() {
            numRunningActivities--
            if (numRunningActivities <= 0) {
                numRunningActivities = 0
                // App is in background
                if (!config.allowSystemLayer) {
                    overlayService?.stopModules()
                }
            }
        }
    }

    companion object {
        private const val TAG = "DebugOverlay"

        internal const val KEY_CONFIG = "com.ms_square.debugoverlay.extra.CONFIG"
        internal const val ACTION_UNBIND = "com.ms_square.debugoverlay.ACTION_UNBIND"

        @JvmStatic
        var DEBUG = false
            private set

        /**
         * Convenience method to create the default [DebugOverlay] instance.
         *
         * This instance is automatically initialized with the following default settings:
         * - Overlay is placed at BOTTOM_START (bottom left)
         * - Overlay's background color is black of opacity 25%
         * - Overlay's textColor is white
         * - Overlay's textSize is 12sp
         * - Overlay's textAlpha is 1 (opaque)
         * - Overlay is placed on System window layer
         * - Notification is shown to control(show/hide) the overlay
         * - Activity to start when the fore-mentioned notification is tapped is null; thus does nothing when tapped
         *
         * If these settings do not meet the requirements of your application you can construct your own
         * with full control over the configuration by using [Builder] to create a [DebugOverlay] instance.
         */
        @JvmStatic
        fun with(application: Application): DebugOverlay {
            return Builder(application).build()
        }

        /**
         * Control whether the DebugOverlay's internal debugging logs are turned on.
         * If enabled, you will see output in logcat as the components of DebugOverlay operates.
         */
        @JvmStatic
        fun enableDebugLogging(enabled: Boolean) {
            DEBUG = enabled
        }

        /**
         * Tells whether the DebugOverlay's internal debugging logs are turned on.
         * @return true if the DebugOverlay's internal debugging logs are enabled.
         */
        @JvmStatic
        fun isDebugLoggingEnabled(): Boolean = DEBUG

        // Returns true if the current process is the main process (matches the initial application pid)
        private fun isMainProcess(application: Application): Boolean {
            val mainProcessName = application.packageName
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                return mainProcessName == Application.getProcessName()
            }
            val currentProcessName = getProcessName(application) ?: return true
            return mainProcessName == currentProcessName
        }

        // A fallback way to get the current process name on older android OSs, should get a
        // name like "com.package.name"(main process name) or "com.package.name:remote"
        private fun getProcessName(application: Application): String? {
            val myPid = android.os.Process.myPid()
            val am = application.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val infos = am.runningAppProcesses ?: return null

            return infos.firstOrNull { it.pid == myPid }?.processName
        }
    }
}
