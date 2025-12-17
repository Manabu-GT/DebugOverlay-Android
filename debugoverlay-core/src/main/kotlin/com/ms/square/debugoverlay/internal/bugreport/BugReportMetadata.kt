package com.ms.square.debugoverlay.internal.bugreport

import kotlinx.serialization.Serializable

/**
 * User-provided metadata for a bug report.
 *
 * @param title Short summary of the issue (required, must not be blank)
 * @param description Detailed description of the issue (optional)
 */
@Serializable
internal data class BugReportMetadata(val title: String, val description: String = "") {
  init {
    require(title.isNotBlank()) { "Bug report title cannot be blank" }
  }
}
