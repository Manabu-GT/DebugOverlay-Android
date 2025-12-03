package com.ms.square.debugoverlay.internal.data

import com.ms.square.debugoverlay.NetworkRequestTracker
import com.ms.square.debugoverlay.NoOpNetworkRequestTracker
import com.ms.square.debugoverlay.internal.data.model.LogcatEntry
import com.ms.square.debugoverlay.internal.data.model.NetworkStats
import com.ms.square.debugoverlay.internal.data.source.LogcatDataSource
import com.ms.square.debugoverlay.internal.data.source.NetStatsDataSource
import com.ms.square.debugoverlay.model.NetworkRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

internal class DebugOverlayDataRepository(scope: CoroutineScope) {

  private val currentNetworkRequestTracker = MutableStateFlow<NetworkRequestTracker>(NoOpNetworkRequestTracker)
  private val logcatDataSource = LogcatDataSource(scope)
  private val netStatsDataSource = NetStatsDataSource(scope)

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

  val logs: Flow<List<LogcatEntry>> = logcatDataSource.logs
  val netStats: Flow<NetworkStats> = netStatsDataSource.stats

  @OptIn(ExperimentalCoroutinesApi::class)
  val networkRequests: Flow<List<NetworkRequest>> = currentNetworkRequestTracker
    .flatMapLatest { tracker -> tracker.requests }

  fun setNetworkTracker(tracker: NetworkRequestTracker) {
    currentNetworkRequestTracker.value = tracker
  }
}
