package com.ms.square.debugoverlay.internal.data.model

import androidx.annotation.AnyThread
import androidx.annotation.Size
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.runningFold
import java.util.LinkedList

private const val VALUE_HISTORY_SIZE: Int = 16

internal data class Metrics(
  val value: Float,
  @field:Size(VALUE_HISTORY_SIZE.toLong()) val valueHistory: ImmutableList<Float>,
)

internal fun Flow<Float>.toMetrics(): Flow<Metrics> {
  val circularBuffer = CircularBuffer<Float>()
  // drop(1) to drop the initial value
  return runningFold(Metrics(0f, persistentListOf())) { acc, newValue ->
    circularBuffer.add(newValue)
    Metrics(newValue, circularBuffer.toImmutableList())
  }.drop(1)
}

@AnyThread
private class CircularBuffer<T>(private val capacity: Int = VALUE_HISTORY_SIZE) {
  private val buffer = LinkedList<T>()

  @Synchronized
  fun add(item: T) {
    if (buffer.size >= capacity) {
      buffer.removeFirst()
    }
    buffer.addLast(item)
  }

  @Synchronized
  fun toImmutableList(): ImmutableList<T> = buffer.toImmutableList()
}
