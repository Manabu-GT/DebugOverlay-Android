package com.ms.square.debugoverlay.internal.data.source

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ThreadNameCacheTest {

  @Test
  fun `resolve returns main when pid equals tid`() {
    val cache = ThreadNameCache(threadNameResolver = { error("ShouldNotBeCalled") })

    val result = cache.resolve(pid = 1234, tid = 1234)

    assertThat(result).isEqualTo("main")
  }

  @Test
  fun `resolve returns resolver result for non-main thread`() {
    val cache = ThreadNameCache(threadNameResolver = { tid -> "Thread-$tid-resolved" })

    val result = cache.resolve(pid = 1234, tid = 5678)

    assertThat(result).isEqualTo("Thread-5678-resolved")
  }

  @Test
  fun `resolve returns fallback for invalid tid`() {
    val cache = ThreadNameCache(threadNameResolver = { error("ShouldNotBeCalled") })

    assertThat(cache.resolve(pid = 1234, tid = 0)).isEqualTo("Thread-0")
    assertThat(cache.resolve(pid = 1234, tid = -1)).isEqualTo("Thread--1")
  }

  @Test
  fun `resolve falls back to Thread-tid when resolver returns null`() {
    val cache = ThreadNameCache(threadNameResolver = { null })

    val result = cache.resolve(pid = 1234, tid = 5678)

    assertThat(result).isEqualTo("Thread-5678")
  }

  @Test
  fun `resolve caches result and does not call resolver again for same tid`() {
    var callCount = 0
    val cache = ThreadNameCache(threadNameResolver = {
      callCount++
      "Worker"
    })

    cache.resolve(pid = 1234, tid = 5678)
    cache.resolve(pid = 1234, tid = 5678)
    cache.resolve(pid = 1234, tid = 5678)

    assertThat(callCount).isEqualTo(1)
  }
}
