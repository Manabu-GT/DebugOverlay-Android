package com.ms.square.debugoverlay.internal.bugreport.ui

import android.graphics.Bitmap
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.res.stringResource
import com.ms.square.debugoverlay.DebugOverlay
import com.ms.square.debugoverlay.core.R
import com.ms.square.debugoverlay.internal.Logger
import com.ms.square.debugoverlay.internal.bugreport.BugReportGenerator
import com.ms.square.debugoverlay.internal.bugreport.model.DraftInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val THUMBNAIL_MAX_DIMENSION = 160

/**
 * Holds localized strings used in the draft picker.
 */
private data class DraftPickerStrings(
  val undoLabel: String,
  val deletedMessage: String,
  val draftNotFoundMessage: String,
  val captureErrorMessage: String,
)

@Composable
private fun rememberDraftPickerStrings(): DraftPickerStrings = DraftPickerStrings(
  undoLabel = stringResource(R.string.debugoverlay_undo),
  deletedMessage = stringResource(R.string.debugoverlay_draft_deleted),
  draftNotFoundMessage = stringResource(R.string.debugoverlay_draft_not_found),
  captureErrorMessage = stringResource(R.string.debugoverlay_bug_report_error)
)

/**
 * Holds UI state for the draft picker screen.
 * References are stable (don't change), but the objects themselves contain mutable state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Stable
private class DraftPickerState(
  val scope: CoroutineScope,
  val snackbarHostState: SnackbarHostState,
  val sheetState: SheetState,
  val draftSelectionState: DraftSelectionState,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun rememberDraftPickerState(): DraftPickerState {
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val draftSelectionState = rememberDraftSelectionState()
  return remember(scope, snackbarHostState, sheetState, draftSelectionState) {
    DraftPickerState(scope, snackbarHostState, sheetState, draftSelectionState)
  }
}

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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DraftPickerScreen(
  bugReportGenerator: BugReportGenerator = DebugOverlay.bugReportGenerator,
  onDraftSelected: (DraftInfo) -> Unit,
  onNewCaptureCreated: (File) -> Unit,
  onDismiss: () -> Unit,
  onError: suspend (String) -> Unit,
) {
  val state = rememberDraftPickerState()
  val strings = rememberDraftPickerStrings()

  // Observe drafts from storage
  val drafts by bugReportGenerator.drafts.collectAsState(initial = emptyList())

  // Track loaded thumbnails
  val thumbnails = remember { mutableStateMapOf<String, Bitmap?>() }

  // Load thumbnails for drafts
  LoadThumbnailsEffect(bugReportGenerator, drafts, thumbnails)

  // Dismiss sheet when all drafts are deleted
  val visibleDrafts = state.draftSelectionState.filterVisible(drafts)
  AutoDismissEffect(
    bugReportGenerator = bugReportGenerator,
    drafts = drafts,
    visibleDrafts = visibleDrafts,
    sheetState = state.sheetState,
    draftSelectionState = state.draftSelectionState,
    onDismiss = onDismiss
  )

  // Create callbacks
  val callbacks = rememberDraftSelectionCallbacks(
    state = state,
    onDraftSelected = onDraftSelected,
    onNewCaptureCreated = onNewCaptureCreated,
    onDismiss = onDismiss,
    onError = onError,
    strings = strings
  )

  DraftSelectionBottomSheet(
    drafts = drafts,
    thumbnails = thumbnails,
    sheetState = state.sheetState,
    snackbarHostState = state.snackbarHostState,
    state = state.draftSelectionState,
    callbacks = callbacks
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

    // Determine which drafts need loading BEFORE launching coroutines (prevents race condition)
    val draftsToLoad = drafts.filter { it.folderPath !in thumbnails }

    // Pre-register with null to prevent duplicate launches on recomposition
    draftsToLoad.forEach { thumbnails[it.folderPath] = null }

    // Load thumbnails in parallel with error handling
    draftsToLoad.forEach { draft ->
      launch {
        val thumbnail = runCatching {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutoDismissEffect(
  bugReportGenerator: BugReportGenerator,
  drafts: List<DraftInfo>,
  visibleDrafts: List<DraftInfo>,
  sheetState: SheetState,
  draftSelectionState: DraftSelectionState,
  onDismiss: () -> Unit,
) {
  LaunchedEffect(visibleDrafts.size) {
    if (drafts.isNotEmpty() && visibleDrafts.isEmpty()) {
      // Delete pending drafts directly here (in suspend context with NonCancellable)
      // This avoids launching a new coroutine that might not start if scope is cancelled
      withContext(NonCancellable) {
        // toList() to avoid ConcurrentModificationException when iterating and removing
        draftSelectionState.pendingDelete.values.toList().forEach { draft ->
          bugReportGenerator.deleteCaptureFolder(draft.folder)
          draftSelectionState.confirmDelete(draft)
        }
      }
      sheetState.hide()
      onDismiss()
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun rememberDraftSelectionCallbacks(
  state: DraftPickerState,
  onDraftSelected: (DraftInfo) -> Unit,
  onNewCaptureCreated: (File) -> Unit,
  onDismiss: () -> Unit,
  onError: suspend (String) -> Unit,
  strings: DraftPickerStrings,
): DraftSelectionCallbacks {
  // Use rememberUpdatedState to capture latest values without recreating callbacks
  val currentOnDraftSelected by rememberUpdatedState(onDraftSelected)
  val currentOnNewCaptureCreated by rememberUpdatedState(onNewCaptureCreated)
  val currentOnDismiss by rememberUpdatedState(onDismiss)
  val currentOnError by rememberUpdatedState(onError)
  val currentStrings by rememberUpdatedState(strings)

  return remember(state) {
    DraftSelectionCallbacks(
      onCreateNew = {
        state.scope.launch {
          state.sheetState.hide()
          DebugOverlay.bugReportGenerator.captureToFolder()
            .onSuccess { folder -> currentOnNewCaptureCreated(folder) }
            .onFailure {
              Logger.e("Failed to capture new bug report", it)
              currentOnError(currentStrings.captureErrorMessage)
              currentOnDismiss()
            }
        }
      },
      onDraftSelected = { draft ->
        state.scope.launch {
          state.sheetState.hide()
          if (draft.folder.exists()) {
            currentOnDraftSelected(draft)
          } else {
            state.snackbarHostState.showSnackbar(currentStrings.draftNotFoundMessage)
          }
        }
      },
      onDraftDeleted = { draft ->
        state.scope.launch {
          val result = state.snackbarHostState.showSnackbar(
            message = currentStrings.deletedMessage,
            actionLabel = currentStrings.undoLabel,
            duration = SnackbarDuration.Short
          )
          when (result) {
            SnackbarResult.ActionPerformed -> state.draftSelectionState.undoDelete(draft)
            SnackbarResult.Dismissed -> withContext(NonCancellable) {
              DebugOverlay.bugReportGenerator.deleteCaptureFolder(draft.folder)
              state.draftSelectionState.confirmDelete(draft)
            }
          }
        }
      },
      onDismiss = {
        state.scope.launch {
          // Use NonCancellable to ensure deletions complete even if scope is cancelled
          // (e.g., when Activity finishes during auto-dismiss)
          withContext(NonCancellable) {
            state.draftSelectionState.pendingDelete.values.forEach { draft ->
              DebugOverlay.bugReportGenerator.deleteCaptureFolder(draft.folder)
            }
          }
          currentOnDismiss()
        }
      }
    )
  }
}
