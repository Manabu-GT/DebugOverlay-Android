package com.ms.square.debugoverlay.internal.bugreport

import android.app.Application
import android.graphics.Bitmap
import com.ms.square.debugoverlay.internal.Logger
import com.ms.square.debugoverlay.internal.bugreport.FileNames.DEVICE_INFO
import com.ms.square.debugoverlay.internal.bugreport.FileNames.METADATA
import com.ms.square.debugoverlay.internal.bugreport.model.BugReport
import com.ms.square.debugoverlay.internal.bugreport.model.BugReportMetadata
import com.ms.square.debugoverlay.internal.bugreport.model.BugReportResult
import com.ms.square.debugoverlay.internal.bugreport.model.BugReportSnapshot
import com.ms.square.debugoverlay.internal.bugreport.model.BugReportSummary
import com.ms.square.debugoverlay.internal.bugreport.model.CustomLogSourceData
import com.ms.square.debugoverlay.internal.bugreport.model.DeviceInfoSummary
import com.ms.square.debugoverlay.internal.bugreport.model.DraftInfo
import com.ms.square.debugoverlay.internal.bugreport.model.UserInput
import com.ms.square.debugoverlay.internal.bugreport.model.toSummary
import com.ms.square.debugoverlay.internal.bugreport.model.validatedTitle
import com.ms.square.debugoverlay.internal.data.DebugOverlayDataRepository
import com.ms.square.debugoverlay.internal.data.model.DeviceInfo
import com.ms.square.debugoverlay.internal.util.awaitCatching
import com.ms.square.debugoverlay.internal.util.captureUiHierarchy
import com.ms.square.debugoverlay.internal.util.runCatchingNonCancellation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
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
  private val context: Application,
  private val repository: DebugOverlayDataRepository,
  private val activityProvider: ActivityProvider,
  private val appInfoProvider: AppInfoProvider = DefaultAppInfoProvider,
  private val storage: BugReportDraftStorage = DefaultBugReportDraftStorage(context),
  private val zipWriter: BugReportZipWriter = BugReportZipWriter(context),
) {

  /** Observable list of saved drafts. Used by draft picker UI. */
  val drafts: Flow<List<DraftInfo>> = storage.drafts

  /** Observable count of saved drafts. Used by FAB to show badge. */
  val draftCount: Flow<Int> = storage.draftCount

  private val json = Json { ignoreUnknownKeys = true }

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
        // Capture app info synchronously (fast, cached by system)
        val appInfo = appInfoProvider.getAppInfo(context)

        supervisorScope {
          val screenshotDeferred = async {
            activityProvider.activity?.let { ScreenshotCapture.capture(it) }
          }
          val logcatLogsDeferred = async { repository.queryLogcatSnapshot() }
          // Check if custom log source is registered to determine if custom logs should be included
          val hasCustomLogSource = repository.hasCustomLogSource.value
          val customLogSourceDataDeferred = if (hasCustomLogSource) {
            async {
              val logs = repository.customLogSourceLogs.first()
              val sourceName = repository.customLogSourceName.first()
              // sourceName should never be null when hasCustomLogSource is true
              sourceName?.let { CustomLogSourceData(logs, it) }
            }
          } else {
            null
          }
          val networkRequestsDeferred = async { repository.networkRequests.first() }
          val deviceInfoDeferred = async { repository.queryDeviceInfoSnapshot() }
          val jankStatsDeferred = async { repository.jankStats.first() }
          val appExitInfosDeferred = async { repository.queryAppExitInfosSnapshot() }
          val uiHierarchyDeferred = async { captureUiHierarchy() }

          BugReportSnapshot(
            timestampMs = timestampMs,
            appInfo = appInfo,
            // ScreenshotCapture.capture() handles errors internally, returns null on failure
            screenshot = screenshotDeferred.await(),
            deviceInfo = deviceInfoDeferred.awaitCatching().getOrNull(),
            logcatLogs = logcatLogsDeferred.awaitCatching().getOrDefault(emptyList()),
            customLogSourceData = customLogSourceDataDeferred?.awaitCatching()?.getOrNull(),
            networkRequests = networkRequestsDeferred.awaitCatching().getOrDefault(emptyList()),
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
   * Creates a bug report from the captured data in the folder.
   *
   * Constructs a [BugReport] containing both the ZIP archive and summary metadata
   * for export integrations.
   *
   * @param captureFolder Folder returned from [captureToFolder]
   * @param defaultTitle Default title to use if userInput.title is blank (should be localized)
   * @param userInput Optional user-provided title and description
   * @return [BugReportResult.Success] with the bug report, or [BugReportResult.Error] on failure
   */
  suspend fun createReportFromFolder(
    captureFolder: File,
    defaultTitle: String,
    userInput: UserInput? = null,
  ): BugReportResult = withContext(Dispatchers.IO) {
    try {
      // Load metadata from folder
      val metadata = loadMetadata(captureFolder)
        ?: return@withContext BugReportResult.Error.IoError(
          IOException("metadata.json not found or corrupt in ${captureFolder.name}")
        )

      // Create ZIP file
      val zipFile = zipWriter.writeFromFolder(captureFolder, userInput)

      // Build summary for exporters
      val summary = BugReportSummary(
        title = userInput.validatedTitle(defaultTitle),
        description = userInput?.description,
        appInfo = metadata.appInfo.toSummary(),
        deviceInfo = loadDeviceInfoSummary(captureFolder),
        capturedAt = metadata.capturedAt
      )

      val report = BugReport.fromFile(zipFile, summary)
      BugReportResult.Success(report)
    } catch (e: IOException) {
      Logger.e("Bug report write failed", e)
      BugReportResult.Error.IoError(e)
    }
  }

  private fun loadMetadata(folder: File): BugReportMetadata? {
    val file = File(folder, METADATA)
    if (!file.exists()) return null
    return runCatching {
      json.decodeFromString(BugReportMetadata.serializer(), file.readText())
    }.getOrNull()
  }

  /**
   * Loads device info summary from the folder's device_info.json.
   * Returns null if the file doesn't exist or can't be parsed.
   */
  private fun loadDeviceInfoSummary(folder: File): DeviceInfoSummary? {
    val file = File(folder, DEVICE_INFO)
    if (!file.exists()) {
      Logger.w("device_info.json not found in ${folder.name}")
      return null
    }

    return runCatching {
      json.decodeFromString<DeviceInfo>(file.readText()).toSummary()
    }.getOrElse { e ->
      Logger.w("Failed to parse device_info.json: ${e.message}")
      null
    }
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
