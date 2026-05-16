package com.ms.square.debugoverlay

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.annotation.AnyThread
import androidx.annotation.IntRange
import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import com.ms.square.debugoverlay.internal.InternalDebugOverlayApi
import com.ms.square.debugoverlay.internal.Logger
import com.ms.square.debugoverlay.internal.OverlayViewManager
import com.ms.square.debugoverlay.internal.bugreport.BugReportGenerator
import com.ms.square.debugoverlay.internal.bugreport.IntentShareExporter
import com.ms.square.debugoverlay.internal.bugreport.validateFilename
import com.ms.square.debugoverlay.internal.data.DebugOverlayDataRepository
import com.ms.square.debugoverlay.internal.ui.DebugPanelActivity
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
        _overlayDataRepository?.apply {
          setNetworkSource(newConfig.networkRequestSource)
          setCustomLogSource(newConfig.customLogSource)
          setLogcatMaxEntries(newConfig.maxLogcatEntries)
        }
        overlayViewManager?.overlayMode?.value = newConfig.overlayMode
      }
    }

  // added volatile as config setter reads it
  @Volatile
  private var _overlayDataRepository: DebugOverlayDataRepository? = null

  // Held to detect window removal and prevent redundant overlay updates.
  // Not a leak: nulled in hideOverlay() when window is removed.
  // added volatile as config setter reads it
  @Volatile
  @SuppressLint("StaticFieldLeak")
  private var overlayViewManager: OverlayViewManager? = null

  private var overlayScope: CoroutineScope? = null
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
      val repository = DebugOverlayDataRepository(
        context = application,
        scope = scope,
        initialLogcatMaxEntries = config.maxLogcatEntries
      ).apply {
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

  // CopyOnWriteArrayList enables lock-free iteration during bug report generation
  // synchronized block in addBugReportContributor ensures atomic duplicate detection
  internal val bugReportContributors = CopyOnWriteArrayList<BugReportDataContributor>()

  /**
   * Configures DebugOverlay settings.
   *
   * Auto-installation happens via AndroidX Startup before [Application.onCreate].
   * You typically call this function in [Application.onCreate] after dependency injection to
   * configure network tracking or other features.
   *
   * Example:
   * ```kotlin
   * DebugOverlay.configure {
   *   overlayMode = OverlayMode.BugReporterOnly
   *   networkRequestSource = yourNetworkRequestSource
   * }
   * ```
   *
   * @param block Configuration DSL that modifies settings via [ConfigBuilder]
   */
  @AnyThread
  public fun configure(block: ConfigBuilder.() -> Unit) {
    config = ConfigBuilder(config).apply(block).build()
  }

  /**
   * Launches the debug panel programmatically. Works in any [OverlayMode],
   * including [OverlayMode.Hidden] where no overlay is shown.
   *
   * Typical usage from your debug menu:
   * ```
   * Button(onClick = { DebugOverlay.openPanel(context) }) { Text("Open debug panel") }
   * ```
   *
   * @param context Any [Context].
   */
  @AnyThread
  public fun openPanel(context: Context) {
    val intent = Intent(context, DebugPanelActivity::class.java)
      .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
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
   * Invalid filenames are rejected with a warning log. Valid characters: a-z, A-Z, 0-9, _, ., -
   * Duplicate filenames (case-insensitive) are ignored to prevent file overwrites.
   *
   * @param contributor The contributor to register
   * @see BugReportDataContributor
   */
  @AnyThread
  public fun addBugReportContributor(contributor: BugReportDataContributor) {
    val validationError = validateFilename(contributor.filename)
    if (validationError != null) {
      Logger.w("BugReportContributor rejected: $validationError (filename='${contributor.filename}')")
      return
    }

    synchronized(bugReportContributors) {
      if (bugReportContributors.none { it.filename.equals(contributor.filename, ignoreCase = true) }) {
        bugReportContributors.add(contributor)
      } else {
        Logger.w("BugReportContributor with filename '${contributor.filename}' already registered, ignoring")
      }
    }
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

    /**
     * The exporter used when the user submits a bug report.
     *
     * Default is the built-in share sheet exporter.
     * Set a custom [BugReportExporter] to send reports to Jira, GitHub, Slack, etc.
     */
    public var bugReportExporter: BugReportExporter = initial.bugReportExporter

    /**
     * Maximum number of entries kept in the built-in Logcat tab buffer.
     * Also passed to `logcat -T N` / `-t N` so it controls how many lines the
     * OS replays on producer start (panel open) and on bug-report snapshot.
     *
     * Default is [Config.DEFAULT_MAX_LOGCAT_ENTRIES] (300). Each entry holds a parsed
     * [com.ms.square.debugoverlay.model.LogEntry].
     *
     * @throws IllegalArgumentException if assigned a non-positive value.
     */
    @IntRange(from = 1)
    public var maxLogcatEntries: Int = initial.maxLogcatEntries
      set(value) {
        require(value > 0) { "maxLogcatEntries must be positive, was $value" }
        field = value
      }

    internal fun build(): Config = Config(
      overlayMode = overlayMode,
      networkRequestSource = networkRequestSource,
      customLogSource = customLogSource,
      bugReportExporter = bugReportExporter,
      maxLogcatEntries = maxLogcatEntries
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
   * @property bugReportExporter The exporter used when the user submits a bug report.
   *   Default is the built-in share sheet exporter.
   * @property maxLogcatEntries Maximum number of entries kept in the built-in Logcat
   *   tab buffer. Default is [DEFAULT_MAX_LOGCAT_ENTRIES].
   *
   * @see configure
   */
  public data class Config(
    val overlayMode: OverlayMode = OverlayMode.FullMetrics(),
    val networkRequestSource: NetworkRequestSource = NoOpNetworkRequestSource,
    val customLogSource: LogSource? = null,
    val bugReportExporter: BugReportExporter = IntentShareExporter,
    val maxLogcatEntries: Int = DEFAULT_MAX_LOGCAT_ENTRIES,
  ) {
    public companion object {
      /** Default value for [maxLogcatEntries]. */
      public const val DEFAULT_MAX_LOGCAT_ENTRIES: Int = 300
    }
  }
}
