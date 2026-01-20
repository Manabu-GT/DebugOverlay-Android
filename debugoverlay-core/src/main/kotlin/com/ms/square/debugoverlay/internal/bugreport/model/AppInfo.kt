package com.ms.square.debugoverlay.internal.bugreport.model

import kotlinx.serialization.Serializable

/**
 * Information about the host application for bug reports.
 *
 * Automatically captured and included in metadata.json to provide
 * essential context for debugging without manual setup.
 */
@Serializable
internal data class AppInfo(
  /** Application package name (e.g., "com.example.myapp"). */
  val packageName: String,

  /** User-facing version string (e.g., "1.2.3"), null if not set. */
  val versionName: String?,

  /** Numeric version code for programmatic comparison. */
  val versionCode: Long,

  /** Target SDK version the app is built against. */
  val targetSdkVersion: Int,

  /** Minimum SDK version required to run the app. */
  val minSdkVersion: Int,

  /** Whether the app is built with debuggable flag enabled. */
  val isDebuggable: Boolean,

  /** Human-readable store name (e.g., "Google Play Store", "Unknown", "Sideloaded"). */
  val installerStore: String,

  /** Raw installer package name (e.g., "com.android.vending"), null if sideloaded. */
  val installerPackage: String?,

  /** Timestamp when the app was first installed (milliseconds since epoch). */
  val firstInstallTime: Long,

  /** Timestamp when the app was last updated (milliseconds since epoch). */
  val lastUpdateTime: Long,
)
