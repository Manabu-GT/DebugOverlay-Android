package com.ms.square.debugoverlay

import android.app.ActivityManager
import android.app.Application
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.os.Parcelable
import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.ms.square.debugoverlay.internal.ActivityOverlayViewManager
import com.ms.square.debugoverlay.internal.Logger
import com.ms.square.debugoverlay.internal.OverlayViewManager
import com.ms.square.debugoverlay.internal.SystemOverlayViewManager
import kotlinx.parcelize.Parcelize

class DebugOverlay private constructor(private val application: Application, private val config: Config) {

  internal var overlayService: DebugOverlayService? = null
  internal var unBindRequestReceived = false

  private var overlayViewManager: OverlayViewManager? = null
  private var activityLifecycleCallbacks: Application.ActivityLifecycleCallbacks? = null
  private var installed = false

  @MainThread
  fun install() {
    check(!installed) { "install() can be called only once!" }

    if (!isMainProcess(application)) {
      // Just return early without any work if it's not running in the main app process
      return
    }

    if (config.allowSystemLayer) {
      overlayViewManager = SystemOverlayViewManager(application)
      startAndBindDebugOverlayService()
    } else {
      overlayViewManager = ActivityOverlayViewManager(application)
    }

    activityLifecycleCallbacks = overlayViewManager?.createActivityLifecycleCallbacks(this).also {
      application.registerActivityLifecycleCallbacks(it)
    }

    installed = true
  }

  @VisibleForTesting(otherwise = VisibleForTesting.NONE)
  internal fun uninstall() {
    unbindFromDebugOverlayService()
    application.stopService(DebugOverlayService.createIntent(application))
    activityLifecycleCallbacks?.let {
      application.unregisterActivityLifecycleCallbacks(it)
    }
    installed = false
  }

  internal fun startAndBindDebugOverlayService() {
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
      Logger.d("DebugOverlayService is connected")
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
    override fun onServiceDisconnected(name: ComponentName) = Unit
  }

  private val receiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      if (ACTION_UNBIND == intent.action) {
        Logger.d("DebugOverlayService unbind request received")
        unBindRequestReceived = true
        unbindFromDebugOverlayService()
      }
    }
  }

  @Parcelize
  internal data class Config(val allowSystemLayer: Boolean, val activityName: String?) : Parcelable

  companion object {

    internal const val KEY_CONFIG = "com.ms_square.debugoverlay.extra.CONFIG"
    internal const val ACTION_UNBIND = "com.ms_square.debugoverlay.ACTION_UNBIND"

    /**
     * Convenience method to create the [DebugOverlay] instance for its installation.
     */
    @JvmStatic
    fun with(application: Application): DebugOverlay {
      val allowSystemLayer = application.resources.getBoolean(R.bool.debugoverlay_use_system_layer)
      return DebugOverlay(application, Config(allowSystemLayer, getLauncherActivityName(application)))
    }

    private fun getLauncherActivityName(context: Context): String? {
      val pm = context.packageManager
      val launchIntent = pm.getLaunchIntentForPackage(context.packageName)
      return launchIntent?.component?.className
    }

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
