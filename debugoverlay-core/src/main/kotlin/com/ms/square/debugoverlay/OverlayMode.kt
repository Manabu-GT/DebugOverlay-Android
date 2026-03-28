package com.ms.square.debugoverlay

/**
 * Overlay display mode.
 *
 * Configure via [DebugOverlay.configure]:
 * ```kotlin
 * DebugOverlay.configure {
 *   overlayMode = OverlayMode.BugReporterOnly
 * }
 * ```
 */
public sealed interface OverlayMode {
  /**
   * Shows real-time metrics panel (CPU, Memory, FPS).
   * Tapping opens the debug panel.
   * Best for developers during active development/testing.
   *
   * @param tabs Controls which tabs appear and their order.
   *   Default: [DebugTab.defaults] (all built-in tabs in standard order).
   *   When overridden, only the specified tabs appear, in the given order.
   *   The Custom Log tab is auto-injected when a [LogSource] is configured
   *   and does not need to be included here.
   */
  public data class FullMetrics(val tabs: List<DebugTab> = DebugTab.defaults) : OverlayMode

  /**
   * Shows a minimal bug reporter FAB.
   * Tapping captures screenshot and generates a bug report.
   * Best for QA/PM builds where real-time metrics panel isn't needed.
   */
  public data object BugReporterOnly : OverlayMode
}
