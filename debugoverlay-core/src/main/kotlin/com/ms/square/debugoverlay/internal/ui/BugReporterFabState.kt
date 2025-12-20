package com.ms.square.debugoverlay.internal.ui

/**
 * State machine for the Bug Reporter FAB.
 *
 * Visual feedback is driven by state:
 * - [Idle]: Normal appearance, ready for tap
 * - [Processing]: Spinner overlay while capturing screenshot
 * - [Error]: Red tint, indicates capture failure
 *
 * Note: Success state is not needed because the FAB transitions to Idle immediately
 * after launching BugReportActivity. The activity handles the rest of the flow.
 */
internal sealed interface BugReporterFabState {
  data object Idle : BugReporterFabState
  data object Processing : BugReporterFabState
  data class Error(val message: String) : BugReporterFabState
}
