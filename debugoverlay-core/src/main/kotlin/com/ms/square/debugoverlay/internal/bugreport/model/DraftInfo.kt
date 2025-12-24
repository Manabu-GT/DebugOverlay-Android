package com.ms.square.debugoverlay.internal.bugreport.model

import com.ms.square.debugoverlay.internal.bugreport.FileNames
import java.io.File

/**
 * Represents a saved bug report draft.
 *
 * A folder is considered a draft when it contains a [FileNames.USER_INPUT] file,
 * which is saved when the user dismisses the metadata dialog.
 *
 * @param folderPath Absolute path to the draft folder (String for immutability)
 * @param lastModifiedMs Folder last modified timestamp, captured at construction
 * @param metadata User-provided title and description, null if parse failed or file missing
 * @param hasScreenshot Whether the draft has a screenshot file
 */
internal data class DraftInfo(
  val folderPath: String,
  val lastModifiedMs: Long,
  val metadata: BugReportMetadata?,
  val hasScreenshot: Boolean,
) {
  /** Convenience property to get the folder as a File. */
  val folder: File get() = File(folderPath)
}
