package com.ms.square.debugoverlay.internal.data.model

import android.app.ApplicationExitInfo

/**
 * Data class representing an app exit event from [ApplicationExitInfo].
 */
internal data class AppExitInfo(
  val id: Long,
  val reason: AppExitReason,
  val timestampMs: Long,
  val description: String?,
  val processName: String,
  val pssKb: Long,
  val rssKb: Long,
  val importance: ProcessImportance,
  val trace: String?,
)

/**
 * App exit reason with severity classification.
 *
 * Values match [android.app.ApplicationExitInfo] REASON_* constants.
 *
 * @see <a href="https://developer.android.com/reference/android/app/ApplicationExitInfo">ApplicationExitInfo</a>
 */
internal enum class AppExitReason(val value: Int, val label: String, val severity: Severity, val explanation: String) {
  ANR(
    value = 6,
    label = "ANR",
    severity = Severity.CRITICAL,
    explanation = "App was unresponsive. Check for main thread blocking " +
      "(network calls, heavy computation, or deadlocks)."
  ),
  CRASH(
    value = 4,
    label = "Crash",
    severity = Severity.CRITICAL,
    explanation = "Unhandled Java/Kotlin exception. Check the stack trace for the root cause."
  ),
  CRASH_NATIVE(
    value = 5,
    label = "Native Crash",
    severity = Severity.CRITICAL,
    explanation = "Native code (C/C++) crashed. Check native libraries or NDK code for memory issues."
  ),
  DEPENDENCY_DIED(
    value = 12,
    label = "Dependency Died",
    severity = Severity.INFO,
    explanation = "A process the app depended on (e.g., content provider) was terminated."
  ),
  EXCESSIVE_RESOURCE(
    value = 9,
    label = "Excessive Resources",
    severity = Severity.WARNING,
    explanation = "System killed the app for using too much CPU or memory. Check for resource-intensive operations."
  ),
  EXIT_SELF(
    value = 1,
    label = "Exit Self",
    severity = Severity.INFO,
    explanation = "App terminated itself via System.exit(). This is usually intentional."
  ),
  FREEZER(
    value = 14,
    label = "Freezer",
    severity = Severity.WARNING,
    explanation = "App was frozen but received a sync binder call. Check for unexpected background activity."
  ),
  INITIALIZATION_FAILURE(
    value = 7,
    label = "Init Failure",
    severity = Severity.CRITICAL,
    explanation = "App failed to initialize during startup. Check Application.onCreate() or content providers."
  ),
  LOW_MEMORY(
    value = 3,
    label = "Low Memory",
    severity = Severity.WARNING,
    explanation = "System killed the app to reclaim memory. " +
      "Consider reducing memory footprint or handling onTrimMemory()."
  ),
  OTHER(
    value = 13,
    label = "Other",
    severity = Severity.INFO,
    explanation = "System terminated the app for miscellaneous reasons."
  ),
  PACKAGE_STATE_CHANGE(
    value = 15,
    label = "Package State Change",
    severity = Severity.INFO,
    explanation = "App was killed due to being disabled or a component state change. (API 34+)"
  ),
  PACKAGE_UPDATED(
    value = 16,
    label = "Package Updated",
    severity = Severity.INFO,
    explanation = "App was killed because it was updated. (API 34+)"
  ),
  PERMISSION_CHANGE(
    value = 8,
    label = "Permission Change",
    severity = Severity.INFO,
    explanation = "User modified runtime permissions, causing the app to restart."
  ),
  SIGNALED(
    value = 2,
    label = "Signal",
    severity = Severity.INFO,
    explanation = "Process was killed by an OS signal (e.g., SIGKILL). Usually external or system-initiated."
  ),
  USER_REQUESTED(
    value = 10,
    label = "User Requested",
    severity = Severity.INFO,
    explanation = "User stopped the app via 'Force stop' in Settings or by swiping from Recents."
  ),
  USER_STOPPED(
    value = 11,
    label = "User Stopped",
    severity = Severity.INFO,
    explanation = "The user profile running this app was stopped. " +
      "Occurs on multi-user devices when switching users or stopping a work profile."
  ),
  UNKNOWN(
    value = 0,
    label = "Unknown",
    severity = Severity.INFO,
    explanation = "Exit reason could not be determined."
  ),
  ;

  enum class Severity { CRITICAL, WARNING, INFO }

  companion object {
    fun fromValue(value: Int): AppExitReason = entries.find { it.value == value } ?: UNKNOWN
  }
}

/**
 * Process importance level at the time of exit.
 *
 * Values match [android.app.ActivityManager.RunningAppProcessInfo] IMPORTANCE_* constants.
 * Note: Android may introduce new importance values in future versions; unrecognized values
 * will be mapped to [UNKNOWN].
 *
 * @see android.app.ActivityManager.RunningAppProcessInfo
 */
@Suppress("MagicNumber") // Values are Android API constants
internal enum class ProcessImportance(val value: Int, val label: String) {
  FOREGROUND(100, "Foreground"),
  FOREGROUND_SERVICE(125, "Foreground Service"),
  VISIBLE(200, "Visible"),
  PERCEPTIBLE(230, "Perceptible"),
  SERVICE(300, "Service"),
  TOP_SLEEPING(325, "Top Sleeping"),
  CANT_SAVE_STATE(350, "Can't Save State"),
  CACHED(400, "Cached"),
  GONE(1000, "Gone"),
  UNKNOWN(-1, "Unknown"),
  ;

  companion object {
    fun fromValue(value: Int): ProcessImportance = entries.find { it.value == value } ?: UNKNOWN
  }
}
