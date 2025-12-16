package com.ms.square.debugoverlay.internal.bugreport

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
 * @param screenshot Screenshot of the app at the time of report generation (may be null if capture failed)
 * @param deviceInfo Device hardware, system, battery, and network information
 * @param logs Recent log entries
 * @param networkRequests Recent network requests (if available)
 * @param jankStats Frame rendering statistics from JankStats
 * @param appExitInfos Recent app exit reasons (API 30+)
 * @param uiHierarchy UI hierarchy dump from Radiography
 */
internal data class BugReportData(
  val timestampMs: Long,
  val screenshot: Bitmap?,
  val deviceInfo: DeviceInfo?,
  val logs: List<LogEntry>,
  val networkRequests: List<NetworkRequest>,
  val jankStats: JankStatsUiState?,
  val appExitInfos: List<AppExitInfo>,
  val uiHierarchy: String?,
)
