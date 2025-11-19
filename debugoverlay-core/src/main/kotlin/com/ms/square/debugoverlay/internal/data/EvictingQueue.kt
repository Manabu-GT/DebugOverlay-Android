package com.ms.square.debugoverlay.internal.data

internal class EvictingQueue<T>(private val capacity: Int) {
  private val queue = ArrayDeque<T>(capacity)

  @Synchronized
  fun add(item: T) {
    if (queue.size >= capacity) {
      queue.removeFirst()
    }
    queue.addLast(item)
  }

  @Synchronized
  fun toList(): List<T> = queue.toList()
}
