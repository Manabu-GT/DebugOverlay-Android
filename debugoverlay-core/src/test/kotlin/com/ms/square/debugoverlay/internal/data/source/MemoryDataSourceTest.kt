package com.ms.square.debugoverlay.internal.data.source

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class MemoryDataSourceTest {

  private val testDispatcher = StandardTestDispatcher()

  private val dataSource = MemoryDataSource(
    RuntimeEnvironment.getApplication(),
    defaultDispatcher = testDispatcher,
    ioDispatcher = testDispatcher
  )

  @Test
  fun `heapUsage emits Percentage value when subscribed`() = runTest(testDispatcher) {
    val item = dataSource.heapUsage().first()

    assertThat(item.value).isAtLeast(0f)
    assertThat(item.value).isAtMost(100f)
  }

  @Test
  fun `heapUsage emissions respect interval`() = runTest(testDispatcher) {
    dataSource.heapUsage(interval = 100.milliseconds).test {
      awaitItem() // first emission

      advanceTimeBy(50.milliseconds)
      expectNoEvents() // no emission yet

      advanceTimeBy(50.milliseconds)
      awaitItem() // second emission after interval

      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `pss emits when subscribed`() = runTest(testDispatcher) {
    val item = dataSource.pss().first()

    assertThat(item).isAtLeast(0f)
  }

  @Test
  fun `pss emissions respect interval`() = runTest(testDispatcher) {
    dataSource.pss(interval = 100.milliseconds).test {
      awaitItem() // first emission

      advanceTimeBy(50.milliseconds)
      expectNoEvents() // no emission yet

      advanceTimeBy(50.milliseconds)
      awaitItem() // second emission after interval

      cancelAndIgnoreRemainingEvents()
    }
  }
}
