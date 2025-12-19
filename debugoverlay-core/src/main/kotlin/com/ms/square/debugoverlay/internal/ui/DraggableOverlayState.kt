package com.ms.square.debugoverlay.internal.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntSize

/**
 * State holder for draggable overlay components (FAB, Panel, etc.).
 *
 * Manages position animations, bounds clamping, and snap-to-edge behavior.
 * Uses gravity END/TOP coordinate system where x=0 is right edge, y=0 is top.
 *
 * @param initialOffsetX Initial X offset from right edge
 * @param initialOffsetY Initial Y offset from top
 */
@Stable
internal class DraggableOverlayState(initialOffsetX: Float, initialOffsetY: Float) {
  val offsetX = Animatable(initialOffsetX)
  val offsetY = Animatable(initialOffsetY)

  var isDragging by mutableStateOf(false)
    internal set

  /**
   * Snaps to the nearest horizontal edge with animation.
   *
   * @param screenWidth Total screen width
   * @param contentWidth Width of the draggable content
   */
  internal suspend fun snapToEdge(screenWidth: Float, contentWidth: Float) {
    val maxX = (screenWidth - contentWidth).coerceAtLeast(0f)
    val snapX = if (offsetX.value < maxX / 2) {
      0f // Snap to END (right)
    } else {
      maxX // Snap to START (left)
    }
    offsetX.animateTo(targetValue = snapX, animationSpec = tween())
  }

  /**
   * Updates offset based on drag gesture, with bounds clamping.
   * Uses gravity END/TOP coordinate system (x=0 is right edge, y=0 is top).
   */
  internal suspend fun updateOffset(dragDeltaX: Float, dragDeltaY: Float, contentSize: IntSize, screenSize: IntSize) {
    val maxX = (screenSize.width - contentSize.width).coerceAtLeast(0)
    val maxY = (screenSize.height - contentSize.height).coerceAtLeast(0)

    // LEFT is positive (flip sign because gravity is END)
    offsetX.snapTo((offsetX.value - dragDeltaX).coerceIn(0f, maxX.toFloat()))
    // DOWN is positive (gravity is TOP)
    offsetY.snapTo((offsetY.value + dragDeltaY).coerceIn(0f, maxY.toFloat()))
  }

  /**
   * Clamps position when screen/content size changes (e.g., rotation).
   */
  internal suspend fun clampToBounds(contentSize: IntSize, screenSize: IntSize) {
    if (contentSize.width > 0 && contentSize.height > 0) {
      val maxX = (screenSize.width - contentSize.width).coerceAtLeast(0).toFloat()
      val maxY = (screenSize.height - contentSize.height).coerceAtLeast(0).toFloat()

      val clampedX = offsetX.value.coerceIn(0f, maxX)
      val clampedY = offsetY.value.coerceIn(0f, maxY)

      if (offsetX.value != clampedX) offsetX.snapTo(clampedX)
      if (offsetY.value != clampedY) offsetY.snapTo(clampedY)
    }
  }
}

@Composable
internal fun rememberDraggableOverlayState(initialOffsetX: Float, initialOffsetY: Float): DraggableOverlayState =
  remember {
    DraggableOverlayState(initialOffsetX, initialOffsetY)
  }
