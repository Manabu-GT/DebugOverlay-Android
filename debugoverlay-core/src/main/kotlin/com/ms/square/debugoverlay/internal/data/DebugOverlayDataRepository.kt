package com.ms.square.debugoverlay.internal.data

import android.app.Activity
import android.content.Context
import com.ms.square.debugoverlay.LogTracker
import com.ms.square.debugoverlay.NetworkRequestTracker
import com.ms.square.debugoverlay.NoOpNetworkRequestTracker
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

/** Default name shown when a custom log tracker doesn't provide a source name. */
internal const val DEFAULT_CUSTOM_TRACKER_NAME = "Custom"

internal class DebugOverlayDataRepository(context: Context, scope: CoroutineScope) {

  private val currentNetworkRequestTracker = MutableStateFlow<NetworkRequestTracker>(NoOpNetworkRequestTracker)
  private val logcatDataSource = LogcatDataSource(scope)
  private val customLogTracker = MutableStateFlow<LogTracker?>(null)
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

  // Custom tracker logs - empty list when no custom tracker is registered
  // Use hasCustomTracker to determine if a tracker exists (e.g., for bug reports)
  @OptIn(ExperimentalCoroutinesApi::class)
  val customTrackerLogs: StateFlow<List<LogEntry>> = customLogTracker
    .flatMapLatest { tracker ->
      tracker?.logs
        ?.throttleLatest(500.milliseconds)
        ?.catch { e ->
          Logger.w("Custom log tracker error", e)
          emit(emptyList())
        }
        ?: flowOf(emptyList())
    }
    .stateIn(scope, SharingStarted.Eagerly, emptyList())

  // Custom tracker source name (e.g., "Timber")
  val customTrackerSourceName: StateFlow<String?> = customLogTracker
    .map { it?.sourceName }
    .stateIn(scope, SharingStarted.Eagerly, null)

  // Whether a custom tracker is registered
  val hasCustomTracker: StateFlow<Boolean> = customLogTracker
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
  suspend fun queryDeviceInfoSnapshot(): DeviceInfo = deviceInfoDataSource.queryDeviceInfoSnapshot()
  suspend fun queryAppExitInfosSnapshot(): List<AppExitInfo> = appExitDataSource.queryAppExitInfosSnapshot()

  @OptIn(ExperimentalCoroutinesApi::class)
  val networkRequests: Flow<List<NetworkRequest>> = currentNetworkRequestTracker
    .flatMapLatest { tracker -> tracker.requests }

  fun setNetworkTracker(tracker: NetworkRequestTracker) {
    currentNetworkRequestTracker.value = tracker
  }

  fun setLogTracker(tracker: LogTracker?) {
    customLogTracker.value = tracker
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
}
