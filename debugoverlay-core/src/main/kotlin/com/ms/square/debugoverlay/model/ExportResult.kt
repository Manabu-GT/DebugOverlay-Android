package com.ms.square.debugoverlay.model

import com.ms.square.debugoverlay.BugReportExporter

/**
 * Result of an export operation.
 *
 * Captures the outcome of [BugReportExporter.export] with semantic meaning
 * appropriate for different export types.
 *
 * ## Usage Guidelines
 * - **[Initiated]**: Use when you cannot verify delivery (e.g., share sheet, email client).
 *   The bug report draft is retained for potential re-sharing.
 * - **[Success]**: Use when you have confirmation of successful delivery (e.g., HTTP 2xx
 *   from Jira/GitHub/Slack). The draft is deleted to save space.
 * - **[Failure]**: Use when the export fails and the user should retry. The draft is
 *   retained and an error message is shown.
 */
public sealed interface ExportResult {
  /**
   * Export was initiated but outcome is unknown.
   *
   * Used for share sheet exports where we launch the chooser but cannot
   * detect whether the user actually shared or cancelled.
   */
  public data object Initiated : ExportResult

  /**
   * Export completed successfully with confirmed delivery.
   *
   * Used for HTTP-based exporters (Jira, Slack, etc.) that can verify
   * the upload was received by the server.
   */
  public data object Success : ExportResult

  /**
   * Export failed.
   *
   * @param cause The underlying exception, if available
   */
  public data class Failure(val cause: Throwable? = null) : ExportResult
}
