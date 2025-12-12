package com.ms.square.debugoverlay.internal.data

import androidx.annotation.RestrictTo
import com.ms.square.debugoverlay.internal.InternalDebugOverlayApi

/**
 * A bounded queue that automatically evicts the oldest element when capacity is reached.
 *
 * Thread-safe: all operations are synchronized.
 *
 * @param capacity Maximum number of elements to retain. Must be positive.
 */
@InternalDebugOverlayApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class EvictingQueue<T>(private val capacity: Int) {

  init {
    require(capacity > 0) { "capacity must be positive, was $capacity" }
  }

  private val queue = ArrayDeque<T>(capacity)

  /**
   * Adds an element to the queue, evicting the oldest if at capacity.
   *
   * @return The evicted element, or null if no eviction occurred.
   */
  @Synchronized
  public fun add(item: T): T? {
    val evicted = if (queue.size >= capacity) {
      queue.removeFirst()
    } else {
      null
    }
    queue.addLast(item)
    return evicted
  }

  /**
   * Adds an element to the queue and returns a defensive copy of the queue as a list.
   */
  @Synchronized
  public fun addAndSnapshot(item: T): List<T> {
    add(item)
    return queue.toList()
  }

  /**
   * Returns the current size of the queue.
   */
  public val size: Int
    @Synchronized get() = queue.size

  /**
   * Returns a defensive copy of the queue as a list.
   */
  @Synchronized
  public fun toList(): List<T> = queue.toList()
}
