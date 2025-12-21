package com.ms.square.debugoverlay.internal.bugreport

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.ms.square.debugoverlay.internal.Logger
import com.ms.square.debugoverlay.internal.bugreport.FileNames.APP_EXITS
import com.ms.square.debugoverlay.internal.bugreport.FileNames.DEVICE_INFO
import com.ms.square.debugoverlay.internal.bugreport.FileNames.HTML_REPORT
import com.ms.square.debugoverlay.internal.bugreport.FileNames.JANK_STATS
import com.ms.square.debugoverlay.internal.bugreport.FileNames.LOGS
import com.ms.square.debugoverlay.internal.bugreport.FileNames.NETWORK_REQUESTS
import com.ms.square.debugoverlay.internal.bugreport.FileNames.SCREENSHOT
import com.ms.square.debugoverlay.internal.bugreport.FileNames.UI_HIERARCHY
import com.ms.square.debugoverlay.internal.util.formatFilenameTimestamp
import com.ms.square.debugoverlay.internal.util.runCatchingNonCancellation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

internal const val TEMP_FOLDER_PREFIX = "debugoverlay_capture_"
private const val CACHE_SUBDIR = "debugoverlay_bugreport_drafts"

/**
 * Handles temporary storage of bug report capture data.
 *
 * Saves captured diagnostic data to a temp folder, allowing it to be passed
 * between the DebugPanel/FAB and BugReportActivity via folder path.
 *
 * TODO: Future draft management improvements:
 *  - Store timestamp data and list of bug report files in metadata.json (more robust)
 *  - Use UUID for folder names?
 *  - Add maxDimension parameter to loadScreenshot for memory-efficient thumbnails
 *  - Add Mutex around deleteFolder to prevent race conditions
 *  - Add cleanupOldDrafts(maxDrafts: Int) for draft eviction - should be 10 to start with.
 */
internal class BugReportTempStorage(context: Context) {

  private val folderMutex = Mutex()

  private val cacheDir by lazy {
    File(context.cacheDir, CACHE_SUBDIR).apply { mkdirs() }
  }

  /**
   * Saves a snapshot to a temp folder.
   *
   * The screenshot bitmap in [snapshot] will be recycled after saving and must not be used afterward.
   *
   * @param snapshot The captured diagnostic data
   * @return Result containing the folder path, or a failure with exception details
   */
  @Suppress("LongMethod")
  suspend fun saveSnapshot(snapshot: BugReportSnapshot): Result<File> = withContext(Dispatchers.IO) {
    runCatchingNonCancellation {
      val folder = createTempFolder(snapshot.timestampMs)

      try {
        // Save screenshot first (most important for preview)
        snapshot.screenshot?.let { bitmap ->
          runCatchingNonCancellation { saveScreenshot(bitmap, File(folder, SCREENSHOT)) }
            .onFailure { Logger.e("Failed to save screenshot", it) }
        }

        // Generate and save HTML report (needs bitmap before it's recycled)
        // User metadata is not available at capture time, will be added to ZIP separately
        runCatchingNonCancellation {
          val reportData = snapshot.toReportData(metadata = null)
          HtmlReportBuilder.build(reportData, File(folder, HTML_REPORT))
        }.onFailure { Logger.e("Failed to save HTML report", it) }

        // Save diagnostic data files (wrap each to allow partial saves)
        runCatchingNonCancellation { BugReportFileWriters.writeLogs(snapshot.logs, File(folder, LOGS)) }
          .onFailure { Logger.e("Failed to save logs", it) }

        runCatchingNonCancellation {
          BugReportFileWriters.writeNetworkRequests(snapshot.networkRequests, File(folder, NETWORK_REQUESTS))
        }.onFailure { Logger.e("Failed to save network requests", it) }

        snapshot.deviceInfo?.let { deviceInfo ->
          runCatchingNonCancellation { BugReportFileWriters.writeDeviceInfo(deviceInfo, File(folder, DEVICE_INFO)) }
            .onFailure { Logger.e("Failed to save device info", it) }
        }

        snapshot.jankStats?.let { jankStats ->
          runCatchingNonCancellation { BugReportFileWriters.writeJankStats(jankStats, File(folder, JANK_STATS)) }
            .onFailure { Logger.e("Failed to save jank stats", it) }
        }

        runCatchingNonCancellation {
          BugReportFileWriters.writeAppExits(snapshot.appExitInfos, File(folder, APP_EXITS))
        }
          .onFailure { Logger.e("Failed to save app exits", it) }

        snapshot.uiHierarchy?.let { uiHierarchy ->
          runCatchingNonCancellation { BugReportFileWriters.writeUiHierarchy(uiHierarchy, File(folder, UI_HIERARCHY)) }
            .onFailure { Logger.e("Failed to save UI hierarchy", it) }
        }
      } finally {
        // Recycle bitmap after all saves complete—Activity will reload from disk
        // NOTE: Since the normal GC process will free up this memory when there are
        // no more references to this bitmap, this isn't strictly necessary.
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
    folderMutex.withLock {
      val screenshotFile = File(folder, SCREENSHOT)
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
  }

  /**
   * Deletes a capture folder and all its contents.
   *
   * @param folder The capture folder to delete
   */
  suspend fun deleteFolder(folder: File): Unit = withContext(Dispatchers.IO) {
    folderMutex.withLock {
      if (folder.exists() && folder.name.startsWith(TEMP_FOLDER_PREFIX)) {
        val deleted = folder.deleteRecursively()
        if (deleted) {
          Logger.d("Deleted capture folder: ${folder.absolutePath}")
        } else {
          Logger.w("Failed to delete capture folder: ${folder.absolutePath}")
        }
      }
    }
  }

  private fun createTempFolder(timestampMs: Long): File {
    val folder = File(cacheDir, "$TEMP_FOLDER_PREFIX${formatFilenameTimestamp(timestampMs)}")
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
