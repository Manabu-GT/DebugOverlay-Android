package com.ms.square.debugoverlay.internal.bugreport.model

import com.ms.square.debugoverlay.internal.data.model.DeviceInfo

/**
 * Summary of a finalized bug report for export integrations.
 *
 * Contains the essential metadata needed for creating issues in external
 * systems (Jira, GitHub, Slack, etc.) without parsing the ZIP file.
 *
 * Named "Summary" to differentiate from [BugReportMetadata] which is used
 * for internal JSON serialization.
 */
internal data class BugReportSummary(
  /** Resolved title (defaults to "Bug Report" if user didn't provide one). */
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
 * The full [AppInfo] (with 10+ fields) is available in the ZIP's metadata.json.
 */
internal data class AppInfoSummary(
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
 * The full [DeviceInfo]
 * (with nested HardwareInfo, SystemInfo, etc.) is available in the ZIP.
 */
internal data class DeviceInfoSummary(
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

/**
 * Creates an [AppInfoSummary] from the full [AppInfo].
 */
internal fun AppInfo.toSummary() = AppInfoSummary(
  packageName = packageName,
  versionName = versionName,
  versionCode = versionCode,
  isDebuggable = isDebuggable
)

/**
 * Creates a [DeviceInfoSummary] from the full [DeviceInfo].
 */
internal fun DeviceInfo.toSummary() = DeviceInfoSummary(
  manufacturer = hardware.manufacturer,
  model = hardware.model,
  androidVersion = system.androidVersion,
  apiLevel = system.apiLevel,
  locale = system.locale
)
