package com.ms.square.debugoverlay.internal.data.source

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.concurrent.TimeUnit

class FpsCalculatorTest {

  private val baseTimeNanos = TimeUnit.SECONDS.toNanos(1000)

  private val calculator = FpsCalculator()

  @Test
  fun `first frame establishes baseline and returns null`() {
    val result = calculator.onFrame(frameTimeNanos = baseTimeNanos)

    assertThat(result).isNull()
  }

  @Test
  fun `second frame within interval returns null`() {
    calculator.onFrame(frameTimeNanos = baseTimeNanos) // Baseline
    val result = calculator.onFrame(frameTimeNanos = baseTimeNanos + TimeUnit.MILLISECONDS.toNanos(500))

    assertThat(result).isNull()
  }

  @Test
  fun `returns FPS when interval elapses`() {
    calculator.onFrame(frameTimeNanos = baseTimeNanos) // Baseline

    // Simulate 60 frames over 1 second (one every ~16.67ms)
    for (frame in 1..59) {
      calculator.onFrame(frameTimeNanos = baseTimeNanos + TimeUnit.MILLISECONDS.toNanos(frame * 16L))
    }

    // 60th frame at 1 second mark should trigger emission
    val result = calculator.onFrame(frameTimeNanos = baseTimeNanos + TimeUnit.SECONDS.toNanos(1))

    assertThat(result).isNotNull()
    assertThat(result).isEqualTo(60f)
  }

  @Test
  fun `FPS capped at maxFps`() {
    val calculator = FpsCalculator(
      maxFps = 60f
    )

    calculator.onFrame(frameTimeNanos = baseTimeNanos) // Baseline

    // Simulate 120 frames over 1 second (would be 120 FPS without cap)
    for (frame in 1..119) {
      calculator.onFrame(frameTimeNanos = baseTimeNanos + TimeUnit.MICROSECONDS.toNanos(frame * 8333L))
    }
    val result = calculator.onFrame(frameTimeNanos = baseTimeNanos + TimeUnit.SECONDS.toNanos(1))

    assertThat(result).isEqualTo(60f) // Capped at maxFps
  }

  @Test
  fun `FPS never negative`() {
    calculator.onFrame(frameTimeNanos = baseTimeNanos) // Baseline

    // Only 1 frame in the interval (the emission frame itself)
    val result = calculator.onFrame(frameTimeNanos = baseTimeNanos + TimeUnit.SECONDS.toNanos(1))

    assertThat(result).isAtLeast(0f)
  }

  @Test
  fun `frame counter resets after emission`() {
    calculator.onFrame(frameTimeNanos = baseTimeNanos) // Baseline

    // First interval: 60 frames
    for (frame in 1..59) {
      calculator.onFrame(frameTimeNanos = baseTimeNanos + TimeUnit.MILLISECONDS.toNanos(frame * 16L))
    }
    val first = calculator.onFrame(frameTimeNanos = baseTimeNanos + TimeUnit.SECONDS.toNanos(1))
    assertThat(first).isEqualTo(60f)

    // Second interval: 30 frames
    for (frame in 1..29) {
      calculator.onFrame(
        frameTimeNanos =
        baseTimeNanos + TimeUnit.SECONDS.toNanos(1) + TimeUnit.MILLISECONDS.toNanos(frame * 33L)
      )
    }
    val second = calculator.onFrame(frameTimeNanos = baseTimeNanos + TimeUnit.SECONDS.toNanos(2))

    assertThat(second).isEqualTo(30f)
  }

  @Test
  fun `baseline time updates after each emission`() {
    val calculator = FpsCalculator(intervalNanos = TimeUnit.MILLISECONDS.toNanos(100))

    calculator.onFrame(frameTimeNanos = baseTimeNanos) // Baseline

    // First 100ms: 6 frames = 60 FPS
    for (frame in 1..5) {
      calculator.onFrame(frameTimeNanos = baseTimeNanos + TimeUnit.MILLISECONDS.toNanos(frame * 16L))
    }
    val first = calculator.onFrame(frameTimeNanos = baseTimeNanos + TimeUnit.MILLISECONDS.toNanos(100))
    assertThat(first).isEqualTo(60f)

    // Second 100ms starting from 100ms mark: 12 frames = 120 FPS
    for (frame in 1..11) {
      calculator.onFrame(frameTimeNanos = baseTimeNanos + TimeUnit.MILLISECONDS.toNanos(100 + frame * 8L))
    }
    val second = calculator.onFrame(frameTimeNanos = baseTimeNanos + TimeUnit.MILLISECONDS.toNanos(200))

    assertThat(second).isEqualTo(120f)
  }

  @Test
  fun `handles very short intervals`() {
    val calculator = FpsCalculator(intervalNanos = TimeUnit.MILLISECONDS.toNanos(16)) // ~60Hz interval

    calculator.onFrame(frameTimeNanos = baseTimeNanos) // Baseline
    val result = calculator.onFrame(frameTimeNanos = baseTimeNanos + TimeUnit.MILLISECONDS.toNanos(16))

    assertThat(result).isNotNull()
  }

  @Test
  fun `handles elapsed time past interval boundary`() {
    calculator.onFrame(frameTimeNanos = baseTimeNanos) // Baseline

    // Add frames at 17ms intervals. Emission happens when elapsed >= 1000ms.
    // Frame 59 at 1003ms triggers emission (59 frames in ~1.003 seconds ≈ 58.8 FPS)
    var emittedFps: Float? = null
    for (frame in 1..70) {
      val frameTime = baseTimeNanos + TimeUnit.MILLISECONDS.toNanos(frame * 17L)
      val result = calculator.onFrame(frameTimeNanos = frameTime)
      if (result != null && emittedFps == null) {
        emittedFps = result
      }
    }

    // First emission should happen when crossing 1000ms boundary
    assertThat(emittedFps).isNotNull()
    // 59 frames in 1003ms ≈ 58.82 FPS
    assertThat(emittedFps).isWithin(0.1f).of(58.82f)
  }
}
