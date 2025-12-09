package com.ms.square.debugoverlay.internal.data

internal class EvictingQueue<T>(private val capacity: Int) {
  private val queue = ArrayDeque<T>(capacity)

  @Synchronized
  fun add(item: T): T? {
    val evicted = if (queue.size >= capacity) {
      queue.removeFirst()
    } else {
      null
    }
    queue.addLast(item)
    return evicted
  }

  @Synchronized
  fun toList(): List<T> = queue.toList()
}
