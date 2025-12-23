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
import com.ms.square.debugoverlay.internal.bugreport.FileNames.USER_INPUT
import com.ms.square.debugoverlay.internal.util.formatFilenameTimestamp
import com.ms.square.debugoverlay.internal.util.runCatchingNonCancellation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream

internal const val TEMP_FOLDER_PREFIX = "debugoverlay_capture_"
private const val CACHE_SUBDIR = "debugoverlay_bugreport_drafts"
private const val DEFAULT_MAX_DRAFTS = 10

/**
 * Represents a saved bug report draft.
 *
 * A folder is considered a draft when it contains a [FileNames.USER_INPUT] file,
 * which is saved when the user dismisses the metadata dialog.
 *
 * @param folderPath Absolute path to the draft folder (String for immutability)
 * @param lastModifiedMs Folder last modified timestamp, captured at construction
 * @param metadata User-provided title and description, null if parse failed or file missing
 * @param hasScreenshot Whether the draft has a screenshot file
 */
internal data class DraftInfo(
  val folderPath: String,
  val lastModifiedMs: Long,
  val metadata: BugReportMetadata?,
  val hasScreenshot: Boolean,
) {
  /** Convenience property to get the folder as a File. */
  val folder: File get() = File(folderPath)
}

/**
 * Handles temporary storage of bug report capture data and draft management.
 *
 * Saves captured diagnostic data to a temp folder, allowing it to be passed
 * between the DebugPanel/FAB and BugReportActivity via folder path.
 *
 * Draft management:
 * - A folder is a "draft" when it has [FileNames.USER_INPUT] (saved on dialog dismiss)
 * - Drafts are observable via [drafts] and [draftCount] StateFlows
 * - Maximum [DEFAULT_MAX_DRAFTS] drafts retained; oldest evicted automatically
 *
 * @param context Application context for cache directory access
 * @param scope Coroutine scope for background operations. Default lives for app lifetime.
 */
internal class BugReportTempStorage(
  context: Context,
  private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {

  private val folderMutex = Mutex()
  private val json = Json { ignoreUnknownKeys = true }

  private val cacheDir by lazy {
    File(context.cacheDir, CACHE_SUBDIR).apply { mkdirs() }
  }

  // Draft observability
  private val _drafts = MutableStateFlow<List<DraftInfo>>(emptyList())

  /** Observable list of saved drafts, sorted by most recent first. */
  val drafts: Flow<List<DraftInfo>> = _drafts.asStateFlow()

  /** Observable count of saved drafts. Emits only when count changes. */
  val draftCount: Flow<Int> = _drafts
    .map { it.size }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Lazily, 0)

  init {
    scope.launch {
      runCatchingNonCancellation { refreshDrafts() }
        .onFailure { Logger.e("Failed to refresh drafts on init", it) }
    }
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
    val screenshotFile = File(folder, SCREENSHOT)

    // Quick existence check with mutex (prevents reading during folder deletion)
    val exists = folderMutex.withLock { screenshotFile.exists() }
    if (!exists) return@withContext null

    // Slow bitmap decode outside mutex to avoid blocking other operations
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

  // ========== Draft Management ==========

  /**
   * Refreshes the draft list from disk.
   *
   * Scans the cache directory for folders with [FileNames.USER_INPUT] files,
   * which indicates a saved draft. Updates [drafts] StateFlow atomically.
   *
   * I/O-heavy operations (file existence checks, JSON parsing) are performed
   * outside the mutex to avoid blocking other operations.
   */
  suspend fun refreshDrafts(): Unit = withContext(Dispatchers.IO) {
    // Step 1: List folder names only (fast, mutex protects concurrent folder creation/deletion)
    val candidates = folderMutex.withLock {
      cacheDir.listFiles()
        ?.filter { it.isDirectory && it.name.startsWith(TEMP_FOLDER_PREFIX) }
        ?.toList() ?: emptyList()
    }

    // Step 2: Filter + load metadata (slow I/O, outside mutex)
    val draftList = candidates
      .filter { File(it, USER_INPUT).exists() } // Only folders with user_input.json are drafts
      .mapNotNull { folder ->
        runCatchingNonCancellation {
          DraftInfo(
            folderPath = folder.absolutePath,
            lastModifiedMs = folder.lastModified(),
            metadata = loadUserInputSync(folder),
            hasScreenshot = File(folder, SCREENSHOT).exists()
          )
        }.getOrNull() // Handles folder deleted mid-flight
      }
      .sortedByDescending { it.lastModifiedMs }

    // Step 3: Atomic update
    _drafts.value = draftList
    Logger.d("Refreshed drafts: ${draftList.size} found")
  }

  /**
   * Saves user metadata to a capture folder, marking it as a draft.
   *
   * After saving, evicts old drafts if over limit and refreshes the draft list.
   *
   * @param folder The capture folder
   * @param metadata The user-provided title and description
   */
  suspend fun saveUserInput(folder: File, metadata: BugReportMetadata): Unit = withContext(Dispatchers.IO) {
    val file = File(folder, USER_INPUT)
    runCatchingNonCancellation {
      file.writeText(json.encodeToString(metadata))
      Logger.d("Saved user input to: ${file.absolutePath}")
    }.onFailure {
      Logger.e("Failed to save user input", it)
      return@withContext // Don't evict/refresh if save failed
    }

    // Now that we've created a new draft, evict old ones and refresh list
    evictOldDrafts()
  }

  /**
   * Loads user metadata from a capture folder.
   *
   * @param folder The capture folder
   * @return The metadata, or null if file doesn't exist or parse fails
   */
  suspend fun loadUserInput(folder: File): BugReportMetadata? = withContext(Dispatchers.IO) {
    loadUserInputSync(folder)
  }

  /**
   * Evicts oldest drafts when count exceeds [maxDrafts].
   *
   * Only counts/deletes folders that have [FileNames.USER_INPUT] (actual drafts),
   * not in-progress captures. This is safe to call after saveSnapshot.
   *
   * @param maxDrafts Maximum number of drafts to retain (default: [DEFAULT_MAX_DRAFTS])
   */
  suspend fun evictOldDrafts(maxDrafts: Int = DEFAULT_MAX_DRAFTS): Unit = withContext(Dispatchers.IO) {
    // Hold mutex for entire operation to prevent race conditions
    folderMutex.withLock {
      val currentDrafts = cacheDir.listFiles()
        ?.filter { it.isDirectory && it.name.startsWith(TEMP_FOLDER_PREFIX) }
        ?.filter { File(it, USER_INPUT).exists() } // Only drafts, not in-progress
        ?.sortedByDescending { it.lastModified() }
        ?: emptyList()

      if (currentDrafts.size <= maxDrafts) return@withLock

      // Delete oldest drafts beyond the limit
      val toEvict = currentDrafts.drop(maxDrafts)
      toEvict.forEach { folder ->
        if (folder.exists()) {
          folder.deleteRecursively()
          Logger.d("Evicted old draft: ${folder.name}")
        }
      }
    }

    // Refresh outside mutex
    refreshDrafts()
  }

  /**
   * Synchronous helper to load user input JSON. Called from Dispatchers.IO.
   */
  private fun loadUserInputSync(folder: File): BugReportMetadata? {
    val file = File(folder, USER_INPUT)
    if (!file.exists()) return null
    return runCatching {
      json.decodeFromString(BugReportMetadata.serializer(), file.readText())
    }.getOrNull()
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
