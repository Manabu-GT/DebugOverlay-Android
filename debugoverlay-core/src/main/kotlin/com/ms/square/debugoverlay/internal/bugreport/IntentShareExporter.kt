package com.ms.square.debugoverlay.internal.bugreport

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.ms.square.debugoverlay.BugReportExporter
import com.ms.square.debugoverlay.core.R
import com.ms.square.debugoverlay.formatBugReportMarkdown
import com.ms.square.debugoverlay.internal.Logger
import com.ms.square.debugoverlay.internal.bugreport.model.BugReportArchiveImpl
import com.ms.square.debugoverlay.internal.util.runCatchingNonCancellation
import com.ms.square.debugoverlay.model.BugReport
import com.ms.square.debugoverlay.model.ExportResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val PROVIDER_AUTHORITY_SUFFIX = ".debugoverlay.bugreport.provider"

/**
 * Default exporter that shares the bug report via Android's share sheet.
 * Uses Intent.ACTION_SEND with FileProvider for secure file sharing.
 */
internal object IntentShareExporter : BugReportExporter {

  override suspend fun export(context: Context, report: BugReport): ExportResult {
    val file = when (val archive = report.archive) {
      is BugReportArchiveImpl -> archive.file
      else -> return ExportResult.Failure(
        UnsupportedOperationException("IntentShareExporter requires file-backed archive")
      )
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
      putExtra(Intent.EXTRA_TEXT, formatBugReportMarkdown(report.summary))
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
