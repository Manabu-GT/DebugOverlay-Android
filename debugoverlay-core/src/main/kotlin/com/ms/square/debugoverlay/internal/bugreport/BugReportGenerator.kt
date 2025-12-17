package com.ms.square.debugoverlay.internal.bugreport

import android.graphics.Bitmap
import com.ms.square.debugoverlay.internal.Logger
import com.ms.square.debugoverlay.internal.data.DebugOverlayDataRepository
import com.ms.square.debugoverlay.internal.util.captureUiHierarchy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Orchestrates bug report generation by collecting diagnostic data,
 * capturing screenshots, and packaging everything into a ZIP file.
 */
internal class BugReportGenerator(
  private val zipWriter: BugReportZipWriter,
  private val repository: DebugOverlayDataRepository,
  private val activityProvider: ActivityProvider,
) {

  /**
   * Generates a bug report containing all available diagnostic data.
   *
   * Note: The UI should disable the trigger button during generation to prevent
   * concurrent calls. This method does not enforce single-generation internally.
   *
   * @param metadata Optional user-provided title and description
   * @return [BugReportResult.Success] with the ZIP file, or [BugReportResult.Error] on failure
   */
  suspend fun generate(metadata: BugReportMetadata? = null): BugReportResult {
    val timestampMs = System.currentTimeMillis()
    var screenshot: Bitmap? = null

    return try {
      // Collect data in parallel on Default dispatcher (CPU-bound work)
      // This ensures data is captured close to button tap time and handles both
      // warm (instant) and cold (waiting for first emission) flows efficiently
      val data = withContext(Dispatchers.Default) {
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

          // Await with individual error handling - partial failures don't abort report
          screenshot = runCatching { screenshotDeferred.await() }.getOrNull()

          BugReportData(
            timestampMs = timestampMs,
            userMetadata = metadata,
            screenshot = screenshot,
            logs = runCatching { logsDeferred.await() }.getOrElse { emptyList() },
            networkRequests = runCatching { networkRequestsDeferred.await() }.getOrElse { emptyList() },
            deviceInfo = runCatching { deviceInfoDeferred.await() }.getOrNull(),
            jankStats = runCatching { jankStatsDeferred.await() }.getOrNull(),
            appExitInfos = runCatching { appExitInfosDeferred.await() }.getOrDefault(emptyList()),
            uiHierarchy = runCatching { uiHierarchyDeferred.await() }.getOrNull()
          )
        }
      }

      // Write ZIP on IO dispatcher (blocking file I/O)
      val zipFile = withContext(Dispatchers.IO) {
        zipWriter.write(data)
      }

      BugReportResult.Success(zipFile)
    } catch (e: IOException) {
      Logger.e("Bug report generation failed", e)
      BugReportResult.Error.IoError(e)
    } finally {
      // Always recycle bitmap regardless of success/failure
      screenshot?.recycle()
    }
  }
}
