package com.ms.square.debugoverlay.internal.bugreport

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.ms.square.debugoverlay.DebugOverlay
import com.ms.square.debugoverlay.internal.Logger
import com.ms.square.debugoverlay.internal.bugreport.FileNames.APP_EXITS
import com.ms.square.debugoverlay.internal.bugreport.FileNames.DEVICE_INFO
import com.ms.square.debugoverlay.internal.bugreport.FileNames.HTML_REPORT
import com.ms.square.debugoverlay.internal.bugreport.FileNames.JANK_STATS
import com.ms.square.debugoverlay.internal.bugreport.FileNames.LOGCAT_LOGS
import com.ms.square.debugoverlay.internal.bugreport.FileNames.METADATA
import com.ms.square.debugoverlay.internal.bugreport.FileNames.NETWORK_REQUESTS
import com.ms.square.debugoverlay.internal.bugreport.FileNames.SCREENSHOT
import com.ms.square.debugoverlay.internal.bugreport.FileNames.UI_HIERARCHY
import com.ms.square.debugoverlay.internal.bugreport.model.BugReportMetadata
import com.ms.square.debugoverlay.internal.bugreport.model.BugReportSnapshot
import com.ms.square.debugoverlay.internal.bugreport.model.BugReportState
import com.ms.square.debugoverlay.internal.bugreport.model.DraftInfo
import com.ms.square.debugoverlay.internal.bugreport.model.UserInput
import com.ms.square.debugoverlay.internal.util.checkFolderExists
import com.ms.square.debugoverlay.internal.util.isDirectChildOf
import com.ms.square.debugoverlay.internal.util.runCatchingNonCancellation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

internal const val DEFAULT_MAX_DIMENSION = 1920

internal sealed interface BugReportDraftStorage {
  val drafts: Flow<List<DraftInfo>>
  val draftCount: Flow<Int>

  suspend fun saveSnapshot(snapshot: BugReportSnapshot): File
  suspend fun saveUserInput(folder: File, userInput: UserInput)
  suspend fun markAsSubmitted(folder: File)
  suspend fun loadScreenshot(folder: File, maxDimension: Int = DEFAULT_MAX_DIMENSION): Bitmap?
  suspend fun deleteFolder(folder: File)
}

private const val DRAFTS_SUBDIR = "debugoverlay_bugreport_drafts"
private const val DEFAULT_MAX_DRAFTS = 10

/**
 * Default implementation of [BugReportDraftStorage] using the app's no-backup data directory.
 *
 * Saves captured diagnostic data to a folder in the app's internal storage, allowing it to be passed
 * between the DebugPanel/FAB and BugReportActivity via folder path.
 *
 * Draft lifecycle:
 * - On capture, a folder with [FileNames.METADATA] is created with [BugReportState.IN_PROGRESS]
 * - A folder becomes a "draft" when its state changes to [BugReportState.DRAFT] (on dialog dismiss)
 * - Drafts are observable via [drafts] and [draftCount] flows
 * - Maximum [DEFAULT_MAX_DRAFTS] drafts retained; oldest evicted automatically
 * - Folders are stored in the app's no-backup directory for reliable persistence without cloud sync
 *
 * @param context Application context for no-backup directory access
 */
