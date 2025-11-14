package com.ms.square.debugoverlay

import android.app.Application
import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import com.ms.square.debugoverlay.internal.OverlayViewManager
import com.ms.square.debugoverlay.internal.util.checkMainThread
import com.ms.square.debugoverlay.internal.util.isMainProcess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Internal entry point for DebugOverlay auto-installers.
 * This is called automatically on app startup by either:
 * - DebugOverlayInstaller (ContentProvider) in the default debugoverlay artifact
 * - DebugOverlayStartupInitializer (AndroidX Startup) in the debugoverlay-androidx-startup artifact
 *
 * **Not intended for use by application code.** There is no supported way to manually control
 * the overlay lifecycle in v2.x.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object DebugOverlay {

  private var overlayScope: CoroutineScope? = null
  private var overlayViewManager: OverlayViewManager? = null

  @get:MainThread
  private val isInstalled: Boolean
    get() = overlayScope != null

  @MainThread
  fun install(application: Application) {
    if (!isMainProcess(application)) {
      // Just return early without any work if it's not running in the main app process
      return
    }
    checkMainThread()
    check(!isInstalled) { "DebugOverlay already installed" }

    overlayScope = CoroutineScope(SupervisorJob() + Dispatchers.Default).also {
      overlayViewManager = OverlayViewManager(application, it)
    }
  }
}
