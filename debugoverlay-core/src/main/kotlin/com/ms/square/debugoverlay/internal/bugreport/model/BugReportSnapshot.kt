package com.ms.square.debugoverlay.internal.bugreport.model

import android.graphics.Bitmap
import com.ms.square.debugoverlay.internal.data.model.AppExitInfo
import com.ms.square.debugoverlay.internal.data.model.DeviceInfo
import com.ms.square.debugoverlay.internal.data.model.JankStatsUiState
import com.ms.square.debugoverlay.model.LogEntry
import com.ms.square.debugoverlay.model.NetworkRequest

/**
 * Bundled custom log source data.
 * Ensures logs and source name are always provided together.
 */
internal data class CustomLogSourceData(val logs: List<LogEntry>, val sourceName: String)

/**
 * Immutable snapshot of all diagnostic data captured at a specific moment.
 *
 * All collection properties are point-in-time snapshots from [DebugOverlayDataRepository].
 * Callers should treat these lists as read-only; modifying them has no effect on the
 * underlying repository data.
 *
 * @param capturedAt The time when the snapshot was captured (epoch millis, UTC)
 * @param appInfo Information about the host application
 * @param screenshot Screenshot of the app (null if capture failed)
 * @param deviceInfo Device hardware, system, battery, and network information
 * @param logcatLogs Recent logcat entries (always present)
 * @param customLogSourceData Custom log source data with source name (null if no log source registered)
 * @param networkRequests Recent network requests
 * @param jankStats Frame rendering statistics
 * @param appExitInfos Recent app exit reasons
 * @param uiHierarchy UI hierarchy dump from Radiography
 */
@Suppress("LongParameterList")
internal class BugReportSnapshot(
  val capturedAt: Long,
  val appInfo: AppInfo,
  val screenshot: Bitmap?,
  val deviceInfo: DeviceInfo?,
  val logcatLogs: List<LogEntry>,
  val customLogSourceData: CustomLogSourceData?,
  val networkRequests: List<NetworkRequest>,
  val jankStats: JankStatsUiState?,
  val appExitInfos: List<AppExitInfo>,
  val uiHierarchy: String?,
) {

  /**
   * Converts this snapshot to [BugReportData] for writing.
   *
   * @param userInput User-provided title and description (null if skipped)
   * @return BugReportData ready for ZIP writing
   */
  fun toReportData(userInput: UserInput?): BugReportData = BugReportData(
    capturedAt = capturedAt,
    userInput = userInput,
    appInfo = appInfo,
    screenshot = screenshot,
    deviceInfo = deviceInfo,
    logcatLogs = logcatLogs,
    customLogSourceData = customLogSourceData,
    networkRequests = networkRequests,
    jankStats = jankStats,
    appExitInfos = appExitInfos,
    uiHierarchy = uiHierarchy
  )
}
