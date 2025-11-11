package com.ms.square.debugoverlay

import android.app.Application
import androidx.annotation.MainThread
import com.ms.square.debugoverlay.DebugOverlay.manualInstall
import com.ms.square.debugoverlay.internal.OverlayViewManager
import com.ms.square.debugoverlay.internal.util.checkMainThread
import com.ms.square.debugoverlay.internal.util.isMainProcess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * The entry point API for using Debug overlay in Android app.
 * It allows manually installing or uninstalling a debug overlay.
 */
object DebugOverlay {

  private var overlayScope: CoroutineScope? = null
  private var overlayViewManager: OverlayViewManager? = null

  private var installCause: Exception? = null

  /** @see [manualInstall] */
  @get:MainThread
  val isInstalled: Boolean
    get() = installCause != null

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
   * @param application The application to install the overlay into.
   *
   */
  @MainThread
  fun manualInstall(application: Application) {
    if (!isMainProcess(application)) {
      // Just return early without any work if it's not running in the main app process
      return
    }
    checkMainThread()
    checkNotInstalled()

    overlayScope = CoroutineScope(SupervisorJob() + Dispatchers.Default).also {
      overlayViewManager = OverlayViewManager(application, it)
    }

    // Set the installCause iff after we're fully done with init.
    installCause = RuntimeException("manualInstall() first called here")
  }

  /**
   * Uninstall the debug overlay from the application.
   * You can call install() again to re-install the overlay after uninstalled.
   */
  @MainThread
  fun uninstall() {
    checkMainThread()
    if (isInstalled) {
      overlayViewManager?.cleanUp()
      overlayViewManager = null
      overlayScope?.cancel()
      overlayScope = null
      installCause = null
    }
  }

  private fun checkNotInstalled() {
    check(!isInstalled) { "DebugOverlay already installed, see exception cause for prior install call: $installCause" }
  }
}
