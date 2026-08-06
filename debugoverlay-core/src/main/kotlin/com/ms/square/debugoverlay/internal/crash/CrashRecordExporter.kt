package com.ms.square.debugoverlay.internal.crash

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.ms.square.debugoverlay.core.R
import com.ms.square.debugoverlay.internal.Logger
import com.ms.square.debugoverlay.internal.util.checkFolderExists
import com.ms.square.debugoverlay.internal.util.formatFilenameTimestamp
import com.ms.square.debugoverlay.internal.util.runCatchingNonCancellation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val PROVIDER_AUTHORITY_SUFFIX = ".debugoverlay.bugreport.provider"
private const val EXPORTS_SUBDIR = "debugoverlay_crash_exports"

/**
 * Shares a [CrashRecord] as a plain-text file via Android's share sheet.
 *
 * Writes to a temp file under [Context.getCacheDir] (rather than putting the text
 * directly in [Intent.EXTRA_TEXT]) to avoid binder IPC size limits when logs are large,
 * and reuses the same FileProvider authority already declared for bug report sharing.
 */
internal object CrashRecordExporter {

  suspend fun share(context: Context, record: CrashRecord): Boolean = withContext(Dispatchers.IO) {
    runCatchingNonCancellation {
      val exportsDir = File(context.cacheDir, EXPORTS_SUBDIR).also { it.checkFolderExists() }
      val file = File(exportsDir, "crash_${formatFilenameTimestamp(record.timestampMs)}.txt")
      file.writeText(formatCrashRecordAsText(record))

      val authority = "${context.packageName}$PROVIDER_AUTHORITY_SUFFIX"
      val uri = FileProvider.getUriForFile(context, authority, file)

      val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, record.exceptionType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      val chooserTitle = context.getString(R.string.debugoverlay_share_crash_log)
      withContext(Dispatchers.Main) {
        context.startActivity(Intent.createChooser(intent, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
      }
      true
    }.getOrElse { e ->
      Logger.w("Failed to share crash record", e)
      false
    }
  }
}
