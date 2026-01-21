package com.ms.square.debugoverlay.internal.data.source

import com.ms.square.debugoverlay.internal.data.Percentage

private const val KB_TO_MB = 1024f
private const val PSS_MULTIPLIER = 2f
private const val MIN_MAX_PSS_MB = 512f

/**
 * Pure calculation logic for memory metrics.
 *
 * **Thread Safety:** All functions are stateless and thread-safe.
 */
internal object MemoryMetricsCalculator {

  /**
   * Calculates the recommended maximum PSS value for graph scaling.
   *
   * Uses device's memory class (per-app limit) as baseline:
   * - Low-RAM devices: use standard [memoryClass] (typically 192-512 MB)
   * - Normal devices: use [largeMemoryClass] (typically 512-1024 MB)
   *
   * PSS includes heap + native + shared libraries, so we apply a 2x multiplier.
   * The result is clamped to at least 512 MB for graph readability.
   *
   * @param memoryClass Device's standard memory class in MB
   * @param largeMemoryClass Device's large memory class in MB
   * @param isLowRamDevice Whether the device is a low-RAM device
   * @return Recommended max PSS in MB, minimum 512MB
   */
  fun calculateMaxPss(memoryClass: Int, largeMemoryClass: Int, isLowRamDevice: Boolean): Float {
    val memoryClassMB = if (isLowRamDevice) {
      memoryClass.toFloat()
    } else {
      largeMemoryClass.toFloat()
    }
    return (memoryClassMB * PSS_MULTIPLIER).coerceAtLeast(MIN_MAX_PSS_MB)
  }

  /**
   * Calculates heap usage percentage.
   *
   * @param usedMemory Used memory in bytes (totalMemory - freeMemory)
   * @param maxMemory Maximum memory the runtime can allocate in bytes
   * @return Heap usage percentage, or Percentage.ZERO if maxMemory is zero
   */
  fun calculateHeapUsagePercentage(usedMemory: Long, maxMemory: Long): Percentage {
    if (maxMemory <= 0) return Percentage.ZERO
    return Percentage.ofClamped(usedMemory.toFloat() / maxMemory)
  }

  /** Converts kilobytes to megabytes. */
  fun convertKbToMb(kb: Int): Float = kb / KB_TO_MB
}
