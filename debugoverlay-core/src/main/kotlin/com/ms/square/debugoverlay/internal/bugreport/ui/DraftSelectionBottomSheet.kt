package com.ms.square.debugoverlay.internal.bugreport.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ms.square.debugoverlay.core.R
import com.ms.square.debugoverlay.internal.bugreport.model.DraftInfo

/**
 * State holder for [DraftSelectionBottomSheet].
 *
 * Manages the delay-delete pattern where deleted drafts are hidden immediately
 * but not actually deleted until the undo timeout expires.
 */
@Stable
internal class DraftSelectionState {
  /** Drafts pending deletion, mapped by folder path. Hidden from UI but not yet deleted. */
  internal val pendingDelete = mutableStateMapOf<String, DraftInfo>()

  /** The most recently deleted draft (for undo snackbar). */
  var lastDeleted: DraftInfo? by mutableStateOf(null)
    internal set

  /** Mark a draft for pending deletion. Returns true if added (not already pending). */
  fun markForDeletion(draft: DraftInfo): Boolean {
    if (draft.folderPath in pendingDelete) return false
    pendingDelete[draft.folderPath] = draft
    lastDeleted = draft
    return true
  }

  /** Restore a pending draft (undo). Returns true if restored. */
  fun undoDelete(draft: DraftInfo): Boolean {
    lastDeleted = null
    return pendingDelete.remove(draft.folderPath) != null
  }

  /** Confirm deletion - remove from pending state. Called after folder is actually deleted. */
  fun confirmDelete(draft: DraftInfo) {
    pendingDelete.remove(draft.folderPath)
    if (lastDeleted?.folderPath == draft.folderPath) {
      lastDeleted = null
    }
  }

  /** Filter out pending deletes from the visible draft list. */
  fun filterVisible(drafts: List<DraftInfo>): List<DraftInfo> = drafts.filterNot { it.folderPath in pendingDelete }
}

@Composable
internal fun rememberDraftSelectionState(): DraftSelectionState = remember { DraftSelectionState() }

/**
 * Callbacks for [DraftSelectionBottomSheet] actions.
 */
@Immutable
internal data class DraftSelectionCallbacks(
  val onCreateNew: () -> Unit,
  val onDraftSelected: (DraftInfo) -> Unit,
  val onDraftDeleted: (DraftInfo) -> Unit,
  val onDismiss: () -> Unit,
)

/**
 * Bottom sheet for selecting a draft to resume or creating a new report.
 *
 * Shows:
 * - "Create New Report" button at top
 * - "Saved Drafts (N)" section with draft list
 * - Each draft shows thumbnail, title, timestamp, and delete button
 *
 * Implements delay-delete pattern:
 * - Delete hides the item immediately
 * - Caller shows undo snackbar via [DraftSelectionCallbacks.onDraftDeleted] callback
 * - Caller calls [DraftSelectionState.confirmDelete] after timeout
 * - Caller calls [DraftSelectionState.undoDelete] if user taps undo
 *
 * @param drafts List of available drafts
 * @param thumbnails Map of folder path to loaded thumbnail bitmap
 * @param sheetState Sheet state for controlling visibility
 * @param snackbarHostState Snackbar host state for undo messages
 * @param state State holder for delay-delete pattern
 * @param callbacks Callbacks for user actions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DraftSelectionBottomSheet(
  drafts: List<DraftInfo>,
  thumbnails: Map<String, Bitmap?>,
  sheetState: SheetState,
  snackbarHostState: SnackbarHostState,
  state: DraftSelectionState,
  callbacks: DraftSelectionCallbacks,
) {
  val visibleDrafts = state.filterVisible(drafts)

  ModalBottomSheet(
    onDismissRequest = callbacks.onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
  ) {
    Box(modifier = Modifier.fillMaxWidth()) {
      DraftSelectionContent(
        visibleDrafts = visibleDrafts,
        thumbnails = thumbnails,
        state = state,
        callbacks = callbacks
      )

      SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .padding(16.dp)
      )
    }
  }
}

@Composable
private fun DraftSelectionContent(
  visibleDrafts: List<DraftInfo>,
  thumbnails: Map<String, Bitmap?>,
  state: DraftSelectionState,
  callbacks: DraftSelectionCallbacks,
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .navigationBarsPadding()
  ) {
    CreateNewButton(onClick = callbacks.onCreateNew)
    Spacer(modifier = Modifier.height(8.dp))
    HorizontalDivider()
    Spacer(modifier = Modifier.height(8.dp))
    DraftsSectionHeader(count = visibleDrafts.size)
    DraftList(
      drafts = visibleDrafts,
      thumbnails = thumbnails,
      state = state,
      onDraftSelected = callbacks.onDraftSelected,
      onDraftDeleted = callbacks.onDraftDeleted
    )
    Spacer(modifier = Modifier.height(16.dp))
  }
}

@Composable
private fun CreateNewButton(onClick: () -> Unit) {
  TextButton(
    onClick = onClick,
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 8.dp)
  ) {
    Icon(
      imageVector = Icons.Default.Add,
      contentDescription = null,
      modifier = Modifier.padding(end = 8.dp)
    )
    Text(
      text = stringResource(R.string.debugoverlay_create_new_report),
      style = MaterialTheme.typography.labelLarge
    )
  }
}

@Composable
private fun DraftsSectionHeader(count: Int) {
  Text(
    text = pluralStringResource(R.plurals.debugoverlay_saved_drafts, count, count),
    style = MaterialTheme.typography.titleSmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier
      .padding(horizontal = 16.dp, vertical = 8.dp)
      .semantics { heading() }
  )
}

@Composable
private fun DraftList(
  drafts: List<DraftInfo>,
  thumbnails: Map<String, Bitmap?>,
  state: DraftSelectionState,
  onDraftSelected: (DraftInfo) -> Unit,
  onDraftDeleted: (DraftInfo) -> Unit,
) {
  if (drafts.isEmpty()) {
    // Empty state - shown briefly before sheet auto-dismisses when all drafts deleted
    Text(
      text = stringResource(R.string.debugoverlay_no_saved_drafts),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
    )
  } else {
    LazyColumn {
      items(items = drafts, key = { it.folderPath }) { draft ->
        DraftListItem(
          draft = draft,
          thumbnail = thumbnails[draft.folderPath],
          onClick = { onDraftSelected(draft) },
          onDelete = {
            if (state.markForDeletion(draft)) {
              onDraftDeleted(draft)
            }
          }
        )
      }
    }
  }
}
