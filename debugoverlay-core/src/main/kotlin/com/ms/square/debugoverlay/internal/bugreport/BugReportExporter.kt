package com.ms.square.debugoverlay.internal.bugreport

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.ms.square.debugoverlay.core.R
import com.ms.square.debugoverlay.internal.Logger
import com.ms.square.debugoverlay.internal.bugreport.model.BugReport
import com.ms.square.debugoverlay.internal.bugreport.model.BugReportArchiveImpl
import com.ms.square.debugoverlay.internal.util.runCatchingNonCancellation
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
 *
 * ## Conceptual Example: Jira Exporter
 *
 * This shows how an example Jira exporter could use [BugReport] to create
 * an issue with the bug report attached. The actual implementation may differ.
 *
 * ```kotlin
 * class JiraExporter(
 *   private val client: OkHttpClient,
 *   private val jiraBaseUrl: String,
 *   private val projectKey: String,
 *   private val issueType: String = "Bug"
 * ) : BugReportExporter {
 *
 *   override suspend fun export(report: BugReport): ExportResult {
 *     val summary = report.summary
 *     val archive = report.archive
 *
 *     // Step 1: Create issue with metadata from summary
 *     val issueBody = buildJsonObject {
 *       put("fields", buildJsonObject {
 *         put("project", buildJsonObject { put("key", projectKey) })
 *         put("summary", summary.title)
 *         put("description", buildString {
 *           summary.description?.let { append("$it\n\n") }
 *           append("---\n")
 *           append("App: ${summary.appInfo.packageName}")
 *           summary.appInfo.versionName?.let { append(" v$it") }
 *           summary.deviceInfo?.let { device ->
 *             append("\nDevice: ${device.manufacturer} ${device.model}")
 *             append("\nAndroid: ${device.androidVersion} (API ${device.apiLevel})")
 *           }
 *         })
 *         put("issuetype", buildJsonObject { put("name", issueType) })
 *       })
 *     }
 *
 *     val createRequest = Request.Builder()
 *       .url("$jiraBaseUrl/rest/api/2/issue")
 *       .post(issueBody.toString().toRequestBody("application/json".toMediaType()))
 *       .build()
 *
 *     val issueKey = client.newCall(createRequest).execute().use { response ->
 *       if (!response.isSuccessful) return ExportResult.Failure()
 *       // Parse issue key from response...
 *     }
 *
 *     // Step 2: Attach ZIP file to the created issue
 *     val attachRequest = Request.Builder()
 *       .url("$jiraBaseUrl/rest/api/2/issue/$issueKey/attachments")
 *       .header("X-Atlassian-Token", "no-check")
 *       .post(MultipartBody.Builder()
 *         .setType(MultipartBody.FORM)
 *         .addFormDataPart("file", archive.fileName, archive.toRequestBody())
 *         .build())
 *       .build()
 *
 *     return client.newCall(attachRequest).execute().use { response ->
 *       if (response.isSuccessful) ExportResult.Success else ExportResult.Failure()
 *     }
 *   }
 * }
 * ```
 */
internal interface BugReportExporter {
  /**
   * Export the bug report.
   *
   * Called on [kotlinx.coroutines.Dispatchers.IO]. Blocking I/O operations
   * (network uploads, file reads) are safe. For UI operations, use
   * `withContext(Dispatchers.Main)`.
   *
   * @param report The bug report containing archive and summary metadata
   * @return The result of the export operation
   */
  suspend fun export(report: BugReport): ExportResult
}

private const val PROVIDER_AUTHORITY_SUFFIX = ".debugoverlay.bugreport.provider"

/**
 * Default exporter that shares the bug report via Android's share sheet.
 * Uses Intent.ACTION_SEND with FileProvider for secure file sharing.
 */
internal class IntentShareExporter(private val context: Context) : BugReportExporter {

  override suspend fun export(report: BugReport): ExportResult {
    val file = when (val archive = report.archive) {
      is BugReportArchiveImpl -> archive.file
      // Future: If new archive implementations don't provide file access:
      // else -> return ExportResult.Failure(
      //   UnsupportedOperationException("Archive type does not provide file access")
      // )
    }
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

    return runCatchingNonCancellation {
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
