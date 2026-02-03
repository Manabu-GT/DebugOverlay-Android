package com.ms.square.debugoverlay.internal.bugreport.model

import com.ms.square.debugoverlay.model.BugReportArchive
import java.io.File
import java.io.InputStream

/**
 * Internal implementation of [BugReportArchive] that wraps a [File].
 *
 * Exposes [file] for internal use (e.g., `IntentShareExporter` needs it for FileProvider).
 */
internal class BugReportArchiveImpl(internal val file: File) : BugReportArchive {

  override val fileName: String = file.name

  override val sizeBytes: Long = file.length()

  override fun openInputStream(): InputStream = file.inputStream()
}
