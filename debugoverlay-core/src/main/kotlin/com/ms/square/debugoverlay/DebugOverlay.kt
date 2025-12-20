package com.ms.square.debugoverlay

import android.app.Application
import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import com.ms.square.debugoverlay.internal.InternalDebugOverlayApi
import com.ms.square.debugoverlay.internal.Logger
import com.ms.square.debugoverlay.internal.OverlayViewManager
import com.ms.square.debugoverlay.internal.bugreport.BugReportGenerator
import com.ms.square.debugoverlay.internal.data.DebugOverlayDataRepository
import com.ms.square.debugoverlay.internal.util.checkMainThread
import com.ms.square.debugoverlay.internal.util.isMainProcess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Internal entry point for DebugOverlay auto-installers.
 * [DebugOverlay.install] is called automatically on app startup by:
 * - DebugOverlayStartupInitializer (AndroidX Startup) in the debugoverlay artifact
 *
 * ** [DebugOverlay.install] is Not intended for use by application code.**
 * There is no supported way to manually control the overlay lifecycle in v2.x.
 */
public object DebugOverlay {

  @Volatile
  private var config: Config = Config()
    set(newConfig) {
      if (field != newConfig) {
        field = newConfig
        _overlayDataRepository?.apply {
          setNetworkTracker(newConfig.networkRequestTracker)
          setLogTracker(newConfig.logTracker)
        } ?: Logger.d("Config updated before install, will apply during install")
        overlayViewManager?.overlayMode = newConfig.overlayMode
      }
    }

  // added volatile as config setter reads it
  @Volatile
  private var _overlayDataRepository: DebugOverlayDataRepository? = null

  private var overlayScope: CoroutineScope? = null
  private var overlayViewManager: OverlayViewManager? = null
  private var _bugReportGenerator: BugReportGenerator? = null

  @get:MainThread
  private val isInstalled: Boolean
    get() = overlayScope != null

  internal val overlayDataRepository: DebugOverlayDataRepository
    get() = _overlayDataRepository ?: error("DebugOverlayDataRepository not initialized")

  @get:MainThread
  internal val bugReportGenerator: BugReportGenerator
    get() = _bugReportGenerator ?: error("BugReportGenerator not initialized")

  @InternalDebugOverlayApi
  @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
  @MainThread
  public fun install(application: Application) {
    if (!isMainProcess(application)) {
      // Just return early without any work if it's not running in the main app process
      return
    }
    checkMainThread()
    check(!isInstalled) { "DebugOverlay already installed" }

    overlayScope = CoroutineScope(SupervisorJob() + Dispatchers.Default).also { scope ->
      val repository = DebugOverlayDataRepository(application, scope).apply {
        setNetworkTracker(config.networkRequestTracker)
        setLogTracker(config.logTracker)
      }
      _overlayDataRepository = repository

      val viewManager = OverlayViewManager(application, scope, config.overlayMode)
      overlayViewManager = viewManager

      _bugReportGenerator = BugReportGenerator(
        context = application,
        repository = repository,
        activityProvider = viewManager
      )
    }
  }

  /**
   * Configures DebugOverlay settings.
   *
   * Auto-installation happens via AndroidX Startup before [Application.onCreate].
   * Call this function in [Application.onCreate] after dependency injection to
   * configure network tracking or other features.
   *
   * @param block Configuration builder that receives current [Config] and returns new [Config]
   */
  public fun configure(block: Config.() -> Config) {
    config = config.block()
  }

  /**
   * DebugOverlay configuration.
   *
   * @property overlayMode The overlay display mode.
   *   [OverlayMode.FullMetrics] (default) shows real-time metrics panel.
   *   [OverlayMode.BugReporterOnly] shows a minimal FAB for quick bug reporting.
   * @property networkRequestTracker Tracks HTTP requests for display in Network tab.
   *   Default is [NoOpNetworkRequestTracker] which disables network tracking.
   *   Use DebugOverlayNetworkInterceptor from debugoverlay-extension-okhttp for OkHttp integration.
   * @property logTracker Custom log tracker to replace system logcat reading.
   *   Default is null which uses the built-in system logcat reader.
   *   Use DebugOverlayTimberTree from debugoverlay-extension-timber for Timber integration.
   *
   * @see configure
   */
  public data class Config(
    val overlayMode: OverlayMode = OverlayMode.BugReporterOnly,
    val networkRequestTracker: NetworkRequestTracker = NoOpNetworkRequestTracker,
    val logTracker: LogTracker? = null,
  )
}
