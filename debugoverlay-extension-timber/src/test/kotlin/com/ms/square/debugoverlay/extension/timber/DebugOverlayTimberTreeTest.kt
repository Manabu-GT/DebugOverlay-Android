package com.ms.square.debugoverlay.extension.timber

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DebugOverlayTimberTreeTest {

  private val tree = DebugOverlayTimberTree()

  @Test
  fun `clear empties captured logs and subsequent collection still works`() = runTest {
    tree.i("first")
    assertThat(tree.logs.first()).hasSize(1)

    tree.clear()

    assertThat(tree.logs.first()).isEmpty()

    tree.i("second")
    val afterClear = tree.logs.first()
    assertThat(afterClear).hasSize(1)
    assertThat(afterClear.first().message).isEqualTo("second")
  }
}
