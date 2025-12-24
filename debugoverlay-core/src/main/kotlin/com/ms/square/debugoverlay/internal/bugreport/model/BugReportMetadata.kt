package com.ms.square.debugoverlay.internal.bugreport.model

import kotlinx.serialization.Serializable

/**
 * User-provided metadata for a bug report.
 *
 * Title may be blank for drafts; use [validatedTitle] to get a display-safe title
 * with a default fallback.
 *
 * @param title Short summary of the issue (may be blank for drafts)
 * @param description Detailed description of the issue (optional)
 */
@Serializable
internal data class BugReportMetadata(val title: String, val description: String = "")

/**
 * Returns a validated title, falling back to [defaultTitle] if blank or null.
 *
 * All consumers that display or use the title should use this extension
 * to ensure consistent default title handling.
 *
 * @param defaultTitle The fallback title (typically from R.string.debugoverlay_bug_report_default_title)
 * @return The title if not blank, otherwise [defaultTitle]
 */
internal fun BugReportMetadata?.validatedTitle(defaultTitle: String): String =
  this?.title?.takeIf { it.isNotBlank() } ?: defaultTitle
