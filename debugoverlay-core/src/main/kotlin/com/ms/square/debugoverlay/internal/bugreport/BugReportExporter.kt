package com.ms.square.debugoverlay.internal.bugreport

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.ms.square.debugoverlay.core.R
import com.ms.square.debugoverlay.internal.Logger
import com.ms.square.debugoverlay.internal.bugreport.model.BugReportArchive
import com.ms.square.debugoverlay.internal.bugreport.model.BugReportArchiveImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Result of an export operation.
 *
 * Captures the outcome of [BugReportExporter.export] with semantic meaning
 * appropriate for different export types.
 */
internal sealed interface ExportResult {
  /**
   * Export was initiated but outcome is unknown.
   *
   * Used for share sheet exports where we launch the chooser but cannot
   * detect whether the user actually shared or cancelled.
   */
  data object Initiated : ExportResult

  /**
   * Export completed successfully with confirmed delivery.
   *
   * Used for HTTP-based exporters (Jira, Slack, etc.) that can verify
   * the upload was received by the server.
   */
  data object Success : ExportResult

  /**
   * Export failed.
   *
   * @param cause The underlying exception, if available
   */
  data class Failure(val cause: Throwable? = null) : ExportResult
}

/**
 * Interface for exporting bug reports.
 *
 * Currently internal, expected to be made public in the future to allow
 * custom integrations (Jira, GitHub Issues, Slack, etc.).
 */
internal interface BugReportExporter {
  /**
   * Export the bug report.
   *
   * Called on [kotlinx.coroutines.Dispatchers.IO]. Blocking I/O operations
   * (network uploads, file reads) are safe. For UI operations, use
   * `withContext(Dispatchers.Main)`.
   *
   * @param report The generated bug report archive
   * @return The result of the export operation
   */
  suspend fun export(report: BugReportArchive): ExportResult
}

private const val PROVIDER_AUTHORITY_SUFFIX = ".debugoverlay.bugreport.provider"

/**
 * Default exporter that shares the bug report via Android's share sheet.
 * Uses Intent.ACTION_SEND with FileProvider for secure file sharing.
 */
internal class IntentShareExporter(private val context: Context) : BugReportExporter {

  override suspend fun export(report: BugReportArchive): ExportResult {
    check(report is BugReportArchiveImpl) { "IntentShareExporter requires BugReportArchiveImpl" }
    val file = report.file
    val authority = "${context.packageName}$PROVIDER_AUTHORITY_SUFFIX"
    val uri = FileProvider.getUriForFile(context, authority, file)

    val subject = context.getString(R.string.debugoverlay_bug_report_subject, file.nameWithoutExtension)
    val chooserTitle = context.getString(R.string.debugoverlay_share_bug_report)

    val intent = Intent(Intent.ACTION_SEND).apply {
      type = "application/zip"
      clipData = ClipData.newRawUri(null, uri)
      putExtra(Intent.EXTRA_STREAM, uri)
      putExtra(Intent.EXTRA_SUBJECT, subject)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    return runCatching {
      // Switch to Main for UI operation (startActivity)
      withContext(Dispatchers.Main) {
        context.startActivity(
          Intent.createChooser(intent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          }
        )
      }
      ExportResult.Initiated
    }.getOrElse { e ->
      Logger.w("Failed to share bug report", e)
      ExportResult.Failure(e)
    }
  }
}
