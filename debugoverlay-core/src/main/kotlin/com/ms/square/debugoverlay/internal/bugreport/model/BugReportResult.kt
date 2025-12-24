package com.ms.square.debugoverlay.internal.bugreport.model

import java.io.File

/**
 * Result type for bug report generation operations.
 *
 * Note: Concurrent generation should be prevented at the UI layer by disabling
 * the bug report button while generation is in progress.
 */
internal sealed class BugReportResult {
  /**
   * Bug report was generated successfully.
   * @param zipFile The generated ZIP file containing the bug report
   */
  data class Success(val zipFile: File) : BugReportResult()

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
