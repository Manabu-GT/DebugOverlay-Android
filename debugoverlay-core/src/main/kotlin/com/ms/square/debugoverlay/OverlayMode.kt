package com.ms.square.debugoverlay

/**
 * Overlay display mode.
 *
 * Configure via [DebugOverlay.configure]:
 * ```kotlin
 * DebugOverlay.configure {
 *   copy(overlayMode = OverlayMode.BugReporterOnly)
 * }
 * ```
 */
public sealed interface OverlayMode {
  /**
   * Shows real-time metrics panel (CPU, Memory, FPS).
   * Tapping opens the debug panel.
   * Best for developers during active development/testing.
   */
  public data object FullMetrics : OverlayMode

  /**
   * Shows a minimal bug reporter FAB.
   * Tapping captures screenshot and generates a bug report.
   * Best for QA/PM builds where real-time metrics panel isn't needed.
   */
  public data object BugReporterOnly : OverlayMode
}
