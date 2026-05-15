package com.ms.square.debugoverlay.internal.data

import androidx.annotation.GuardedBy
import androidx.annotation.IntRange
import androidx.annotation.RestrictTo
import com.ms.square.debugoverlay.internal.InternalDebugOverlayApi

/**
 * A bounded queue that automatically evicts the oldest element when capacity is reached.
 *
 * Thread-safe: all operations are synchronized on the instance monitor. The [capacity]
 * property is mutable; assigning a new value resizes the queue (evicting from the head
 * if shrinking below current size).
 *
 * @param initialCapacity Initial maximum number of elements to retain. Must be positive.
 */
@InternalDebugOverlayApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class EvictingQueue<T>(initialCapacity: Int) {

  @GuardedBy("this")
  private val queue = ArrayDeque<T>(initialCapacity)

  /**
   * Current maximum number of elements the queue will retain.
   * Assigning a smaller value evicts the oldest elements until size <= capacity.
   */
  @IntRange(from = 1)
  @GuardedBy("this")
  public var capacity: Int = initialCapacity
    @Synchronized get

    @Synchronized set(value) {
      field = value
      while (queue.size > value) {
        queue.removeFirst()
      }
    }

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

  /**
   * Removes all elements from the queue.
   */
  @Synchronized
  public fun clear() {
    queue.clear()
  }
}
