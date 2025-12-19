package com.ms.square.debugoverlay.internal.bugreport

import com.ms.square.debugoverlay.internal.Logger
import com.ms.square.debugoverlay.internal.data.DebugOverlayDataRepository
import com.ms.square.debugoverlay.internal.util.captureUiHierarchy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Orchestrates bug report generation by collecting diagnostic data,
 * capturing screenshots, and packaging everything into a ZIP file.
 *
 * Uses a capture-first flow:
 * 1. [captureSnapshot] - Captures all diagnostic data immediately
 * 2. [writeReport] - Writes ZIP from snapshot after user provides metadata
 */
internal class BugReportGenerator(
  private val zipWriter: BugReportZipWriter,
  private val repository: DebugOverlayDataRepository,
  private val activityProvider: ActivityProvider,
) {

  /**
   * Captures a snapshot of all diagnostic data at the current moment.
   *
   * Use this for "capture-first" flows where the data should be captured
   * immediately on button tap, before showing a metadata dialog.
   *
   * The screenshot bitmap will be garbage collected when the snapshot is no
   * longer referenced. Simply set the reference to null when done.
   *
   * @return [Result.success] with the snapshot, or [Result.failure] on error
   */
  @Suppress("TooGenericExceptionCaught")
  suspend fun captureSnapshot(): Result<BugReportSnapshot> {
    val timestampMs = System.currentTimeMillis()

    return try {
      val snapshot = withContext(Dispatchers.Default) {
        supervisorScope {
          val screenshotDeferred = async {
            activityProvider.activity?.let { ScreenshotCapture.capture(it) }
          }
          val logsDeferred = async { repository.logs.first() }
          val networkRequestsDeferred = async { repository.networkRequests.first() }
          val deviceInfoDeferred = async { repository.queryDeviceInfoSnapshot() }
          val jankStatsDeferred = async { repository.jankStats.first() }
          val appExitInfosDeferred = async { repository.queryAppExitInfosSnapshot() }
          val uiHierarchyDeferred = async { captureUiHierarchy() }

          BugReportSnapshot(
            timestampMs = timestampMs,
            screenshot = runCatchingNonCancellation { screenshotDeferred.await() }.getOrNull(),
            logs = runCatchingNonCancellation { logsDeferred.await() }.getOrElse { emptyList() },
            networkRequests = runCatchingNonCancellation { networkRequestsDeferred.await() }.getOrElse { emptyList() },
            deviceInfo = runCatchingNonCancellation { deviceInfoDeferred.await() }.getOrNull(),
            jankStats = runCatchingNonCancellation { jankStatsDeferred.await() }.getOrNull(),
            appExitInfos = runCatchingNonCancellation { appExitInfosDeferred.await() }.getOrDefault(emptyList()),
            uiHierarchy = runCatchingNonCancellation { uiHierarchyDeferred.await() }.getOrNull()
          )
        }
      }
      Result.success(snapshot)
    } catch (e: CancellationException) {
      throw e // Preserve structured concurrency
    } catch (e: Exception) {
      Logger.e("Snapshot capture failed", e)
      Result.failure(e)
    }
  }

  /**
   * Writes a bug report ZIP from a previously captured snapshot.
   *
   * @param snapshot The captured diagnostic data
   * @param metadata Optional user-provided title and description
   * @return [BugReportResult.Success] with the ZIP file, or [BugReportResult.Error] on failure
   */
  suspend fun writeReport(snapshot: BugReportSnapshot, metadata: BugReportMetadata? = null): BugReportResult = try {
    val data = snapshot.toReportData(metadata)
    val zipFile = withContext(Dispatchers.IO) {
      zipWriter.write(data)
    }
    BugReportResult.Success(zipFile)
  } catch (e: IOException) {
    Logger.e("Bug report write failed", e)
    BugReportResult.Error.IoError(e)
  }
}

/**
 * Runs [block] catching all exceptions except [CancellationException].
 * This preserves structured concurrency while allowing graceful degradation.
 */
@Suppress("TooGenericExceptionCaught")
private inline fun <T> runCatchingNonCancellation(block: () -> T): Result<T> = try {
  Result.success(block())
} catch (e: CancellationException) {
  throw e
} catch (e: Exception) {
  Result.failure(e)
}
