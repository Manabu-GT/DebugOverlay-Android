package com.ms.square.debugoverlay.internal.data

import com.google.common.truth.Truth.assertThat
import com.ms.square.debugoverlay.internal.InternalDebugOverlayApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test

@OptIn(InternalDebugOverlayApi::class)
class EvictingQueueTest {

  @Test(expected = IllegalArgumentException::class)
  fun `constructor throws when capacity is zero`() {
    EvictingQueue<String>(0)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `constructor throws when capacity is negative`() {
    EvictingQueue<String>(-1)
  }

  @Test
  fun `constructor succeeds with capacity of 1`() {
    val queue = EvictingQueue<String>(1)
    assertThat(queue.size).isEqualTo(0)
  }

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
  fun `concurrent adds do not lose elements or exceed capacity`() = runBlocking {
    val capacity = 100
    val queue = EvictingQueue<Int>(capacity)
    val numAdds = 1000

    val startSignal = CompletableDeferred<Unit>()

    val jobs = (1..numAdds).map { i ->
      launch(Dispatchers.Default) {
        startSignal.await()
        queue.add(i)
      }
    }
    startSignal.complete(Unit)
    jobs.joinAll()

    // 1. Verify Size
    assertThat(queue.size).isEqualTo(capacity)
    val finalContent = queue.toList()
    assertThat(finalContent).hasSize(capacity)

    // 2. Verify Data Consistency (Addressing "corrupted or duplicated elements")
    assertThat(finalContent.toSet()).hasSize(capacity)

    // 3. Verify No Data Corruption
    // Ensure every element in the queue is actually one of the numbers we added.
    finalContent.forEach { value ->
      assertThat(value).isIn(1..numAdds)
    }
  }

  @Test
  fun `concurrent reads and writes maintain integrity`() = runBlocking {
    val capacity = 50
    val queue = EvictingQueue<Int>(capacity)
    val iterations = 1000
    val startSignal = CompletableDeferred<Unit>()

    val writeJobs = (1..iterations).map { i ->
      launch(Dispatchers.Default) {
        startSignal.await()
        queue.add(i)
      }
    }

    val readJobs = (1..iterations).map {
      launch(Dispatchers.Default) {
        startSignal.await()
        queue.toList()
        queue.size
      }
    }

    startSignal.complete(Unit)
    (writeJobs + readJobs).joinAll()

    // 1. Verify Size
    assertThat(queue.size).isEqualTo(capacity)
    val finalContent = queue.toList()
    assertThat(finalContent.size).isEqualTo(capacity)

    // 2. Verify Data Consistency (Addressing "corrupted or duplicated elements")
    assertThat(finalContent.toSet()).hasSize(capacity)

    // 3. Verify No Data Corruption
    // Ensure every element in the queue is actually one of the numbers we added.
    finalContent.forEach { value ->
      assertThat(value).isIn(1..iterations)
    }
  }

  @Test
  fun `concurrent addAndSnapshot maintains atomicity`() = runBlocking {
    val capacity = 100
    val queue = EvictingQueue<Int>(capacity)
    val iterations = 1000
    val startSignal = CompletableDeferred<Unit>()

    // Launch many threads that try to add and snapshot simultaneously
    val jobs = (1..iterations).map { i ->
      async(Dispatchers.Default) {
        startSignal.await()
        // The result MUST contain 'i' as the last element because
        // the method is synchronized. If it wasn't atomic, another
        // thread could sneak in an add() before the snapshot was taken.
        val snapshot = queue.addAndSnapshot(i)

        // Return the verification result for this thread
        snapshot.last() == i
      }
    }

    startSignal.complete(Unit)
    val results = jobs.awaitAll()

    // Verify every single thread got its own item as the last element
    assertThat(results).doesNotContain(false)
    assertThat(queue.size).isEqualTo(capacity)
  }

  @Test
  fun `add and toList handle null elements`() {
    val queue = EvictingQueue<String?>(3)

    queue.add(null)
    queue.add("value")
    queue.add(null)

    assertThat(queue.toList()).containsExactly(null, "value", null).inOrder()
  }
}
