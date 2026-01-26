package com.ms.square.debugoverlay.internal.bugreport.model

/**
 * Result type for bug report generation operations.
 *
 * Note: Concurrent generation should be prevented at the UI layer by disabling
 * the bug report button while generation is in progress.
 */
internal sealed class BugReportResult {
  /**
   * Bug report was generated successfully.
   * @param report The generated bug report containing archive and summary
   */
  data class Success(val report: BugReport) : BugReportResult()

  /**
   * Bug report generation failed.
   */
  sealed class Error : BugReportResult() {
    /**
     * I/O error during report generation (file creation, ZIP writing, etc.)
     * @param cause The underlying exception
     */
    data class IoError(val cause: Throwable) : Error()
  }
}
