package com.ms.square.debugoverlay.internal.bugreport.model

import java.io.File

/**
 * A complete bug report ready for export.
 *
 * Combines the ZIP archive with its summary metadata, providing exporters
 * with everything needed to create issues in external systems without
 * having to parse the ZIP file.
 *
 * Currently internal - will be made public when the exporter API is finalized.
 */
internal data class BugReport(
  /** The bug report ZIP archive. */
  val archive: BugReportArchive,
  /** Summary metadata for issue creation. */
  val summary: BugReportSummary,
) {
  companion object {
    /**
     * Creates a [BugReport] from a ZIP file and summary data.
     */
    fun fromFile(zipFile: File, summary: BugReportSummary): BugReport = BugReport(
      archive = BugReportArchiveImpl(zipFile),
      summary = summary
    )
  }
}
