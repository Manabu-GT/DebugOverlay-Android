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

internal class NetStatsDataSource(scope: CoroutineScope) {

  private val myUid = Process.myUid()

  private val baselineBytes = scope.async(Dispatchers.IO) {
    TrafficStats.getUidRxBytes(myUid) to TrafficStats.getUidTxBytes(myUid)
  }

  val stats: Flow<NetworkStats> = flow {
    // Wait for the baseline to be ready
    val (baselineBytesReceived, baselineBytesSent) = baselineBytes.await()
    while (currentCoroutineContext().isActive) {
      val totalBytesReceived = TrafficStats.getUidRxBytes(myUid)
      val totalBytesSent = TrafficStats.getUidTxBytes(myUid)

      @Suppress("ComplexCondition")
      if (baselineBytesReceived == TRAFFIC_STATS_UNSUPPORTED || baselineBytesSent == TRAFFIC_STATS_UNSUPPORTED ||
        totalBytesReceived == TRAFFIC_STATS_UNSUPPORTED || totalBytesSent == TRAFFIC_STATS_UNSUPPORTED
      ) {
        Logger.i("The use of TrafficStats is not supported on this device.")
        emit(NetworkStats.UNSUPPORTED)
        break
      }
      emit(NetworkStats(totalBytesReceived - baselineBytesReceived, totalBytesSent - baselineBytesSent))
      delay(3.seconds)
    }
  }
    .flowOn(Dispatchers.IO).stateIn(
      scope,
      started = SharingStarted.WhileSubscribed(),
      initialValue = NetworkStats.INITIAL_VALUE
    )
}
