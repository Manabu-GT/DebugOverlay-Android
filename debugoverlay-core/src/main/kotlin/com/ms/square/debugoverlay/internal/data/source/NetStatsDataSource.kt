package com.ms.square.debugoverlay.internal.data.source

import android.net.TrafficStats
import android.os.Process
import com.ms.square.debugoverlay.internal.Logger
import com.ms.square.debugoverlay.internal.data.model.NetworkStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.seconds

internal const val TRAFFIC_STATS_UNSUPPORTED = TrafficStats.UNSUPPORTED.toLong()
private val TRAFFIC_STATS_UPDATE_INTERVAL = 3.seconds

internal class NetStatsDataSource(scope: CoroutineScope) {

  private val myUid = Process.myUid()

  private val baselineBytes = scope.async(Dispatchers.IO) {
    val rx = TrafficStats.getUidRxBytes(myUid)
    val tx = TrafficStats.getUidTxBytes(myUid)
    if (rx == TRAFFIC_STATS_UNSUPPORTED || tx == TRAFFIC_STATS_UNSUPPORTED) {
      return@async null
    }
    rx to tx
  }

  val stats: Flow<NetworkStats> = flow {
    // Wait for the baseline to be ready
    val baseline = baselineBytes.await()
    if (baseline == null) {
      Logger.i("The use of TrafficStats is not supported on this device.")
      emit(NetworkStats.UNSUPPORTED)
      return@flow
    }
    while (currentCoroutineContext().isActive) {
      val totalBytesReceived = TrafficStats.getUidRxBytes(myUid)
      val totalBytesSent = TrafficStats.getUidTxBytes(myUid)

      if (totalBytesReceived == TRAFFIC_STATS_UNSUPPORTED || totalBytesSent == TRAFFIC_STATS_UNSUPPORTED) {
        Logger.i("The use of TrafficStats is not supported on this device.")
        emit(NetworkStats.UNSUPPORTED)
        break
      }
      emit(NetworkStats(totalBytesReceived - baseline.first, totalBytesSent - baseline.second))
      delay(TRAFFIC_STATS_UPDATE_INTERVAL)
    }
  }
    .flowOn(Dispatchers.IO).stateIn(
      scope,
      started = SharingStarted.WhileSubscribed(),
      initialValue = NetworkStats.INITIAL_VALUE
    )
}
