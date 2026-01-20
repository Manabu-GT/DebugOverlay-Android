package com.ms.square.debugoverlay.internal.data.source

import kotlin.time.Duration.Companion.seconds

private const val NANOS_PER_SECOND = 1_000_000_000f

/**
 * Calculates FPS from a stream of frame timestamps.
 *
 * **Thread Safety:** This class is NOT thread-safe. All calls to [onFrame] must
 * originate from the same thread (typically Main thread via Choreographer callbacks).
 *
 * @param intervalNanos The interval over which FPS is calculated (default: 1 second)
 * @param maxFps Maximum FPS cap (typically device's max refresh rate)
 */
internal class FpsCalculator(
  private val intervalNanos: Long = 1.seconds.inWholeNanoseconds,
  private val maxFps: Float = Float.MAX_VALUE,
) {

  private var frameCount: Long = 0
  private var startFrameTimeNanos: Long = 0

  /**
   * Process a frame timestamp and potentially return an FPS value.
   *
   * @param frameTimeNanos The timestamp of the current frame in nanoseconds
   * @return The calculated FPS if the interval has elapsed, null otherwise
   */
  fun onFrame(frameTimeNanos: Long): Float? {
    if (startFrameTimeNanos > 0) {
      frameCount++
      val elapsedTimeNanos = frameTimeNanos - startFrameTimeNanos
      if (elapsedTimeNanos >= intervalNanos) {
        // Use floating-point division to handle sub-second intervals correctly
        val elapsedSeconds = elapsedTimeNanos / NANOS_PER_SECOND
        val fps = if (elapsedSeconds > 0) frameCount / elapsedSeconds else 0f
        // Reset for next interval
        startFrameTimeNanos = frameTimeNanos
        frameCount = 0
        return fps.coerceIn(0f, maxFps)
      }
    } else {
      // First frame establishes baseline
      startFrameTimeNanos = frameTimeNanos
    }
    return null
  }
}
