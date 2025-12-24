package com.ms.square.debugoverlay.internal.bugreport.model

import android.graphics.Bitmap
import com.ms.square.debugoverlay.internal.data.model.AppExitInfo
import com.ms.square.debugoverlay.internal.data.model.DeviceInfo
import com.ms.square.debugoverlay.internal.data.model.JankStatsUiState
import com.ms.square.debugoverlay.model.LogEntry
import com.ms.square.debugoverlay.model.NetworkRequest

/**
 * Immutable snapshot of all diagnostic data captured at a specific moment.
 *
 * All collection properties are point-in-time snapshots from [DebugOverlayDataRepository].
 * Callers should treat these lists as read-only; modifying them has no effect on the
 * underlying repository data.
 *
 * @param timestampMs The time when the snapshot was captured (epoch millis)
 * @param screenshot Screenshot of the app (null if capture failed)
 * @param deviceInfo Device hardware, system, battery, and network information
 * @param logs Recent log entries
 * @param networkRequests Recent network requests
 * @param jankStats Frame rendering statistics
 * @param appExitInfos Recent app exit reasons
 * @param uiHierarchy UI hierarchy dump from Radiography
 */
@Suppress("LongParameterList")
internal class BugReportSnapshot(
  val timestampMs: Long,
  val screenshot: Bitmap?,
  val deviceInfo: DeviceInfo?,
  val logs: List<LogEntry>,
  val networkRequests: List<NetworkRequest>,
  val jankStats: JankStatsUiState?,
  val appExitInfos: List<AppExitInfo>,
  val uiHierarchy: String?,
) {

  /**
   * Converts this snapshot to [BugReportData] for writing.
   *
   * @param metadata User-provided title and description (null if skipped)
   * @return BugReportData ready for ZIP writing
   */
  fun toReportData(metadata: BugReportMetadata?): BugReportData = BugReportData(
    timestampMs = timestampMs,
    userMetadata = metadata,
    screenshot = screenshot,
    deviceInfo = deviceInfo,
    logs = logs,
    networkRequests = networkRequests,
    jankStats = jankStats,
    appExitInfos = appExitInfos,
    uiHierarchy = uiHierarchy
  )
}
