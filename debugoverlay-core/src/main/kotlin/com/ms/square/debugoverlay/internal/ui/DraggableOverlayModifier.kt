package com.ms.square.debugoverlay.internal.ui

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Visual feedback configuration during drag.
 */
internal data class DragVisualFeedback(val draggingAlpha: Float = 0.7f, val draggingScale: Float = 1.05f)

/**
 * Makes a composable draggable via long-press with position persistence and snap-to-edge behavior.
 *
 * @param state The draggable state holder
 * @param screenSize Current screen size for bounds calculation
 * @param contentSize Size of the draggable content
 * @param scope CoroutineScope for launching animations
 * @param visualFeedback Alpha/scale effects during drag
 * @param onHapticFeedback Callback to trigger haptic feedback on drag start
 */
internal fun Modifier.draggableOverlay(
  state: DraggableOverlayState,
  screenSize: IntSize,
  contentSize: IntSize,
  scope: CoroutineScope,
  visualFeedback: DragVisualFeedback = DragVisualFeedback(),
  onHapticFeedback: () -> Unit = {},
): Modifier = this
  .alpha(if (state.isDragging) visualFeedback.draggingAlpha else 1f)
  .scale(if (state.isDragging) visualFeedback.draggingScale else 1f)
  .pointerInput(screenSize, contentSize) {
    detectDragGesturesAfterLongPress(
      onDragStart = {
        state.isDragging = true
        onHapticFeedback()
      },
      onDragEnd = {
        scope.launch {
          state.snapToEdge(screenSize.width.toFloat(), contentSize.width.toFloat())
          state.isDragging = false
        }
      },
      onDragCancel = {
        state.isDragging = false
      },
      onDrag = { change, dragAmount ->
        change.consume()
        scope.launch {
          state.updateOffset(
            dragDeltaX = dragAmount.x,
            dragDeltaY = dragAmount.y,
            contentSize = contentSize,
            screenSize = screenSize
          )
        }
      }
    )
  }
