package com.ms.square.debugoverlay.internal.data

import android.app.Activity
import android.content.Context
import com.ms.square.debugoverlay.LogTracker
import com.ms.square.debugoverlay.NetworkRequestTracker
import com.ms.square.debugoverlay.NoOpNetworkRequestTracker
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

internal class DebugOverlayDataRepository(context: Context, scope: CoroutineScope) {

  private val currentNetworkRequestTracker = MutableStateFlow<NetworkRequestTracker>(NoOpNetworkRequestTracker)
  private val logcatDataSource = LogcatDataSource(scope)
  private val currentLogTracker = MutableStateFlow<LogTracker>(logcatDataSource)
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

  // Expose current log source name for UI indicator
  val logSourceName: Flow<String> = currentLogTracker.map { it.sourceName }

  // IMPORTANT: Do NOT call logcatDataSource.close() explicitly when switching trackers
  // WhileSubscribed handles lifecycle - logcat auto-restarts when switching back
  @OptIn(ExperimentalCoroutinesApi::class)
  val logs: Flow<List<LogEntry>> = currentLogTracker.flatMapLatest { tracker ->
    // Custom trackers (e.g., Timber) get throttled here since they emit on every log call.
    // LogcatDataSource already has internal throttling, so no need to double-throttle.
    if (tracker === logcatDataSource) tracker.logs else tracker.logs.throttleLatest(500.milliseconds)
  }

  val netStats: Flow<NetworkStats> = netStatsDataSource.stats
  val deviceInfo: Flow<DeviceInfo?> = deviceInfoDataSource.deviceInfo
  val jankStats: Flow<JankStatsUiState> = jankStatsDataSource.state

  val isAppExitSupported: Boolean
    get() = appExitDataSource.isSupported

  val appExitInfos: Flow<List<AppExitInfo>> = appExitDataSource.appExitInfos

  @OptIn(ExperimentalCoroutinesApi::class)
  val networkRequests: Flow<List<NetworkRequest>> = currentNetworkRequestTracker
    .flatMapLatest { tracker -> tracker.requests }

  fun setNetworkTracker(tracker: NetworkRequestTracker) {
    currentNetworkRequestTracker.value = tracker
  }

  fun setLogTracker(tracker: LogTracker?) {
    currentLogTracker.value = tracker ?: logcatDataSource
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
