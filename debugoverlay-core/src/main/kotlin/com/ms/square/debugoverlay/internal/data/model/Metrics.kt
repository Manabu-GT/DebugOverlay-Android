package com.ms.square.debugoverlay.internal.data.model

import androidx.annotation.Size
import com.ms.square.debugoverlay.internal.InternalDebugOverlayApi
import com.ms.square.debugoverlay.internal.data.EvictingQueue

private const val VALUE_HISTORY_SIZE: Int = 16

internal data class Metrics(val value: Float, @field:Size(VALUE_HISTORY_SIZE.toLong()) val valueHistory: List<Float>)

/**
 * Accumulates metric values into a circular buffer and produces [Metrics] snapshots.
 * Used in DebugOverlayPanelDataSourceImpl to persist history across flow collection restarts.
 */
@OptIn(InternalDebugOverlayApi::class)
internal class MetricsAccumulator {
  private val queue = EvictingQueue<Float>(VALUE_HISTORY_SIZE)

  fun accumulate(value: Float): Metrics = Metrics(value, queue.addAndSnapshot(value))
}
