package com.ms.square.debugoverlay.internal.data.source

import android.content.Context
import com.ms.square.debugoverlay.internal.data.model.DebugOverlayPanelMetrics
import com.ms.square.debugoverlay.internal.data.model.toMetrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

internal sealed interface DebugOverlayPanelDataSource {
  fun debugOverlayPanelMetrics(): Flow<DebugOverlayPanelMetrics>
}

internal class DebugOverlayPanelDataSourceImpl(context: Context, displayDataSource: DisplayDataSource) : DebugOverlayPanelDataSource {

  private val cpuDataSource = CpuDataSource()
  private val memoryDataSource = MemoryDataSource(context)
  private val fpsDataSourceImpl = FpsDataSource(displayDataSource)

  override fun debugOverlayPanelMetrics(): Flow<DebugOverlayPanelMetrics> {
    return combine(
      cpuDataSource.cpuUsage().map { it.value }.toMetrics(),
      memoryDataSource.heapUsage().map { it.value }.toMetrics(),
      memoryDataSource.pss().toMetrics(),
      fpsDataSourceImpl.fps().toMetrics().flowOn(Dispatchers.Main)
    ) {
      cpuUsage, heapUsage, pss, fps ->
      DebugOverlayPanelMetrics(cpuUsage, heapUsage, pss, fps)
    }.flowOn(Dispatchers.IO)
  }
}

