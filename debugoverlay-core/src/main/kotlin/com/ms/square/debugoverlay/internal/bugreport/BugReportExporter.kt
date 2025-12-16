package com.ms.square.debugoverlay.internal.bugreport

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.ms.square.debugoverlay.core.R
import com.ms.square.debugoverlay.internal.Logger
import java.io.File

/**
 * Interface for exporting bug reports.
 *
 * Currently internal, expected to be made public in the future to allow
 * custom integrations (Jira, GitHub Issues, Slack, etc.).
 *
 * **Future Public API:**
 * ```kotlin
 * DebugOverlay.configure {
 *   copy(bugReportExporters = listOf(
 *     JiraExporter(projectKey = "MYAPP"),
 *     GitHubIssueExporter(repo = "user/repo"),
 *     IntentShareExporter(context)
 *   ))
 * }
 * ```
 */
internal interface BugReportExporter {
  /**
   * Export the bug report.
   * @param zipFile The generated ZIP file containing report data
   * @return true if export succeeded, false otherwise
   */
  suspend fun export(zipFile: File): Boolean
}

private const val PROVIDER_AUTHORITY_SUFFIX = ".debugoverlay.bugreport.provider"

/**
 * Default exporter that shares the bug report via Android's share sheet.
 * Uses Intent.ACTION_SEND with FileProvider for secure file sharing.
 */
internal class IntentShareExporter(private val context: Context) : BugReportExporter {

  override suspend fun export(zipFile: File): Boolean {
    val authority = "${context.packageName}$PROVIDER_AUTHORITY_SUFFIX"
    val uri = FileProvider.getUriForFile(context, authority, zipFile)

    val subject = context.getString(R.string.debugoverlay_bug_report_subject, zipFile.nameWithoutExtension)
    val chooserTitle = context.getString(R.string.debugoverlay_share_bug_report)

    val intent = Intent(Intent.ACTION_SEND).apply {
      type = "application/zip"
      clipData = ClipData.newRawUri(null, uri)
      putExtra(Intent.EXTRA_STREAM, uri)
      putExtra(Intent.EXTRA_SUBJECT, subject)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    return runCatching {
      context.startActivity(
        Intent.createChooser(intent, chooserTitle).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
      )
      true
    }.getOrElse { e ->
      Logger.w("Failed to share bug report", e)
      false
    }
  }
}
