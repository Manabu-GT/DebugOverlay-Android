package com.ms.square.debugoverlay.internal.bugreport

import android.content.Context
import android.graphics.Bitmap
import com.ms.square.debugoverlay.internal.Logger
import com.ms.square.debugoverlay.internal.data.DebugOverlayDataRepository
import com.ms.square.debugoverlay.internal.util.captureUiHierarchy
import com.ms.square.debugoverlay.internal.util.runCatchingNonCancellation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * Orchestrates bug report generation by collecting diagnostic data,
 * capturing screenshots, and packaging everything into a ZIP file.
 *
 * Uses a folder-based flow (unified for both FAB and Debug Panel):
 * 1. [captureToFolder] - Captures all data and saves to temp folder
 * 2. [loadScreenshotPreview] - Loads screenshot for dialog preview
 * 3. [createReportFromFolder] - Creates ZIP after user provides metadata
 * 4. [deleteCaptureFolder] - Cleans up temp folder
 */
internal class BugReportGenerator(
  context: Context,
  private val repository: DebugOverlayDataRepository,
  private val activityProvider: ActivityProvider,
  private val tempStorage: BugReportTempStorage = BugReportTempStorage(context),
  private val zipWriter: BugReportZipWriter = BugReportZipWriter(context),
) {

  /**
   * Captures all diagnostic data and saves to a temp folder.
   *
   * The folder path can be passed to [BugReportActivity] via Intent extra.
   * Screenshot bitmap is recycled after saving to disk.
   *
   * @return [Result.success] with the folder path, or [Result.failure] on error
   */
  @Suppress("TooGenericExceptionCaught")
  suspend fun captureToFolder(): Result<File> {
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
      // Save to folder (bitmap is recycled inside saveSnapshot)
      tempStorage.saveSnapshot(snapshot)
    } catch (e: CancellationException) {
      throw e // Preserve structured concurrency
    } catch (e: Exception) {
      Logger.e("Capture to folder failed", e)
      Result.failure(e)
    }
  }

  /**
   * Loads screenshot from capture folder for preview in the metadata dialog.
   *
   * @param captureFolder Folder returned from [captureToFolder]
   * @return The screenshot bitmap, or null if not available
   */
  suspend fun loadScreenshotPreview(captureFolder: File): Bitmap? = tempStorage.loadScreenshot(captureFolder)

  /**
   * Creates a ZIP file from the captured data in the folder.
   *
   * @param captureFolder Folder returned from [captureToFolder]
   * @param metadata Optional user-provided title and description
   * @return [BugReportResult.Success] with the ZIP file, or [BugReportResult.Error] on failure
   */
  suspend fun createReportFromFolder(captureFolder: File, metadata: BugReportMetadata? = null): BugReportResult = try {
    val zipFile = withContext(Dispatchers.IO) {
      zipWriter.writeFromFolder(captureFolder, metadata)
    }
    BugReportResult.Success(zipFile)
  } catch (e: IOException) {
    Logger.e("Bug report write failed", e)
    BugReportResult.Error.IoError(e)
  }

  /**
   * Deletes a capture folder and all its contents.
   * Call this after successful share or when user cancels.
   *
   * @param captureFolder Folder to delete
   */
  suspend fun deleteCaptureFolder(captureFolder: File) = tempStorage.deleteFolder(captureFolder)
}
