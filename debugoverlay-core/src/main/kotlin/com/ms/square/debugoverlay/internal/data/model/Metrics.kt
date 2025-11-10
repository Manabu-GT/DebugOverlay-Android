package com.ms.square.debugoverlay.internal.data.model

import androidx.annotation.Size
import java.util.LinkedList

private const val VALUE_HISTORY_SIZE: Int = 16

internal data class Metrics(
  val value: Float,
  @field:Size(VALUE_HISTORY_SIZE.toLong()) val valueHistory: List<Float>,
)

/**
 * Accumulates metric values into a circular buffer and produces [Metrics] snapshots.
 * Used in DebugOverlayPanelDataSourceImpl to persist history across flow collection restarts.
 */
internal class MetricsAccumulator {
  private val buffer = CircularBuffer<Float>()

  fun accumulate(value: Float): Metrics {
    buffer.add(value)
    return Metrics(value, buffer.toList())
  }

  private class CircularBuffer<T>(private val capacity: Int = VALUE_HISTORY_SIZE) {
    private val buffer = LinkedList<T>()

    fun add(item: T) {
      if (buffer.size >= capacity) {
        buffer.removeFirst()
      }
      buffer.addLast(item)
    }

    fun toList(): List<T> = buffer.toList()
  }
}
