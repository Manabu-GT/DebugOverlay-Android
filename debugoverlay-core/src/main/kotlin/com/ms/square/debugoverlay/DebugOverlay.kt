package com.ms.square.debugoverlay

import android.app.Application
import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import com.ms.square.debugoverlay.internal.OverlayViewManager
import com.ms.square.debugoverlay.internal.data.DebugOverlayDataRepository
import com.ms.square.debugoverlay.internal.util.checkMainThread
import com.ms.square.debugoverlay.internal.util.isMainProcess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Internal entry point for DebugOverlay auto-installers.
 * [DebugOverlay.install] is called automatically on app startup by either:
 * - DebugOverlayInstaller (ContentProvider) in the default debugoverlay artifact
 * - DebugOverlayStartupInitializer (AndroidX Startup) in the debugoverlay-androidx-startup artifact
 *
 * ** [DebugOverlay.install] is Not intended for use by application code.**
 * There is no supported way to manually control the overlay lifecycle in v2.x.
 */
public object DebugOverlay {

  /**
   * DebugOverlay configuration data class. Properties can be updated via [copy].
   *
   * @see [config]
   */
  public data class Config(val networkRequestTracker: NetworkRequestTracker = NoOpNetworkRequestTracker)

  @get:MainThread
  @set:MainThread
  public var config: Config = Config()
    get() {
      checkMainThread()
      return field
    }
    set(newConfig) {
      checkMainThread()
      if (field != newConfig) {
        field = newConfig
        _overlayDataRepository?.setNetworkTracker(newConfig.networkRequestTracker)
      }
    }

  private var _overlayDataRepository: DebugOverlayDataRepository? = null

  private var overlayScope: CoroutineScope? = null
  private var overlayViewManager: OverlayViewManager? = null

  @get:MainThread
  private val isInstalled: Boolean
    get() = overlayScope != null

  @get:MainThread
  internal val overlayDataRepository: DebugOverlayDataRepository
    get() = _overlayDataRepository ?: error("DebugOverlayDataRepository not initialized")

  @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
  @MainThread
  public fun install(application: Application) {
    if (!isMainProcess(application)) {
      // Just return early without any work if it's not running in the main app process
      return
    }
    checkMainThread()
    check(!isInstalled) { "DebugOverlay already installed" }

    overlayScope = CoroutineScope(SupervisorJob() + Dispatchers.Default).also {
      _overlayDataRepository = DebugOverlayDataRepository(it).apply {
        setNetworkTracker(config.networkRequestTracker)
      }
      overlayViewManager = OverlayViewManager(application, it)
    }
  }
}
