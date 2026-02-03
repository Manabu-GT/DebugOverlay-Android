package com.ms.square.debugoverlay.model

/**
 * A complete bug report ready for export.
 *
 * Combines the ZIP archive with its summary metadata, providing exporters
 * with everything needed to create issues in external systems without
 * having to parse the ZIP file.
 *
 * @see com.ms.square.debugoverlay.BugReportExporter
 */
public data class BugReport(
  /** The bug report ZIP archive. */
  val archive: BugReportArchive,
  /** Summary metadata for issue creation. */
  val summary: BugReportSummary,
)
