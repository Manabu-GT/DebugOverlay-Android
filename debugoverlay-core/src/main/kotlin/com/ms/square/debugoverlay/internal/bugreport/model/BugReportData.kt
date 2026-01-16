package com.ms.square.debugoverlay.internal.bugreport.model

import android.graphics.Bitmap
import com.ms.square.debugoverlay.internal.data.model.AppExitInfo
import com.ms.square.debugoverlay.internal.data.model.DeviceInfo
import com.ms.square.debugoverlay.internal.data.model.JankStatsUiState
import com.ms.square.debugoverlay.model.LogEntry
import com.ms.square.debugoverlay.model.NetworkRequest

/**
 * Data class containing all diagnostic information for a bug report.
 *
 * @param timestampMs The time when the bug report was generated (epoch millis)
 * @param userInput User-provided title and description (null if skipped)
 * @param screenshot Screenshot of the app at the time of report generation (may be null if capture failed)
 * @param deviceInfo Device hardware, system, battery, and network information
 * @param logcatLogs Recent logcat entries (always present)
 * @param customLogSourceData Custom log source data with source name (null if no log source registered)
 * @param networkRequests Recent network requests (if available)
 * @param jankStats Frame rendering statistics from JankStats
 * @param appExitInfos Recent app exit reasons (API 30+)
 * @param uiHierarchy UI hierarchy dump from Radiography
 */
internal data class BugReportData(
  val timestampMs: Long,
  val userInput: UserInput?,
  val screenshot: Bitmap?,
  val deviceInfo: DeviceInfo?,
  val logcatLogs: List<LogEntry>,
  val customLogSourceData: CustomLogSourceData?,
  val networkRequests: List<NetworkRequest>,
  val jankStats: JankStatsUiState?,
  val appExitInfos: List<AppExitInfo>,
  val uiHierarchy: String?,
)
