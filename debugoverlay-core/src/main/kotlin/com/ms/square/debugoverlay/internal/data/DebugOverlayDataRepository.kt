package com.ms.square.debugoverlay.internal.data

import com.ms.square.debugoverlay.internal.data.model.LogcatEntry
import com.ms.square.debugoverlay.internal.data.model.NetworkStats
import com.ms.square.debugoverlay.internal.data.source.LogcatDataSource
import com.ms.square.debugoverlay.internal.data.source.NetStatsDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

internal class DebugOverlayDataRepository(scope: CoroutineScope) {

  private val logcatDataSource = LogcatDataSource(scope)
  private val netStatsDataSource = NetStatsDataSource(scope)

  init {
    scope.launch {
      try {
        awaitCancellation()
      } finally {
        // Note: netStatsDataSource self-cleans via stateIn + WhileSubscribed
        logcatDataSource.close()
      }
    }
  }

  val logs: Flow<List<LogcatEntry>> = logcatDataSource.logs
  val netStats: Flow<NetworkStats> = netStatsDataSource.stats
}
