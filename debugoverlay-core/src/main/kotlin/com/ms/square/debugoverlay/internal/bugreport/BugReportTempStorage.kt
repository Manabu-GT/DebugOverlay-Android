package com.ms.square.debugoverlay.internal.bugreport

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.ms.square.debugoverlay.internal.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

internal const val TEMP_FOLDER_PREFIX = "debugoverlay_capture_"
internal const val SCREENSHOT_FILENAME = "screenshot.png"
private const val HTML_REPORT_FILENAME = "bug_report.html"

/**
 * Handles temporary storage of bug report capture data.
 *
 * Saves captured diagnostic data to a temp folder, allowing it to be passed
 * between the FAB (in WindowManager overlay) and BugReportActivity via folder path.
 *
 * TODO: Future draft management improvements:
 *  - Store timestamp in metadata.json instead of folder name (more robust)
 *  - Use UUID for folder names
 *  - Add maxDimension parameter to loadScreenshot for memory-efficient thumbnails
 *  - Add Mutex around deleteFolder to prevent race conditions
 *  - Add cleanupOldDrafts(maxDrafts: Int) for draft eviction
 *  - Add cleanupStaleCaptures() to delete folders older than 24h on app start
 */
internal class BugReportTempStorage(context: Context) {

  private val cacheDir = context.cacheDir

  /**
   * Saves a snapshot to a temp folder.
   *
   * The screenshot bitmap in [snapshot] will be recycled after saving and must not be used afterward.
   *
   * @param snapshot The captured diagnostic data
   * @return Result containing the folder path, or a failure with exception details
   */
  @Suppress("TooGenericExceptionCaught", "LongMethod")
  suspend fun saveSnapshot(snapshot: BugReportSnapshot): Result<File> = withContext(Dispatchers.IO) {
    runCatching {
      val folder = createTempFolder(snapshot.timestampMs)

      try {
        // Save screenshot first (most important for preview)
        snapshot.screenshot?.let { bitmap ->
          runCatching { saveScreenshot(bitmap, File(folder, SCREENSHOT_FILENAME)) }
            .onFailure { Logger.e("Failed to save screenshot", it) }
        }

        // Generate and save HTML report (needs bitmap before it's recycled)
        // User metadata is not available at capture time, will be added to ZIP separately
        runCatching {
          val reportData = snapshot.toReportData(metadata = null)
          HtmlReportBuilder.build(reportData, File(folder, HTML_REPORT_FILENAME))
        }.onFailure { Logger.e("Failed to save HTML report", it) }

        // Save diagnostic data files (wrap each to allow partial saves)
        runCatching { BugReportFileWriters.writeLogs(snapshot.logs, File(folder, "logs.json")) }
          .onFailure { Logger.e("Failed to save logs", it) }

        runCatching {
          BugReportFileWriters.writeNetworkRequests(snapshot.networkRequests, File(folder, "network_requests.json"))
        }.onFailure { Logger.e("Failed to save network requests", it) }

        snapshot.deviceInfo?.let {
          runCatching { BugReportFileWriters.writeDeviceInfo(it, File(folder, "device_info.json")) }
            .onFailure { Logger.e("Failed to save device info", it) }
        }

        snapshot.jankStats?.let {
          runCatching { BugReportFileWriters.writeJankStats(it, File(folder, "jank_stats.json")) }
            .onFailure { Logger.e("Failed to save jank stats", it) }
        }

        runCatching { BugReportFileWriters.writeAppExits(snapshot.appExitInfos, File(folder, "app_exits.txt")) }
          .onFailure { Logger.e("Failed to save app exits", it) }

        snapshot.uiHierarchy?.let {
          runCatching { BugReportFileWriters.writeUiHierarchy(it, File(folder, "ui_hierarchy.txt")) }
            .onFailure { Logger.e("Failed to save UI hierarchy", it) }
        }
      } finally {
        // Recycle bitmap after all saves complete—Activity will reload from disk
        snapshot.screenshot?.recycle()
      }

      Logger.d("Bug report snapshot saved to: ${folder.absolutePath}")
      folder
    }
  }

  /**
   * Loads the screenshot from a capture folder for preview.
   *
   * @param folder The capture folder
   * @return The screenshot bitmap, or null if not available or loading fails
   */
  @Suppress("TooGenericExceptionCaught")
  suspend fun loadScreenshot(folder: File): Bitmap? = withContext(Dispatchers.IO) {
    val screenshotFile = File(folder, SCREENSHOT_FILENAME)
    if (!screenshotFile.exists()) return@withContext null

    try {
      // TODO: Add maxDimension parameter with BitmapFactory.Options for memory-efficient thumbnails
      BitmapFactory.decodeFile(screenshotFile.absolutePath)
    } catch (e: OutOfMemoryError) {
      Logger.e("OOM while loading screenshot", e)
      null
    } catch (e: Exception) {
      Logger.e("Failed to load screenshot", e)
      null
    }
  }

  /**
   * Extracts the capture timestamp from folder name.
   *
   * @param folder The capture folder
   * @return The timestamp in millis, or current time if parsing fails
   */
  // TODO: Read from metadata.json instead of parsing folder name (more robust for draft management)
  fun getTimestamp(folder: File): Long {
    return folder.name.removePrefix(TEMP_FOLDER_PREFIX).toLongOrNull()
      ?: System.currentTimeMillis()
  }

  /**
   * Deletes a capture folder and all its contents.
   *
   * @param folder The capture folder to delete
   */
  // TODO: Add Mutex to prevent race conditions when multiple deletions happen concurrently
  suspend fun deleteFolder(folder: File): Unit = withContext(Dispatchers.IO) {
    if (folder.exists() && folder.name.startsWith(TEMP_FOLDER_PREFIX)) {
      val deleted = folder.deleteRecursively()
      if (deleted) {
        Logger.d("Deleted capture folder: ${folder.absolutePath}")
      } else {
        Logger.w("Failed to delete capture folder: ${folder.absolutePath}")
      }
    }
  }

  private fun createTempFolder(timestampMs: Long): File {
    val folder = File(cacheDir, "$TEMP_FOLDER_PREFIX$timestampMs")
    check(folder.mkdirs() || folder.exists()) {
      "Failed to create temp folder: ${folder.absolutePath}"
    }
    return folder
  }

  private fun saveScreenshot(bitmap: Bitmap, file: File) {
    FileOutputStream(file).use { out ->
      bitmap.compress(Bitmap.CompressFormat.PNG, UNUSED_PNG_QUALITY, out)
    }
  }
}
