package com.ms.square.debugoverlay

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Build
import androidx.annotation.MainThread
import com.ms.square.debugoverlay.internal.OverlayViewManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class DebugOverlay private constructor(private val application: Application) {

  private var overlayScope: CoroutineScope? = null
  private var overlayViewManager: OverlayViewManager? = null
  private var installed = false

  /**
   * Install the debug overlay into the application.
   * In the main process, this method is automatically called on app startup.
   * You can call this method directly to customize the installation timings, however you must
   * first disable the automatic call by overriding the `debugoverlay_auto_install` boolean
   * resource:
   *
   `* ``xml
   * <?xml version="1.0" encoding="utf-8"?>
   * <resources>
   *   <bool name="debugoverlay_auto_install">false</bool>
   * </resources>
   * ```
   * Also, note that the above only works if `debugoverlay` module is used.
   *
   */
  @MainThread
  fun install() {
    check(!installed) { "install() can be called only once!" }

    if (!isMainProcess(application)) {
      // Just return early without any work if it's not running in the main app process
      return
    }

    overlayScope = CoroutineScope(SupervisorJob() + Dispatchers.Default).also {
      overlayViewManager = OverlayViewManager(application, it)
    }
    installed = true
  }

  /**
   * Uninstall the debug overlay from the application.
   * You can call install() again to re-install the overlay after uninstalled.
   */
  @MainThread
  fun uninstall() {
    overlayViewManager?.cleanUp()
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
    fun with(application: Application): DebugOverlay = DebugOverlay(application)

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
