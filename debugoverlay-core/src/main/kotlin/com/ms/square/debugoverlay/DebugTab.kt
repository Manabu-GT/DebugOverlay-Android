package com.ms.square.debugoverlay

import androidx.compose.runtime.Composable

/**
 * A custom tab displayed in the debug panel.
 *
 * Custom tabs are appended after the built-in tabs (Logcat, Network, etc.).
 * Configure via [OverlayMode.FullMetrics]:
 *
 * ```kotlin
 * DebugOverlay.configure {
 *   overlayMode = OverlayMode.FullMetrics(
 *     customTabs = listOf(
 *       DebugTab(title = "Feature Flags") { FeatureFlagsContent() }
 *     )
 *   )
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
public class DebugTab(internal val title: String, internal val content: @Composable () -> Unit)
