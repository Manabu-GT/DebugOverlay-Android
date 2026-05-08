package com.ms.square.debugoverlay.extension.trigger.shake

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Parity tests ported from Square's Seismic
 * (https://github.com/square/seismic/blob/master/library/src/test/java/com/squareup/seismic/ShakeDetectorTest.java)
 * to verify the Kotlin port preserves the upstream sample-queue and shake-detection
 * behavior exactly.
 */
class ShakeDetectorTest {

  @Test
  fun `initial queue is not shaking`() {
    val q = ShakeDetector.SampleQueue()
    assertThat(q.isShaking).isFalse()
  }

  /** LG Ally sample rate — the slowest device upstream Seismic targets. */
  @Test
  fun `LG Ally rate transitions through shaking and back`() {
    val q = ShakeDetector.SampleQueue()

    // The queue holds 500_000_000 ns (0.5s) of samples or 4 samples, whichever is greater.
    q.add(1_000_000_000L, false)
    q.add(1_300_000_000L, false)
    q.add(1_600_000_000L, false)
    q.add(1_900_000_000L, false)
    assertContent(q, false, false, false, false)
    assertThat(q.isShaking).isFalse()

    // The oldest two entries will be removed.
    q.add(2_200_000_000L, true)
    q.add(2_500_000_000L, true)
    assertContent(q, false, false, true, true)
    assertThat(q.isShaking).isFalse()

    // Another entry should be removed; now 3 out of 4 are true.
    q.add(2_800_000_000L, true)
    assertContent(q, false, true, true, true)
    assertThat(q.isShaking).isTrue()

    q.add(3_100_000_000L, false)
    assertContent(q, true, true, true, false)
    assertThat(q.isShaking).isTrue()

    q.add(3_400_000_000L, false)
    assertContent(q, true, true, false, false)
    assertThat(q.isShaking).isFalse()
  }

  @Test
  fun `clear resets isShaking to false`() {
    val q = ShakeDetector.SampleQueue()
    q.add(1_000_000_000L, true)
    q.add(1_200_000_000L, true)
    q.add(1_400_000_000L, true)
    assertThat(q.isShaking).isTrue()
    q.clear()
    assertThat(q.isShaking).isFalse()
  }

  private fun assertContent(q: ShakeDetector.SampleQueue, vararg expected: Boolean) {
    val samples = q.asList()
    assertThat(samples).hasSize(expected.size)
    expected.forEachIndexed { i, value ->
      assertThat(samples[i].accelerating).isEqualTo(value)
    }
  }
}
