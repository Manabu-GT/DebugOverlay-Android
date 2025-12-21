package com.ms.square.debugoverlay.internal.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

// Padding to accommodate scale effect during drag (1.1x scale needs ~5% padding per side)
private val FAB_DRAG_PADDING = 4.dp

/**
 * A draggable wrapper for [BugReporterFab] that allows repositioning via drag gesture.
 *
 * Features:
 * - Drag to reposition anywhere on screen
 * - Snaps to nearest horizontal edge on drag end
 * - Reports position changes for persistence
 * - Visual feedback during drag (transparency, slight scale)
 *
 * @param initialOffsetX Initial X position (gravity END, 0 = right edge)
 * @param initialOffsetY Initial Y position (gravity TOP, 0 = top edge)
 * @param onPositionChanged Called when position changes (for persistence)
 * @param onError Called when bug report fails with error message
 */
@Composable
internal fun DraggableBugReporterFab(
  initialOffsetX: Float,
  initialOffsetY: Float,
  modifier: Modifier = Modifier,
  onPositionChanged: (x: Int, y: Int) -> Unit,
  onError: (String) -> Unit = {},
) {
  val windowInfo = LocalWindowInfo.current
  val view = LocalView.current
  val scope = rememberCoroutineScope()
  val currentOnPositionChanged by rememberUpdatedState(onPositionChanged)

  var contentSize by remember { mutableStateOf(IntSize.Zero) }
  val screenSize = remember(windowInfo.containerSize) {
    IntSize(windowInfo.containerSize.width, windowInfo.containerSize.height)
  }

  val state = rememberDraggableOverlayState(
    initialOffsetX = initialOffsetX,
    initialOffsetY = initialOffsetY
  )

  // Clamp position when screen size changes (e.g., rotation)
  LaunchedEffect(screenSize, contentSize) {
    state.clampToBounds(contentSize, screenSize)
  }

  // Report position changes for WindowManager updates
  LaunchedEffect(Unit) {
    snapshotFlow { state.offsetX.value to state.offsetY.value }
      .collect { (x, y) -> currentOnPositionChanged(x.roundToInt(), y.roundToInt()) }
  }

  Box(
    modifier = modifier
      .onSizeChanged { contentSize = it }
      .draggableOverlay(
        state = state,
        screenSize = screenSize,
        contentSize = contentSize,
        scope = scope,
        visualFeedback = DragVisualFeedback(draggingAlpha = 0.7f, draggingScale = 1.1f),
        onHapticFeedback = { view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) }
      ),
    contentAlignment = Alignment.Center
  ) {
    // Inner Box with padding to prevent clipping during drag scale (1.1x).
    // This padding MUST be inside the outer Box so onSizeChanged() captures
    // the total size including padding, reserving layout space for the scaled FAB.
    Box(modifier = Modifier.padding(FAB_DRAG_PADDING)) {
      BugReporterFab(onError = onError)
    }
  }
}
