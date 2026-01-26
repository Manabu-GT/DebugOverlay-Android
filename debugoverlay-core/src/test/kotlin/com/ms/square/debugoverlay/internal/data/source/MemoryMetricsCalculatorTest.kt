package com.ms.square.debugoverlay.internal.data.source

import com.google.common.truth.Truth.assertThat
import com.ms.square.debugoverlay.internal.data.Percentage
import org.junit.Test

class MemoryMetricsCalculatorTest {

  @Test
  fun `calculateMaxPss uses memoryClass for low-RAM devices`() {
    val result = MemoryMetricsCalculator.calculateMaxPss(
      memoryClass = 384,
      largeMemoryClass = 512,
      isLowRamDevice = true
    )

    // 384 * 2 = 768MB (memoryClass used, not largeMemoryClass)
    assertThat(result).isEqualTo(768f)
  }

  @Test
  fun `calculateMaxPss uses largeMemoryClass for normal devices`() {
    val result = MemoryMetricsCalculator.calculateMaxPss(
      memoryClass = 192,
      largeMemoryClass = 512,
      isLowRamDevice = false
    )

    // 512 * 2 = 1024MB
    assertThat(result).isEqualTo(1024f)
  }

  @Test
  fun `calculateMaxPss enforces minimum of 512MB`() {
    val result = MemoryMetricsCalculator.calculateMaxPss(
      memoryClass = 128,
      largeMemoryClass = 256,
      isLowRamDevice = true
    )

    // 128 * 2 = 256MB, but clamped to 512MB minimum
    assertThat(result).isEqualTo(512f)
  }

  @Test
  fun `calculateMaxPss handles zero memoryClass`() {
    val result = MemoryMetricsCalculator.calculateMaxPss(
      memoryClass = 0,
      largeMemoryClass = 0,
      isLowRamDevice = false
    )

    // 0 * 2 = 0MB, clamped to 512MB minimum
    assertThat(result).isEqualTo(512f)
  }

  @Test
  fun `calculateHeapUsagePercentage returns correct percentage`() {
    val result = MemoryMetricsCalculator.calculateHeapUsagePercentage(
      usedMemory = 50_000_000L,
      maxMemory = 100_000_000L
    )

    assertThat(result).isEqualTo(Percentage.ofClamped(0.5f))
  }

  @Test
  fun `calculateHeapUsagePercentage returns 0 when maxMemory is zero or negative`() {
    val zeroResult = MemoryMetricsCalculator.calculateHeapUsagePercentage(
      usedMemory = 50_000_000L,
      maxMemory = 0L
    )
    val negativeResult = MemoryMetricsCalculator.calculateHeapUsagePercentage(
      usedMemory = 50_000_000L,
      maxMemory = -1L
    )

    assertThat(zeroResult).isEqualTo(Percentage.ZERO)
    assertThat(negativeResult).isEqualTo(Percentage.ZERO)
  }

  @Test
  fun `convertKbToMb converts kilobytes to megabytes`() {
    val result = MemoryMetricsCalculator.convertKbToMb(kb = 102400)

    // 102400 KB = 100 MB
    assertThat(result).isEqualTo(100f)
  }
}