@Suppress("TooManyFunctions")
internal class DefaultBugReportDraftStorage(
  private val context: Context,
  private val appInfoProvider: AppInfoProvider = DefaultAppInfoProvider,
) : BugReportDraftStorage {

  private val draftsInitialized = AtomicBoolean(false)

  private val folderMutex = Mutex()
  private val json = Json { ignoreUnknownKeys = true }

  private val draftsDir by lazy {
    File(context.noBackupFilesDir, DRAFTS_SUBDIR).also {
      it.checkFolderExists()
    }
  }

  // Draft observability
  private val _drafts = MutableStateFlow<List<DraftInfo>>(emptyList())

  /** Observable list of saved drafts, sorted by most recent first. */
  override val drafts: Flow<List<DraftInfo>> = _drafts.asStateFlow()
    .onStart {
      initDraftsIfNeeded()
    }

  /** Observable count of saved drafts. Emits only when count changes. */
  override val draftCount: Flow<Int> = _drafts
    .map { it.size }
    .distinctUntilChanged()
    .onStart {
      initDraftsIfNeeded()
    }

  private suspend fun initDraftsIfNeeded() {
    if (draftsInitialized.compareAndSet(false, true)) {
      runCatchingNonCancellation { refreshDrafts() }
        .onFailure { Logger.e("Failed to refresh drafts on init", it) }
    }
  }

  /**
   * Saves a snapshot to a folder using best-effort persistence.
   *
   * Individual file writes may fail without failing the overall operation.
   * The screenshot bitmap in [snapshot] will be recycled after saving and must not be used afterward.
   *
   * Creates a [FileNames.METADATA] file with [BugReportState.IN_PROGRESS] state.
   *
   * @param snapshot The captured diagnostic data
   * @return File containing the folder path
   */
  override suspend fun saveSnapshot(snapshot: BugReportSnapshot): File = withContext(Dispatchers.IO) {
    val folder = createFolder()

    try {
      // Save metadata first (required for draft detection)
      val metadata = BugReportMetadata(
        capturedAt = snapshot.capturedAt,
        state = BugReportState.IN_PROGRESS,
        appInfo = snapshot.appInfo
      )
      saveBestEffort("metadata") { saveMetadata(folder, metadata) }

      // Save screenshot (most important for preview)
      snapshot.screenshot?.let { bitmap ->
        saveBestEffort("screenshot") { saveScreenshot(bitmap, File(folder, SCREENSHOT)) }
      }

      // Generate and save HTML report (needs bitmap before it's recycled)
      // User input is not available at capture time, will be added to ZIP separately
      saveBestEffort("HTML report") {
        val reportData = snapshot.toReportData(userInput = null)
        HtmlReportBuilder.build(reportData, File(folder, HTML_REPORT))
      }

      saveDiagnosticFiles(snapshot, folder)
    } finally {
      // Recycle bitmap after all saves complete—Activity will reload from disk
      // NOTE: Since the normal GC process will free up this memory when there are
      // no more references to this bitmap, this isn't strictly necessary.
      snapshot.screenshot?.recycle()
    }

    Logger.d("Bug report snapshot saved to: ${folder.absolutePath}")
    folder
  }

  private suspend fun saveDiagnosticFiles(snapshot: BugReportSnapshot, folder: File) {
    saveBestEffort("logcat logs") {
      BugReportFileWriters.writeLogcatLogs(snapshot.logcatLogs, File(folder, LOGCAT_LOGS))
    }
    snapshot.customLogSourceData?.let { customData ->
      saveBestEffort("custom logs") {
        val filename = FileNames.customLogSourceFilename(customData.sourceName)
        BugReportFileWriters.writeCustomLogs(customData.logs, customData.sourceName, File(folder, filename))
      }
    }
    saveBestEffort("network requests") {
      BugReportFileWriters.writeNetworkRequests(snapshot.networkRequests, File(folder, NETWORK_REQUESTS))
    }
    snapshot.deviceInfo?.let { deviceInfo ->
      saveBestEffort("device info") {
        BugReportFileWriters.writeDeviceInfo(deviceInfo, File(folder, DEVICE_INFO))
      }
    }
    snapshot.jankStats?.let { jankStats ->
      saveBestEffort("jank stats") {
        BugReportFileWriters.writeJankStats(jankStats, File(folder, JANK_STATS))
      }
    }
    saveBestEffort("app exits") {
      BugReportFileWriters.writeAppExits(snapshot.appExitInfos, File(folder, APP_EXITS))
    }
    snapshot.uiHierarchy?.let { uiHierarchy ->
      saveBestEffort("UI hierarchy") {
        BugReportFileWriters.writeUiHierarchy(uiHierarchy, File(folder, UI_HIERARCHY))
      }
    }
    val contributors = DebugOverlay.bugReportContributors
    if (contributors.isNotEmpty()) {
      saveBestEffort("custom data") {
        collectCustomData(contributors, folder)
      }
    }
  }

  /**
   * Loads the screenshot from a capture folder for preview.
   *
   * No mutex is needed here because:
   * - BitmapFactory.decodeFile() handles missing/deleted files gracefully (returns null)
   * - The try-catch covers any exceptions from concurrent deletion
   * - Holding mutex during decode (100-500ms) would block other operations unnecessarily
   *
   * @param folder The capture folder
   * @param maxDimension The max dimension to restrict the resulting bitmap size for memory efficiency.
   * @return The screenshot bitmap, or null if not available or loading fails
   */
  override suspend fun loadScreenshot(folder: File, maxDimension: Int): Bitmap? = withContext(Dispatchers.IO) {
    val screenshotFile = File(folder, SCREENSHOT)
    if (!screenshotFile.exists()) return@withContext null

    runCatchingNonCancellation {
      // First decode bounds only
      val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
      }
      BitmapFactory.decodeFile(screenshotFile.absolutePath, options)

      // If bounds decode failed, file doesn't exist or is invalid
      if (options.outWidth <= 0 || options.outHeight <= 0) return@withContext null

      // Calculate sample size to fit within maxDimension
      options.inSampleSize = calculateInSampleSize(options, maxDimension, maxDimension)
      options.inJustDecodeBounds = false

      // Decode with sampling
      BitmapFactory.decodeFile(screenshotFile.absolutePath, options)
    }.getOrElse { e ->
      Logger.e("Failed to load screenshot", e)
      null
    }
  }

  private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val (height, width) = options.outHeight to options.outWidth
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {
      val halfHeight = height / 2
      val halfWidth = width / 2
      while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
        inSampleSize *= 2
      }
    }
    return inSampleSize
  }

  /**
   * Deletes a capture folder and all its contents.
   *
   * Safety: Only deletes folders that are direct children of [draftsDir] to prevent
   * accidental deletion of arbitrary paths.
   *
   * @param folder The capture folder to delete
   */
  override suspend fun deleteFolder(folder: File): Unit = withContext(Dispatchers.IO) {
    val deleted = folderMutex.withLock {
      // Safety check: only delete folders that are direct children of our drafts directory
      if (!folder.isDirectChildOf(draftsDir)) {
        Logger.w("Refusing to delete folder outside drafts directory: ${folder.absolutePath}")
        return@withLock false
      }

      if (folder.exists()) {
        val result = folder.deleteRecursively()
        if (result) {
          Logger.d("Deleted capture folder: ${folder.absolutePath}")
        } else {
          Logger.w("Failed to delete capture folder: ${folder.absolutePath}")
        }
        result
      } else {
        false
      }
    }

    // Refresh draft list to remove deleted folder from UI
    if (deleted) {
      refreshDrafts()
    }
  }

  // ========== Draft Management ==========

  /**
   * Refreshes the draft list from disk.
   *
   * Scans the drafts directory for folders with [FileNames.METADATA] files
   * that have [BugReportState.DRAFT] or [BugReportState.SUBMITTED] state.
   * Updates [drafts] StateFlow atomically.
   *
   * I/O-heavy operations (file existence checks, JSON parsing) are performed
   * outside the mutex to avoid blocking other operations.
   */
  private suspend fun refreshDrafts(): Unit = withContext(Dispatchers.IO) {
    // Step 1: List folder names only (fast, mutex protects concurrent folder creation/deletion)
    val candidates = folderMutex.withLock {
      draftsDir.listFiles()
        ?.filter { it.isDirectory }
        ?.toList() ?: emptyList()
    }

    // Step 2: Filter + load metadata (slow I/O, outside mutex)
    val draftList = candidates
      .mapNotNull { folder ->
        runCatchingNonCancellation {
          val metadata = loadMetadata(folder) ?: return@mapNotNull null
          if (!metadata.state.isRetainedDraft) return@mapNotNull null
          DraftInfo(
            folderPath = folder.absolutePath,
            metadata = metadata,
            hasScreenshot = File(folder, SCREENSHOT).exists()
          )
        }.getOrNull() // Handles folder deleted mid-flight
      }
      .sortedByDescending { it.capturedAt }

    // Step 3: Atomic update
    _drafts.value = draftList
    Logger.d("Refreshed drafts: ${draftList.size} found")
  }

  /**
   * Saves user input to a capture folder, marking it as a draft.
   *
   * Updates the existing [FileNames.METADATA] file with [BugReportState.DRAFT] state
   * and the provided [userInput].
   *
   * After saving, evicts old drafts if over limit and refreshes the draft list.
   *
   * @param folder The capture folder
   * @param userInput The user-provided title and description
   */
  override suspend fun saveUserInput(folder: File, userInput: UserInput): Unit = withContext(Dispatchers.IO) {
    runCatchingNonCancellation {
      val existingMetadata = loadMetadata(folder)
      val updatedMetadata = existingMetadata?.copy(
        state = BugReportState.DRAFT,
        userInput = userInput
      ) ?: BugReportMetadata(
        capturedAt = folder.lastModified(),
        state = BugReportState.DRAFT,
        userInput = userInput,
        appInfo = appInfoProvider.getAppInfo(context)
      )
      saveMetadata(folder, updatedMetadata)
      Logger.d("Saved user input to: ${folder.absolutePath}")
    }.onFailure {
      Logger.e("Failed to save user input", it)
      return@withContext // Don't evict/refresh if save failed
    }

    // Now that we've created a new draft, evict old ones and refresh list
    evictOldDrafts()
    refreshDrafts()
  }

  /**
   * Marks a capture folder as submitted after a successful export.
   *
   * Updates the existing [FileNames.METADATA] file with [BugReportState.SUBMITTED] state.
   * This retains the draft so the user can re-share it from the draft picker.
   *
   * @param folder The capture folder
   */
  override suspend fun markAsSubmitted(folder: File): Unit = withContext(Dispatchers.IO) {
    folderMutex.withLock {
      runCatchingNonCancellation {
        val existingMetadata = loadMetadata(folder) ?: return@withContext
        val updatedMetadata = existingMetadata.copy(state = BugReportState.SUBMITTED)
        saveMetadata(folder, updatedMetadata)
        Logger.d("Marked as submitted: ${folder.absolutePath}")
      }.onFailure {
        Logger.e("Failed to mark as submitted", it)
        return@withContext
      }
    }

    evictOldDrafts()
    refreshDrafts()
  }

  /**
   * Evicts oldest drafts when count exceeds [maxDrafts].
   *
   * Counts/deletes folders that have [BugReportState.DRAFT] or [BugReportState.SUBMITTED] state,
   * not in-progress captures. This is safe to call after saveSnapshot.
   *
   * @param maxDrafts Maximum number of drafts to retain (default: [DEFAULT_MAX_DRAFTS])
   */
  private suspend fun evictOldDrafts(maxDrafts: Int = DEFAULT_MAX_DRAFTS): Unit = withContext(Dispatchers.IO) {
    // Hold mutex for entire operation to prevent race conditions
    folderMutex.withLock {
      val currentDrafts = draftsDir.listFiles()
        ?.filter { it.isDirectory }
        ?.mapNotNull { folder ->
          val metadata = loadMetadata(folder) ?: return@mapNotNull null
          if (!metadata.state.isRetainedDraft) return@mapNotNull null
          folder to metadata.capturedAt
        }
        ?.sortedByDescending { it.second }
        ?.map { it.first }
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
  }

  /**
   * Synchronously load metadata JSON. Called from Dispatchers.IO.
   */
  private fun loadMetadata(folder: File): BugReportMetadata? {
    val file = File(folder, METADATA)
    if (!file.exists()) return null
    return runCatching {
      json.decodeFromString(BugReportMetadata.serializer(), file.readText())
    }.getOrNull()
  }

  /**
   * Synchronously save metadata JSON. Called from Dispatchers.IO.
   */
  private fun saveMetadata(folder: File, metadata: BugReportMetadata) {
    val file = File(folder, METADATA)
    file.writeText(json.encodeToString(BugReportMetadata.serializer(), metadata))
  }

  private fun createFolder(): File {
    val folder = File(draftsDir, UUID.randomUUID().toString())
    folder.checkFolderExists()
    return folder
  }

  private fun saveScreenshot(bitmap: Bitmap, file: File) {
    FileOutputStream(file).use { out ->
      bitmap.compress(Bitmap.CompressFormat.PNG, UNUSED_PNG_QUALITY, out)
    }
  }
}

/**
 * Executes [block] catching all non-cancellation exceptions.
 * Logs failures but continues execution (best-effort persistence).
 */
private inline fun saveBestEffort(name: String, block: () -> Unit) {
  runCatchingNonCancellation(block)
    .onFailure { Logger.e("Failed to save $name", it) }
}
