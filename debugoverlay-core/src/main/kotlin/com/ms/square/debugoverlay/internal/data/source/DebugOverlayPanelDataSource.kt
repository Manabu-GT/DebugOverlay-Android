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

internal class DebugOverlayPanelDataSourceImpl(
  context: Context
) : DebugOverlayPanelDataSource {

  private val cpuDataSource = CpuDataSource()
  private val memoryDataSource = MemoryDataSource(context)
  private val fpsDataSource = FpsDataSource(context)

  override fun debugOverlayPanelMetrics(): Flow<DebugOverlayPanelMetrics> = combine(
    cpuDataSource.cpuUsage().map { it.value }.toMetrics(),
    memoryDataSource.heapUsage().map { it.value }.toMetrics(),
    memoryDataSource.pss().toMetrics(),
    fpsDataSource.fps().toMetrics().flowOn(Dispatchers.Main)
  ) { cpuUsage, heapUsage, pss, fps ->
    DebugOverlayPanelMetrics(
      cpuMetrics = cpuUsage,
      heapMetrics = heapUsage,
      pssMetrics = pss,
      maxPss = memoryDataSource.maxPss,
      fpsMetrics = fps,
      targetFps = fpsDataSource.currentTargetFps,
      maxFps = fpsDataSource.maxSupportedFps
    )
  }.flowOn(Dispatchers.IO)
}
