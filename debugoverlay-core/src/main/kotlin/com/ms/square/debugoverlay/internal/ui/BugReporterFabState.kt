package com.ms.square.debugoverlay.internal.ui

/**
 * State machine for the Bug Reporter FAB.
 *
 * Visual feedback is driven by state:
 * - [Idle]: Normal appearance, ready for tap
 * - [Processing]: Spinner overlay, capturing screenshot and generating report
 * - [Success]: Green tint, scale animation
 * - [Error]: Red tint, indicates failure
 */
internal sealed interface BugReporterFabState {
  data object Idle : BugReporterFabState
  data object Processing : BugReporterFabState
  data object Success : BugReporterFabState
  data class Error(val message: String) : BugReporterFabState
}
