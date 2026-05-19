package com.ms.square.debugoverlay.internal.data.source

import android.content.Context
import com.ms.square.debugoverlay.internal.data.model.DebugOverlayPanelMetrics
import com.ms.square.debugoverlay.internal.data.model.MetricsAccumulator
import com.ms.square.debugoverlay.internal.data.model.ThermalState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn

internal sealed interface DebugOverlayPanelDataSource {
  val debugOverlayPanelMetrics: Flow<DebugOverlayPanelMetrics?>

  /**
   * Thermal status flow, exposed separately from [debugOverlayPanelMetrics] so consumers can
   * subscribe conditionally (e.g. only when `OverlayMode.FullMetrics.showThermal` is `true`).
   * Backed by `shareIn(WhileSubscribed)` so the underlying PowerManager listener / headroom
   * polling only runs while something is collecting.
   */
  val thermalState: Flow<ThermalState>
}

internal class DebugOverlayPanelDataSourceImpl(context: Context, overlayScope: CoroutineScope) :
  DebugOverlayPanelDataSource {

  private val cpuDataSource = CpuDataSource()
  private val memoryDataSource = MemoryDataSource(context)
  private val fpsDataSource = FpsDataSource(context)
  private val thermalDataSource = ThermalDataSource(context)

  // Accumulators for maintaining history across flow collection restarts
  private val cpuAccumulator = MetricsAccumulator()
  private val heapAccumulator = MetricsAccumulator()
  private val pssAccumulator = MetricsAccumulator()
  private val fpsAccumulator = MetricsAccumulator()

  override val debugOverlayPanelMetrics: Flow<DebugOverlayPanelMetrics?> = combine(
    cpuDataSource.cpuUsage().map { cpuAccumulator.accumulate(it.value) },
    memoryDataSource.heapUsage().map { heapAccumulator.accumulate(it.value) },
    memoryDataSource.pss().map { pssAccumulator.accumulate(it) },
    fpsDataSource.fps().map { fpsAccumulator.accumulate(it) }
  ) { cpuMetrics, heapMetrics, pssMetrics, fpsMetrics ->
    DebugOverlayPanelMetrics(
      cpuMetrics = cpuMetrics,
      heapMetrics = heapMetrics,
      pssMetrics = pssMetrics,
      maxPss = memoryDataSource.maxPss,
      fpsMetrics = fpsMetrics,
      targetFps = fpsDataSource.currentTargetFps,
      maxFps = fpsDataSource.maxSupportedFps
    )
  }.shareIn(overlayScope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 1_000), replay = 1)

  override val thermalState: Flow<ThermalState> = thermalDataSource.thermalState()
    .shareIn(overlayScope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 1_000), replay = 1)
}
