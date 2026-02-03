package com.ms.square.debugoverlay

import android.content.Context
import com.ms.square.debugoverlay.model.BugReport
import com.ms.square.debugoverlay.model.ExportResult

/**
 * Interface for exporting bug reports to external systems.
 *
 * Implement this interface to create custom exporters for systems like
 * Jira, GitHub Issues, Slack, or any other bug tracking tool.
 *
 * Register your exporter via [DebugOverlay.configure]:
 * ```kotlin
 * DebugOverlay.configure {
 *   bugReportExporter = MyJiraExporter(client, projectKey)
 * }
 * ```
 *
 * ## Threading
 * [export] is called on [kotlinx.coroutines.Dispatchers.IO]. Blocking I/O operations
 * (network uploads, file reads) are safe.
 *
 * ## Context Usage
 * The [Context] parameter is provided for operations that require it
 * (e.g., launching share intents, accessing resources). Do not retain the
 * context beyond the scope of the [export] call.
 *
 * ## Example: Jira Exporter
 * ```kotlin
 * class JiraExporter(
 *   private val client: OkHttpClient,
 *   private val jiraBaseUrl: String,
 *   private val projectKey: String,
 * ) : BugReportExporter {
 *
 *   override suspend fun export(context: Context, report: BugReport): ExportResult {
 *     val summary = report.summary
 *     val archive = report.archive
 *     // Create issue, attach ZIP, return ExportResult.Success or Failure
 *   }
 * }
 * ```
 *
 * @see ExportResult
 */
public interface BugReportExporter {
  /**
   * Export the bug report.
   *
   * @param context Android context for resource access
   * @param report The bug report containing archive and summary metadata
   * @return The result of the export operation
   */
  public suspend fun export(context: Context, report: BugReport): ExportResult
}
