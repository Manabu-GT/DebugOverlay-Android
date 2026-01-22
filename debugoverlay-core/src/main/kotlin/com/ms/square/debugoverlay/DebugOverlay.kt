package com.ms.square.debugoverlay

import android.annotation.SuppressLint
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
import java.util.concurrent.CopyOnWriteArrayList

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
  internal var config: Config = Config()
    private set(newConfig) {
      if (field != newConfig) {
        field = newConfig
        if (!isInstalled) {
          Logger.d("Config is getting updated before install, will apply during install")
        }
        _overlayDataRepository?.apply {
          setNetworkSource(newConfig.networkRequestSource)
          setCustomLogSource(newConfig.customLogSource)
        }
        overlayViewManager?.let { it.overlayMode = newConfig.overlayMode }
      }
    }

  // added volatile as config setter reads it
  @Volatile
  private var _overlayDataRepository: DebugOverlayDataRepository? = null

  private var overlayScope: CoroutineScope? = null

  // Held to detect window removal and prevent redundant overlay updates.
  // Not a leak: nulled in hideOverlay() when window is removed.
  @SuppressLint("StaticFieldLeak")
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
        setNetworkSource(config.networkRequestSource)
        setCustomLogSource(config.customLogSource)
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

  internal val bugReportContributors = CopyOnWriteArrayList<BugReportDataContributor>()

  /**
   * Configures DebugOverlay settings.
   *
   * Auto-installation happens via AndroidX Startup before [Application.onCreate].
   * Call this function in [Application.onCreate] after dependency injection to
   * configure network tracking or other features.
   *
   * Example:
   * ```kotlin
   * DebugOverlay.configure {
   *   overlayMode = OverlayMode.BugReporterOnly
   *   networkRequestSource = myOkHttpSource
   * }
   * ```
   *
   * @param block Configuration DSL that modifies settings via [ConfigBuilder]
   */
  public fun configure(block: ConfigBuilder.() -> Unit) {
    config = ConfigBuilder(config).apply(block).build()
  }

  /**
   * Registers a custom data contributor for bug reports.
   *
   * Contributors add app-specific diagnostic data (preferences, feature flags, etc.)
   * to bug reports. The library handles threading, timeout enforcement, and error
   * isolation automatically.
   *
   * Example:
   * ```kotlin
   * DebugOverlay.addBugReportContributor(SharedPreferencesContributor(context))
   * ```
   *
   * Duplicate instances (same reference) are ignored.
   *
   * @param contributor The contributor to register
   * @see BugReportDataContributor
   */
  public fun addBugReportContributor(contributor: BugReportDataContributor) {
    // Use addIfAbsent for identity-based duplicate check
    bugReportContributors.addIfAbsent(contributor)
  }

  /**
   * DSL builder for [Config].
   *
   * Used with [configure] to modify settings:
   * ```kotlin
   * DebugOverlay.configure {
   *   overlayMode = OverlayMode.BugReporterOnly
   *   networkRequestSource = myOkHttpSource
   * }
   * ```
   */
  public class ConfigBuilder internal constructor(initial: Config) {
    /**
     * The overlay display mode.
     * [OverlayMode.FullMetrics] (default) shows real-time metrics panel.
     * [OverlayMode.BugReporterOnly] shows a minimal FAB for quick bug reporting.
     */
    public var overlayMode: OverlayMode = initial.overlayMode

    /**
     * Provides HTTP requests for display in Network tab.
     * Default is [NoOpNetworkRequestSource] which disables network request display.
     * Use DebugOverlayNetworkInterceptor from debugoverlay-extension-okhttp for OkHttp integration.
     */
    public var networkRequestSource: NetworkRequestSource = initial.networkRequestSource

    /**
     * Optional custom log source shown as an additional tab.
     * Default is null. The built-in Logcat tab is always available.
     * When set, a second tab appears showing logs from this custom source.
     * Use debugoverlay-extension-timber for Timber integration.
     */
    public var customLogSource: LogSource? = initial.customLogSource

    internal fun build(): Config = Config(
      overlayMode = overlayMode,
      networkRequestSource = networkRequestSource,
      customLogSource = customLogSource
    )
  }

  /**
   * DebugOverlay configuration.
   *
   * @property overlayMode The overlay display mode.
   *   [OverlayMode.FullMetrics] (default) shows real-time metrics panel.
   *   [OverlayMode.BugReporterOnly] shows a minimal FAB for quick bug reporting.
   * @property networkRequestSource Provides HTTP requests for display in Network tab.
   *   Default is [NoOpNetworkRequestSource] which disables network request display.
   *   Use DebugOverlayNetworkInterceptor from debugoverlay-extension-okhttp for OkHttp integration.
   * @property customLogSource Optional custom log source shown as an additional tab.
   *   Default is null. The built-in Logcat tab is always available.
   *   When set, a second tab appears showing logs from this custom source.
   *   Use debugoverlay-extension-timber for Timber integration.
   *
   * @see configure
   */
  public data class Config(
    val overlayMode: OverlayMode = OverlayMode.FullMetrics,
    val networkRequestSource: NetworkRequestSource = NoOpNetworkRequestSource,
    val customLogSource: LogSource? = null,
  )
}
