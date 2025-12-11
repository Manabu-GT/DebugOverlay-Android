package com.ms.square.debugoverlay.internal.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
internal fun <T> AutoScrollManager(
  listState: LazyListState,
  filteredEntries: List<T>,
  isAutoScrollEnabled: Boolean,
  enableAutoScroll: () -> Unit,
  disableAutoScroll: () -> Unit,
) {
  // Detect if the user scrolled away from the bottom.
  // If they did, we DISABLE auto-scroll.
  LaunchedEffect(listState) {
    snapshotFlow {
      listState.isScrollInProgress to listState.canScrollForward
    }.distinctUntilChanged()
      .collect { (isScrollInProgress, canScrollForward) ->
        // If user is dragging/scrolling, and they are NOT at the bottom,
        // they are reading history -> Disable stickiness.
        if (isScrollInProgress && canScrollForward) {
          disableAutoScroll()
        }
        // Only re-enable if user intentionally scrolled back to bottom
        if (!canScrollForward && !isScrollInProgress) {
          enableAutoScroll()
        }
      }
  }

  // Auto-scroll to bottom when new entries arrive and if auto scroll is enabled
  LaunchedEffect(filteredEntries, isAutoScrollEnabled) {
    if (isAutoScrollEnabled && filteredEntries.isNotEmpty()) {
      listState.scrollToItem(
        index = filteredEntries.lastIndex
      )
    }
  }
}
