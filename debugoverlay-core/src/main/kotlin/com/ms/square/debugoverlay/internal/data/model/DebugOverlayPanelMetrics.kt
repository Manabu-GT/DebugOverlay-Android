package com.ms.square.debugoverlay.internal.data.model

internal data class DebugOverlayPanelMetrics(
  val cpuMetrics: Metrics,
  val heapMetrics: Metrics,
  val pssMetrics: Metrics,
  val fpsMetrics: Metrics
)
