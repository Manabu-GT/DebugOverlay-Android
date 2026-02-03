package com.ms.square.debugoverlay.model

/**
 * Summary of a finalized bug report for export integrations.
 *
 * Contains the essential metadata needed for creating issues in external
 * systems (Jira, GitHub, Slack, etc.) without parsing the ZIP file.
 */
public data class BugReportSummary(
  /** Resolved title (defaults to "Bug Report" if user didn't provide one). Never empty or blank. */
  val title: String,
  /** User-provided description (null if not provided). */
  val description: String?,
  /** Essential app information. */
  val appInfo: AppInfoSummary,
  /** Essential device information (null if unavailable). */
  val deviceInfo: DeviceInfoSummary?,
  /** Timestamp when the bug report was captured (milliseconds since epoch, UTC). */
  val capturedAt: Long,
)

/**
 * Simplified app info for export integrations.
 *
 * Contains only the essential fields needed for issue creation.
 * The full app info (with 10+ fields) is available in the ZIP's metadata.json.
 */
public data class AppInfoSummary(
  /** Application package name (e.g., "com.example.myapp"). */
  val packageName: String,
  /** User-facing version string (e.g., "1.2.3"), null if not set. */
  val versionName: String?,
  /** Numeric version code for programmatic comparison. */
  val versionCode: Long,
  /** Whether the app is built with debuggable flag enabled. */
  val isDebuggable: Boolean,
)

/**
 * Simplified device info for export integrations.
 *
 * Contains only the essential fields needed for issue creation.
 * The full device info (with nested HardwareInfo, SystemInfo, etc.) is available in the ZIP.
 */
public data class DeviceInfoSummary(
  /** Device manufacturer (e.g., "Google", "Samsung"). */
  val manufacturer: String,
  /** Device model (e.g., "Pixel 8 Pro", "SM-S918B"). */
  val model: String,
  /** Android version (e.g., "14", "13"). */
  val androidVersion: String,
  /** Android API level (e.g., 34, 33). */
  val apiLevel: Int,
  /** Device locale (e.g., "en_US", "ja_JP"). */
  val locale: String,
)
