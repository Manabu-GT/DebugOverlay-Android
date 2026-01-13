package com.ms.square.debugoverlay.internal.bugreport

import android.content.Context
import android.graphics.Bitmap
import com.ms.square.debugoverlay.internal.Logger
import com.ms.square.debugoverlay.internal.bugreport.model.BugReportResult
import com.ms.square.debugoverlay.internal.bugreport.model.BugReportSnapshot
import com.ms.square.debugoverlay.internal.bugreport.model.DraftInfo
import com.ms.square.debugoverlay.internal.bugreport.model.UserInput
import com.ms.square.debugoverlay.internal.data.DebugOverlayDataRepository
import com.ms.square.debugoverlay.internal.util.awaitCatching
import com.ms.square.debugoverlay.internal.util.captureUiHierarchy
import com.ms.square.debugoverlay.internal.util.runCatchingNonCancellation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
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
  private val storage: BugReportDraftStorage = DefaultBugReportDraftStorage(context),
  private val zipWriter: BugReportZipWriter = BugReportZipWriter(context),
) {

  /** Observable list of saved drafts. Used by draft picker UI. */
  val drafts: Flow<List<DraftInfo>> = storage.drafts

  /** Observable count of saved drafts. Used by FAB to show badge. */
  val draftCount: Flow<Int> = storage.draftCount

  /**
   * Captures all diagnostic data and saves to a temp folder.
   *
   * The folder path can be passed to BugReportActivity via Intent extra.
   * Screenshot bitmap is recycled after saving to disk.
   *
   * @return [Result.success] with the folder path, or [Result.failure] on error
   */
  suspend fun captureToFolder(): Result<File> {
    val timestampMs = System.currentTimeMillis()
    return runCatchingNonCancellation {
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
            // ScreenshotCapture.capture() handles errors internally, returns null on failure
            screenshot = screenshotDeferred.await(),
            logs = logsDeferred.awaitCatching().getOrDefault(emptyList()),
            networkRequests = networkRequestsDeferred.awaitCatching().getOrDefault(emptyList()),
            deviceInfo = deviceInfoDeferred.awaitCatching().getOrNull(),
            jankStats = jankStatsDeferred.awaitCatching().getOrNull(),
            appExitInfos = appExitInfosDeferred.awaitCatching().getOrDefault(emptyList()),
            // captureUiHierarchy() handles errors internally, returns null on failure
            uiHierarchy = uiHierarchyDeferred.await()
          )
        }
      }
      // Save to folder (bitmap is recycled inside saveSnapshot)
      storage.saveSnapshot(snapshot)
    }
  }

  /**
   * Loads screenshot from capture folder for preview in the metadata dialog.
   *
   * @param captureFolder Folder returned from [captureToFolder]
   * @param maxDimension Maximum dimension (width or height) for the loaded bitmap. Default is [DEFAULT_MAX_DIMENSION].
   * @return The screenshot bitmap, or null if not available
   */
  suspend fun loadScreenshotPreview(captureFolder: File, maxDimension: Int = DEFAULT_MAX_DIMENSION): Bitmap? =
    storage.loadScreenshot(captureFolder, maxDimension)

  /**
   * Creates a ZIP file from the captured data in the folder.
   *
   * @param captureFolder Folder returned from [captureToFolder]
   * @param userInput Optional user-provided title and description
   * @return [BugReportResult.Success] with the ZIP file, or [BugReportResult.Error] on failure
   */
  suspend fun createReportFromFolder(captureFolder: File, userInput: UserInput? = null): BugReportResult = try {
    val zipFile = zipWriter.writeFromFolder(captureFolder, userInput)
    BugReportResult.Success(zipFile)
  } catch (e: IOException) {
    Logger.e("Bug report write failed", e)
    BugReportResult.Error.IoError(e)
  }

  /**
   * Saves user input to a capture folder, marking it as a draft.
   *
   * Called when user dismisses the metadata dialog without submitting.
   * After saving, evicts old drafts if over limit.
   *
   * @param captureFolder Folder returned from [captureToFolder]
   * @param userInput User-provided title and description
   */
  suspend fun saveUserInputToDraft(captureFolder: File, userInput: UserInput) =
    storage.saveUserInput(captureFolder, userInput)

  /**
   * Deletes a capture folder and all its contents.
   * Call this after successful share or when user cancels.
   *
   * @param captureFolder Folder to delete
   */
  suspend fun deleteCaptureFolder(captureFolder: File) = storage.deleteFolder(captureFolder)
}
