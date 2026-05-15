package com.ms.square.debugoverlay.internal.data

import com.google.common.truth.Truth.assertThat
import com.ms.square.debugoverlay.internal.InternalDebugOverlayApi
import org.junit.Test

@OptIn(InternalDebugOverlayApi::class)
class EvictingQueueTest {

  @Test
  fun `add returns null when queue is not at capacity`() {
    val queue = EvictingQueue<String>(3)

    assertThat(queue.add("first")).isNull()
    assertThat(queue.add("second")).isNull()
    assertThat(queue.add("third")).isNull()
  }

  @Test
  fun `add returns evicted element when at capacity`() {
    val queue = EvictingQueue<String>(2)

    queue.add("first")
    queue.add("second")

    val evicted = queue.add("third")
    assertThat(evicted).isEqualTo("first")
  }

  @Test
  fun `add evicts oldest element first`() {
    val queue = EvictingQueue<Int>(3)

    queue.add(1)
    queue.add(2)
    queue.add(3)

    assertThat(queue.add(4)).isEqualTo(1)
    assertThat(queue.add(5)).isEqualTo(2)
    assertThat(queue.add(6)).isEqualTo(3)

    assertThat(queue.toList()).containsExactly(4, 5, 6).inOrder()
  }

  @Test
  fun `add with capacity of 1 always evicts previous element`() {
    val queue = EvictingQueue<String>(1)

    assertThat(queue.add("first")).isNull()
    assertThat(queue.add("second")).isEqualTo("first")
    assertThat(queue.add("third")).isEqualTo("second")

    assertThat(queue.toList()).containsExactly("third")
  }

  @Test
  fun `size does not exceed capacity`() {
    val queue = EvictingQueue<String>(2)

    queue.add("first")
    queue.add("second")
    queue.add("third")
    queue.add("fourth")

    assertThat(queue.size).isEqualTo(2)
  }

  @Test
  fun `toList returns empty list when queue is empty`() {
    val queue = EvictingQueue<String>(5)
    assertThat(queue.toList()).isEmpty()
  }

  @Test
  fun `toList returns elements in insertion order`() {
    val queue = EvictingQueue<String>(5)

    queue.add("first")
    queue.add("second")
    queue.add("third")

    assertThat(queue.toList()).containsExactly("first", "second", "third").inOrder()
  }

  @Test
  fun `toList returns defensive copy`() {
    val queue = EvictingQueue<String>(5)
    queue.add("first")

    val list1 = queue.toList()
    val list2 = queue.toList()

    assertThat(list1).isNotSameInstanceAs(list2)
    assertThat(list1).isEqualTo(list2)
  }

  @Test
  fun `addAndSnapshot returns list containing added element`() {
    val queue = EvictingQueue<String>(5)
    queue.add("first")

    val result = queue.addAndSnapshot("second")

    assertThat(result).containsExactly("first", "second").inOrder()
  }

  @Test
  fun `addAndSnapshot respects capacity`() {
    val queue = EvictingQueue<Int>(2)

    queue.add(1)
    queue.add(2)
    val result = queue.addAndSnapshot(3)

    assertThat(result).containsExactly(2, 3).inOrder()
  }

  @Test
  fun `add and toList handle null elements`() {
    val queue = EvictingQueue<String?>(3)

    queue.add(null)
    queue.add("value")
    queue.add(null)

    assertThat(queue.toList()).containsExactly(null, "value", null).inOrder()
  }

  @Test
  fun `capacity reflects initial value`() {
    val queue = EvictingQueue<String>(5)
    assertThat(queue.capacity).isEqualTo(5)
  }

  @Test
  fun `capacity setter grows queue without dropping elements`() {
    val queue = EvictingQueue<Int>(2)
    queue.add(1)
    queue.add(2)

    queue.capacity = 4

    assertThat(queue.capacity).isEqualTo(4)
    assertThat(queue.toList()).containsExactly(1, 2).inOrder()
    assertThat(queue.add(3)).isNull()
    assertThat(queue.add(4)).isNull()
    assertThat(queue.toList()).containsExactly(1, 2, 3, 4).inOrder()
  }

  @Test
  fun `capacity setter shrinks queue and evicts oldest elements`() {
    val queue = EvictingQueue<Int>(5)
    queue.add(1)
    queue.add(2)
    queue.add(3)
    queue.add(4)
    queue.add(5)

    queue.capacity = 3

    assertThat(queue.capacity).isEqualTo(3)
    assertThat(queue.toList()).containsExactly(3, 4, 5).inOrder()
  }

  @Test
  fun `add respects new capacity after grow then shrink`() {
    val queue = EvictingQueue<Int>(2)
    queue.capacity = 4
    queue.add(1)
    queue.add(2)
    queue.add(3)
    queue.add(4)

    assertThat(queue.add(5)).isEqualTo(1)
    assertThat(queue.toList()).containsExactly(2, 3, 4, 5).inOrder()
  }
}
