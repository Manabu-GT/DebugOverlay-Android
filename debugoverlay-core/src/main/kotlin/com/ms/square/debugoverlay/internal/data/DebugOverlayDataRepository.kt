package com.ms.square.debugoverlay.internal.data

import android.app.Activity
import android.content.Context
import com.ms.square.debugoverlay.Clearable
import com.ms.square.debugoverlay.LogSource
import com.ms.square.debugoverlay.NetworkRequestSource
import com.ms.square.debugoverlay.NoOpNetworkRequestSource
import com.ms.square.debugoverlay.internal.Logger
import com.ms.square.debugoverlay.internal.data.model.AppExitInfo
import com.ms.square.debugoverlay.internal.data.model.DeviceInfo
import com.ms.square.debugoverlay.internal.data.model.JankStatsUiState
import com.ms.square.debugoverlay.internal.data.model.NetworkStats
import com.ms.square.debugoverlay.internal.data.source.AppExitDataSource
import com.ms.square.debugoverlay.internal.data.source.DeviceInfoDataSource
import com.ms.square.debugoverlay.internal.data.source.JankStatsDataSource
import com.ms.square.debugoverlay.internal.data.source.LogcatDataSource
import com.ms.square.debugoverlay.internal.data.source.NetStatsDataSource
import com.ms.square.debugoverlay.internal.util.throttleLatest
import com.ms.square.debugoverlay.model.LogEntry
import com.ms.square.debugoverlay.model.NetworkRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/** Default name shown when a custom log source doesn't provide a source name. */
internal const val DEFAULT_CUSTOM_LOG_SOURCE_NAME = "Custom"

internal class DebugOverlayDataRepository(context: Context, scope: CoroutineScope, initialLogcatMaxEntries: Int) {

  private val currentNetworkRequestSource = MutableStateFlow<NetworkRequestSource>(NoOpNetworkRequestSource)
  private val logcatDataSource = LogcatDataSource(scope, initialMaxEntries = initialLogcatMaxEntries)
  private val customLogSource = MutableStateFlow<LogSource?>(null)
  private val netStatsDataSource = NetStatsDataSource(scope)
  private val deviceInfoDataSource = DeviceInfoDataSource(context, scope)
  private val jankStatsDataSource = JankStatsDataSource()
  private val appExitDataSource = AppExitDataSource(context, scope)

  init {
    scope.launch {
      try {
        awaitCancellation()
      } finally {
        // Note: netStatsDataSource self-cleans via stateIn + WhileSubscribed
        // The flow will automatically stop collecting when there are no active subscribers,
        // and resume when the UI resubscribes. No explicit cleanup needed.
        logcatDataSource.close()
      }
    }
  }

  // Logcat logs - always available (LogcatDataSource internally uses stateIn)
  val logcatLogs: Flow<List<LogEntry>> = logcatDataSource.logs

  // Custom source logs - empty list when no custom source is registered
  // Use hasCustomLogSource to determine if a source exists (e.g., for bug reports)
  @OptIn(ExperimentalCoroutinesApi::class)
  val customLogSourceLogs: StateFlow<List<LogEntry>> = customLogSource
    .flatMapLatest { source ->
      source?.logs
        ?.throttleLatest(500.milliseconds)
        ?.catch { e ->
          Logger.w("Custom log source error", e)
          emit(emptyList())
        }
        ?: flowOf(emptyList())
    }
    .stateIn(scope, SharingStarted.Eagerly, emptyList())

  // Custom source name (e.g., "Timber")
  val customLogSourceName: StateFlow<String?> = customLogSource
    .map { it?.sourceName }
    .stateIn(scope, SharingStarted.Eagerly, null)

  // Whether a custom log source is registered
  val hasCustomLogSource: StateFlow<Boolean> = customLogSource
    .map { it != null }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, false)

  val netStats: Flow<NetworkStats> = netStatsDataSource.stats
  val deviceInfo: Flow<DeviceInfo?> = deviceInfoDataSource.deviceInfo
  val jankStats: Flow<JankStatsUiState> = jankStatsDataSource.state

  val isAppExitSupported: Boolean
    get() = appExitDataSource.isSupported

  val appExitInfos: Flow<List<AppExitInfo>> = appExitDataSource.appExitInfos

  // Snapshot methods for bug reports (use cached value if available, otherwise query directly)
  suspend fun queryLogcatSnapshot(): List<LogEntry> = logcatDataSource.queryLogcatSnapshot()
  suspend fun queryDeviceInfoSnapshot(): DeviceInfo = deviceInfoDataSource.queryDeviceInfoSnapshot()
  suspend fun queryAppExitInfosSnapshot(): List<AppExitInfo> = appExitDataSource.queryAppExitInfosSnapshot()

  @OptIn(ExperimentalCoroutinesApi::class)
  val networkRequests: Flow<List<NetworkRequest>> = currentNetworkRequestSource
    .flatMapLatest { source -> source.requests }

  fun setNetworkSource(source: NetworkRequestSource) {
    currentNetworkRequestSource.value = source
  }

  fun setCustomLogSource(source: LogSource?) {
    customLogSource.value = source
  }

  fun setLogcatMaxEntries(maxEntries: Int) {
    logcatDataSource.maxEntries = maxEntries
  }

  fun startOrResumeJankStatsTracking(activity: Activity) {
    jankStatsDataSource.startOrResumeTracking(activity)
  }

  fun pauseJankStatsTracking(activity: Activity) {
    jankStatsDataSource.pauseTracking(activity)
  }

  fun stopJankStatsTracking(activity: Activity) {
    jankStatsDataSource.stopTracking(activity)
  }

  /**
   * Clears accumulated entries from sources that opt into [Clearable]:
   * built-in logcat capture plus the current network and custom log sources
   * when they implement [Clearable]. Custom external sources that don't
   * implement [Clearable] are silently skipped.
   */
  fun clearAllLogs() {
    logcatDataSource.clear()
    (currentNetworkRequestSource.value as? Clearable)?.clear()
    (customLogSource.value as? Clearable)?.clear()
  }
}
