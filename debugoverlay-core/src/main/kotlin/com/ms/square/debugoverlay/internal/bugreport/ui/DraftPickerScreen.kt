package com.ms.square.debugoverlay.internal.bugreport.ui

import android.graphics.Bitmap
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import com.ms.square.debugoverlay.DebugOverlay
import com.ms.square.debugoverlay.core.R
import com.ms.square.debugoverlay.internal.Logger
import com.ms.square.debugoverlay.internal.bugreport.BugReportGenerator
import com.ms.square.debugoverlay.internal.bugreport.model.DraftInfo
import com.ms.square.debugoverlay.internal.util.runCatchingNonCancellation
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val THUMBNAIL_MAX_DIMENSION = 160

/**
 * Screen for selecting a saved draft or creating a new bug report.
 *
 * Shows a bottom sheet with:
 * - "Create New Report" button
 * - List of saved drafts with thumbnails
 * - Delete button with undo snackbar
 *
 * @param bugReportGenerator BugReportGenerator to use
 * @param onDraftSelected Called when a saved draft is selected (includes folder + saved userInput)
 * @param onNewCaptureCreated Called when a new capture is created (folder only, no saved metadata)
 * @param onDismiss Called when the sheet is dismissed
 * @param onError Called when an error occurs (e.g., capture failure)
 */
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DraftPickerScreen(
  bugReportGenerator: BugReportGenerator = DebugOverlay.bugReportGenerator,
  onDraftSelected: (DraftInfo) -> Unit,
  onNewCaptureCreated: (File) -> Unit,
  onDismiss: () -> Unit,
  onError: suspend (String) -> Unit,
) {
  val snackbarHostState = remember { SnackbarHostState() }
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val draftSelectionState = rememberDraftSelectionState()
  val scope = rememberCoroutineScope()

  // Observe drafts from storage
  val drafts by bugReportGenerator.drafts.collectAsState(initial = emptyList())

  // Track loaded thumbnails
  val thumbnails = remember { mutableStateMapOf<String, Bitmap?>() }

  // Load thumbnails for drafts
  LoadThumbnailsEffect(bugReportGenerator, drafts, thumbnails)

  val undoLabel = stringResource(R.string.debugoverlay_undo)
  val deletedMessage = stringResource(R.string.debugoverlay_draft_deleted)
  val draftNotFoundMessage = stringResource(R.string.debugoverlay_draft_not_found)
  val captureErrorMessage = stringResource(R.string.debugoverlay_bug_report_error)

  DraftSelectionBottomSheet(
    drafts = drafts,
    thumbnails = thumbnails,
    sheetState = sheetState,
    snackbarHostState = snackbarHostState,
    state = draftSelectionState,
    onCreateNew = {
      scope.launch {
        sheetState.hide()
        bugReportGenerator.captureToFolder()
          .onSuccess { folder -> onNewCaptureCreated(folder) }
          .onFailure {
            Logger.e("Failed to capture new bug report", it)
            onError(captureErrorMessage)
            onDismiss()
          }
      }
    },
    onDraftSelected = { draft ->
      scope.launch {
        if (draft.folder.exists()) {
          sheetState.hide()
          onDraftSelected(draft)
        } else {
          snackbarHostState.showSnackbar(draftNotFoundMessage)
        }
      }
    },
    onDraftDeleted = { draft ->
      scope.launch {
        val result = snackbarHostState.showSnackbar(
          message = deletedMessage,
          actionLabel = undoLabel,
          duration = SnackbarDuration.Short
        )
        when (result) {
          SnackbarResult.ActionPerformed -> draftSelectionState.undoDelete(draft)
          SnackbarResult.Dismissed -> withContext(NonCancellable) {
            bugReportGenerator.deleteCaptureFolder(draft.folder)
            draftSelectionState.confirmDelete(draft)
          }
        }
      }
    },
    onDismiss = {
      scope.launch {
        // Use NonCancellable to ensure deletions complete even if scope is cancelled
        // (e.g., when Activity finishes during auto-dismiss)
        withContext(NonCancellable) {
          draftSelectionState.pendingDelete.values.toList().forEach { draft ->
            bugReportGenerator.deleteCaptureFolder(draft.folder)
          }
        }
        onDismiss()
      }
    }
  )
}

@Composable
private fun LoadThumbnailsEffect(
  bugReportGenerator: BugReportGenerator,
  drafts: List<DraftInfo>,
  thumbnails: MutableMap<String, Bitmap?>,
) {
  LaunchedEffect(drafts) {
    // Remove thumbnails for drafts that no longer exist (prevents memory leak)
    val currentPaths = drafts.map { it.folderPath }.toSet()
    thumbnails.keys.filter { it !in currentPaths }.forEach { thumbnails.remove(it) }

    // Pre-register BEFORE filtering to prevent race
    drafts.forEach { draft ->
      if (draft.folderPath !in thumbnails) {
        thumbnails[draft.folderPath] = null
      }
    }
    // Now launch only for those we just registered
    val draftsToLoad = drafts.filter { draft ->
      thumbnails[draft.folderPath] == null && draft.hasScreenshot
    }
    // Load thumbnails in parallel with error handling
    draftsToLoad.forEach { draft ->
      launch {
        val thumbnail = runCatchingNonCancellation {
          bugReportGenerator.loadScreenshotPreview(
            draft.folder,
            maxDimension = THUMBNAIL_MAX_DIMENSION
          )
        }.getOrNull()
        thumbnails[draft.folderPath] = thumbnail
      }
    }
  }
}
