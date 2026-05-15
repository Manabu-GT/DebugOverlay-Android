package com.ms.square.debugoverlay.internal.data.source

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LogcatDataSourceTest {

  private val scope = TestScope(StandardTestDispatcher())

  @Test
  fun `maxEntries reflects initial value`() {
    val dataSource = LogcatDataSource(scope, initialMaxEntries = 100)

    assertThat(dataSource.maxEntries).isEqualTo(100)
  }

  @Test
  fun `maxEntries setter updates the underlying buffer cap`() {
    val dataSource = LogcatDataSource(scope, initialMaxEntries = 100)

    dataSource.maxEntries = 50

    assertThat(dataSource.maxEntries).isEqualTo(50)
  }
}
