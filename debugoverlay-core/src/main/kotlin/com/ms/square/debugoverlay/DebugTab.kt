package com.ms.square.debugoverlay

import androidx.compose.runtime.Composable

/**
 * A tab displayed in the debug panel.
 *
 * Use the pre-defined companion constants for built-in tabs (e.g., [DebugTab.Logcat],
 * [DebugTab.Network]) and the public constructor for custom tabs.
 *
 * ## Custom Tab Example
 * ```kotlin
 * val myTab = DebugTab(title = "Feature Flags") {
 *     FeatureFlagsContent()
 * }
 * ```
 *
 * ## Bug Reports
 * Tabs are UI-only. To include custom data in bug reports, use
 * [DebugOverlay.addBugReportContributor] separately.
 *
 * @param title Display title shown in the tab row.
 * @param content Composable content rendered when this tab is selected.
 */
public class DebugTab(internal val title: String, internal val content: @Composable () -> Unit) {

  public companion object {
    /** Logcat log viewer. */
    public val Logcat: DebugTab = DebugTab("", {})

    /** Network request history (requires [NetworkRequestSource]). */
    public val Network: DebugTab = DebugTab("", {})

    /** Frame performance metrics (JankStats). */
    public val JankStats: DebugTab = DebugTab("", {})

    /** App termination history (API 30+). */
    public val AppExits: DebugTab = DebugTab("", {})

    /** View hierarchy inspector (Radiography). */
    public val Ui: DebugTab = DebugTab("", {})

    /** Device and system information. */
    public val DeviceInfo: DebugTab = DebugTab("", {})

    /** Default tab list used when no explicit tabs are configured. */
    public val defaults: List<DebugTab> = listOf(Logcat, AppExits, Network, JankStats, Ui, DeviceInfo)
  }
}
