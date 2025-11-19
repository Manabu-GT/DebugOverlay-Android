package com.ms.square.debugoverlay.internal.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import com.ms.square.debugoverlay.internal.data.model.LogcatEntry

@Composable
internal fun AutoScrollManager(
  listState: LazyListState,
  filteredEntries: List<LogcatEntry>,
  isProgrammaticScroll: Boolean,
  isPaused: Boolean,
  onProgrammaticScrollChanged: (Boolean) -> Unit,
  onPauseChanged: (Boolean) -> Unit,
) {
  // Detect user scrolling and auto-pause
  LaunchedEffect(listState) {
    snapshotFlow {
      Triple(
        listState.isScrollInProgress,
        listState.canScrollForward,
        isProgrammaticScroll
      )
    }
      .collect { (isScrolling, canScrollForward, isProgrammatic) ->
        if (isScrolling && canScrollForward && !isProgrammatic) {
          onPauseChanged(true)
        }
      }
  }

  // Auto-scroll to bottom when new entries arrive (only if not paused)
  LaunchedEffect(filteredEntries, isPaused) {
    if (!isPaused && filteredEntries.isNotEmpty()) {
      try {
        onProgrammaticScrollChanged(true)
        listState.scrollToItem(
          index = filteredEntries.lastIndex
        )
      } finally {
        onProgrammaticScrollChanged(false)
        onPauseChanged(false)
      }
    }
  }
}
