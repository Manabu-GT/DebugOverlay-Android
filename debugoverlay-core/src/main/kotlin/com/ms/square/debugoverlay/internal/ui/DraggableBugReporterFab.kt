package com.ms.square.debugoverlay.internal.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

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

  val fabSizePx = with(LocalDensity.current) { FAB_SIZE.toPx().roundToInt() }
  val contentSize = IntSize(fabSizePx, fabSizePx)
  val screenSize = IntSize(windowInfo.containerSize.width, windowInfo.containerSize.height)

  val state = rememberDraggableOverlayState(
    initialOffsetX = initialOffsetX,
    initialOffsetY = initialOffsetY,
    onPositionChanged = onPositionChanged
  )

  // Clamp position when screen size changes (e.g., rotation)
  LaunchedEffect(screenSize) {
    state.clampToBounds(contentSize, screenSize)
  }

  // Report position changes for WindowManager updates
  LaunchedEffect(Unit) {
    state.observePositionChanges(this)
  }

  Box(
    modifier = modifier.draggableOverlay(
      state = state,
      screenSize = screenSize,
      contentSize = contentSize,
      scope = scope,
      visualFeedback = DragVisualFeedback(draggingAlpha = 0.7f, draggingScale = 1.1f),
      onHapticFeedback = { view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) }
    )
  ) {
    BugReporterFab(onError = onError)
  }
}
