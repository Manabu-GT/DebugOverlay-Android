package com.ms.square.debugoverlay

import com.ms.square.debugoverlay.internal.util.formatTimestamp
import com.ms.square.debugoverlay.model.BugReportSummary

/**
 * Formats a [BugReportSummary] into a Markdown string suitable for issue trackers.
 *
 * The output includes:
 * - **Summary** section with the bug report title
 * - **Details** section with the user-provided description (if present)
 * - **Environment** section with app and device information
 * - **Attachments** reminder to include the ZIP file
 *
 * Example usage in a custom exporter:
 * ```kotlin
 * class GitHubExporter(...) : BugReportExporter {
 *   override suspend fun export(context: Context, report: BugReport): ExportResult {
 *     val markdown = formatBugReportMarkdown(report.summary)
 *     // Use markdown as the issue body
 *   }
 * }
 * ```
 */
public fun formatBugReportMarkdown(summary: BugReportSummary): String = buildString {
  appendLine("## Summary")
  appendLine()
  appendLine(summary.title)
  appendLine()

  if (!summary.description.isNullOrBlank()) {
    appendLine("## Details")
    appendLine()
    appendLine(summary.description)
    appendLine()
  }

  appendLine("## Environment")
  appendLine()
  appendLine("| Field | Value |")
  appendLine("|-------|-------|")
  appendLine("| Package | ${summary.appInfo.packageName} |")
  summary.appInfo.versionName?.let { appendLine("| Version | $it (${summary.appInfo.versionCode}) |") }
  appendLine("| Debuggable | ${summary.appInfo.isDebuggable} |")
  summary.deviceInfo?.let { device ->
    appendLine("| Device | ${device.manufacturer} ${device.model} |")
    appendLine("| Android | ${device.androidVersion} (API ${device.apiLevel}) |")
    appendLine("| Locale | ${device.locale} |")
  }
  appendLine("| Captured | ${formatTimestamp(summary.capturedAt)} |")
  appendLine()

  appendLine("## Attachments")
  appendLine()
  appendLine("See attached ZIP file for full diagnostic data (screenshots, logs, device info).")
}
