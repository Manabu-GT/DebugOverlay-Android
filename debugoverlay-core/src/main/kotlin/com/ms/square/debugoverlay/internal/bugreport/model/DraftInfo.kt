package com.ms.square.debugoverlay.internal.bugreport.model

import java.io.File

/**
 * Represents a saved bug report draft.
 *
 * A folder is included when its metadata.json has [BugReportState.DRAFT] or
 * [BugReportState.SUBMITTED] state (see [BugReportState.isRetainedDraft]).
 *
 * @param folderPath Absolute path to the draft folder (String for immutability)
 * @param metadata Bug report metadata containing timestamps, state, and user input
 * @param hasScreenshot Whether the draft has a screenshot file
 */
internal data class DraftInfo(val folderPath: String, val metadata: BugReportMetadata, val hasScreenshot: Boolean) {
  /** Convenience property to get the folder as a File. */
  val folder: File get() = File(folderPath)

  /** Timestamp when this bug was captured (milliseconds since epoch). */
  val capturedAt: Long get() = metadata.capturedAt

  /** Whether this draft has been submitted (exported) at least once. */
  val isSubmitted: Boolean get() = metadata.state == BugReportState.SUBMITTED
}
