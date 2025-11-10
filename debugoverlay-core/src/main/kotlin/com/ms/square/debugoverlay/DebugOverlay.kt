package com.ms.square.debugoverlay

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Build
import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import com.ms.square.debugoverlay.internal.ActivityOverlayViewManager
import com.ms.square.debugoverlay.internal.OverlayViewManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class DebugOverlay private constructor(private val application: Application) {

  private var overlayScope: CoroutineScope? = null
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

    val overlayScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    overlayViewManager = ActivityOverlayViewManager(application, overlayScope)
    this.overlayScope = overlayScope

    activityLifecycleCallbacks = overlayViewManager?.createActivityLifecycleCallbacks(this).also {
      application.registerActivityLifecycleCallbacks(it)
    }

    installed = true
  }

  @VisibleForTesting(otherwise = VisibleForTesting.NONE)
  internal fun uninstall() {
    activityLifecycleCallbacks?.let {
      application.unregisterActivityLifecycleCallbacks(it)
    }
    activityLifecycleCallbacks = null
    overlayViewManager = null
    overlayScope?.cancel()
    overlayScope = null
    installed = false
  }

  companion object {

    /**
     * Convenience method to create the [DebugOverlay] instance for its installation.
     */
    @JvmStatic
    fun with(application: Application): DebugOverlay {
      return DebugOverlay(application)
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
