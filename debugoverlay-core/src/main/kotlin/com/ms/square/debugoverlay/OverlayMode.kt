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
   * Modes that support debug panel tabs and accept custom tabs.
   * Implemented by [FullMetrics] and [Hidden].
   */
  public sealed interface WithCustomTabs : OverlayMode {
    public val customTabs: List<DebugTab>
  }

  /**
   * Shows real-time metrics panel (CPU, Memory, FPS).
   * Tapping opens the debug panel.
   * Best for developers during active development/testing.
   *
   * @param customTabs Custom tabs appended after the built-in tabs in the debug panel.
   * @param showThermal When `true`, adds a thermal-status row to the real-time metrics panel.
   *   Requires Android 11 (API 30) or above with a working thermal HAL — the row stays hidden
   *   on older devices and on API 30+ devices whose HAL does not expose `getThermalHeadroom`
   *   data. If a HAL later starts reporting real values, the row appears on the next poll.
   *   Defaults to `false`.
   */
  public data class FullMetrics(
    override val customTabs: List<DebugTab> = emptyList(),
    public val showThermal: Boolean = false,
  ) : WithCustomTabs

  /**
   * Shows a minimal bug reporter FAB.
   * Tapping captures screenshot and generates a bug report.
   * Best for QA/PM builds where real-time metrics panel isn't needed.
   */
  public data object BugReporterOnly : OverlayMode

  /**
   * No on-screen overlay. Use [DebugOverlay.openPanel] to open the panel programmatically.
   *
   * Use when the overlay would interfere with QA testing or when launching
   * the panel from your own debug menu, notification action, or gesture handler.
   *
   * @param customTabs Custom tabs appended after the built-in tabs in the debug panel.
   */
  public data class Hidden(override val customTabs: List<DebugTab> = emptyList()) : WithCustomTabs
}
