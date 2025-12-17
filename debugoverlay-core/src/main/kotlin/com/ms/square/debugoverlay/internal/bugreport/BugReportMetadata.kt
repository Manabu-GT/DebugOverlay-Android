package com.ms.square.debugoverlay.internal.bugreport

import kotlinx.serialization.Serializable

/**
 * User-provided metadata for a bug report.
 *
 * @param title Short summary of the issue (required)
 * @param description Detailed description of the issue (optional)
 */
@Serializable
internal data class BugReportMetadata(val title: String, val description: String = "")
