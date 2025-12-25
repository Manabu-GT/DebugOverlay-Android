package com.ms.square.debugoverlay.internal.bugreport.model

import kotlinx.serialization.Serializable

/**
 * State of a bug report capture.
 */
@Serializable
internal enum class BugReportState {
  /** Capture happened, metadata.json created, but user hasn't dismissed the dialog yet. */
  IN_PROGRESS,

  /** User saved as draft (dismissed dialog with content). */
  DRAFT,

  /** User submitted and ZIP was created. */
  SUBMITTED,
}

/**
 * User-provided input for a bug report.
 *
 * @param title Short summary of the issue (may be blank)
 * @param description Detailed description of the issue (optional)
 */
@Serializable
internal data class UserInput(val title: String = "", val description: String = "")

/**
 * Returns a validated title, falling back to [defaultTitle] if blank or null.
 *
 * All consumers that display or use the title should use this extension
 * to ensure consistent default title handling.
 *
 * @param defaultTitle The fallback title (typically from R.string.debugoverlay_bug_report_default_title)
 * @return The title if not blank, otherwise [defaultTitle]
 */
internal fun UserInput?.validatedTitle(defaultTitle: String): String =
  this?.title?.takeIf { it.isNotBlank() } ?: defaultTitle

/**
 * Metadata for a bug report stored in metadata.json.
 *
 * This file is created immediately when a snapshot is captured, allowing us to
 * store timestamps and state.
 *
 * @param version Schema version for forward compatibility
 * @param capturedAt Timestamp when the bug report was captured (milliseconds since epoch)
 * @param state Current state of the bug report
 * @param userInput User-provided title and description, null until user saves as draft
 */
@Serializable
internal data class BugReportMetadata(
  val version: Int = 1,
  val capturedAt: Long,
  val state: BugReportState = BugReportState.IN_PROGRESS,
  val userInput: UserInput? = null,
)
