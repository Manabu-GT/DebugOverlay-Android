package com.ms.square.debugoverlay.internal.ui

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.launch

/**
 * Makes a composable draggable via long-press, with snap-to-edge behavior.
 *
 * Owns only the platform plumbing — gestures, haptics, sizes, the visual layer. The motion model and
 * the END/TOP coordinate convention live in [DraggableOverlayState].
 *
 * Tap handling is deliberately NOT included: compose it at the call site with a chained
 * `pointerInput { detectTapGestures { … } }`, guarded on [DraggableOverlayState.isDragging] so a
 * long-press that never moves does not also register as a tap.
 *
 * [draggingScale] is draw-only and does not enlarge the measured size, so a caller in a WRAP_CONTENT
 * window must reserve layout room for it itself (see `FAB_DRAG_PADDING`, `PANEL_DRAG_PADDING`) or the
 * window surface clips the scaled draw — a clip outside Compose's control. Left to the caller because
 * it is specific to that host.
 *
 * @param state The draggable state holder
 * @param draggingAlpha Opacity applied while dragging
 * @param draggingScale Scale applied while dragging
 * @param hapticFeedbackEnabled Whether to buzz on drag start
 */
internal fun Modifier.draggableOverlay(
  state: DraggableOverlayState,
  draggingAlpha: Float = 0.7f,
  draggingScale: Float = 1.05f,
  hapticFeedbackEnabled: Boolean = true,
): Modifier = this
  // One layer, and isDragging is read in the layer block rather than at composition time, so drag
  // start/end updates layer params only — no recomposition.
  .graphicsLayer {
    val dragging = state.isDragging
    alpha = if (dragging) draggingAlpha else 1f
    val targetScale = if (dragging) draggingScale else 1f
    scaleX = targetScale
    scaleY = targetScale
  }
  .then(DraggableOverlayElement(state, hapticFeedbackEnabled))

private data class DraggableOverlayElement(val state: DraggableOverlayState, val hapticFeedbackEnabled: Boolean) :
  ModifierNodeElement<DraggableOverlayNode>() {

  override fun create(): DraggableOverlayNode = DraggableOverlayNode(state, hapticFeedbackEnabled)

  override fun update(node: DraggableOverlayNode) {
    node.update(state, hapticFeedbackEnabled)
  }

  override fun InspectorInfo.inspectableProperties() {
    name = "draggableOverlay"
    properties["hapticFeedbackEnabled"] = hapticFeedbackEnabled
    properties["state"] = state
  }
}

private class DraggableOverlayNode(
  private var state: DraggableOverlayState,
  private var hapticFeedbackEnabled: Boolean,
) : DelegatingNode(),
  LayoutAwareModifierNode,
  CompositionLocalConsumerModifierNode,
  ObserverModifierNode {

  private var contentSize = IntSize.Zero
  private var containerSize = IntSize.Zero

  init {
    delegate(SuspendingPointerInputModifierNode { detectDrags() })
  }

  private suspend fun PointerInputScope.detectDrags() {
    detectDragGesturesAfterLongPress(
      onDragStart = {
        state.onDragStarted()
        if (hapticFeedbackEnabled) {
          currentValueOf(LocalHapticFeedback).performHapticFeedback(HapticFeedbackType.LongPress)
        }
      },
      onDrag = { change, dragAmount ->
        change.consume()
        // Accumulate synchronously so no delta can be lost, then flush an absolute position. The
        // flush is safe to cancel or drop: the next one catches up.
        state.onDrag(dragAmount, change.uptimeMillis)
        coroutineScope.launch { state.flushDrag() }
      },
      onDragEnd = {
        // Ends with the gesture, not with the settle animation. Clearing it only after settle
        // completes would keep the alpha/scale feedback applied for the whole animation, and would
        // make DraggableOverlayPanel's `if (!state.isDragging)` tap guard swallow taps during it.
        state.onDragStopped()
        coroutineScope.launch { state.settle() }
      },
      onDragCancel = {
        state.onDragStopped()
      }
    )
  }

  /**
   * Note this fires on every drag frame, not just on real size changes: moving the window via
   * `WindowManager.updateViewLayout` calls through to `ViewRootImpl.setLayoutParams` →
   * `requestLayout()`, which force-lays-out the ComposeView. Hence the size guard — without it,
   * bounds would be re-clamped and the observeReads re-registered 60+ times a second.
   */
  override fun onRemeasured(size: IntSize) {
    if (contentSize != size) {
      contentSize = size
      refreshBounds()
    }
  }

  override fun onObservedReadsChanged() {
    refreshBounds()
  }

  /**
   * Re-reads the container size and hands both sizes to [state].
   *
   * [observeReads] only tracks a single pass, so this has to re-register the observation every time
   * it runs — that is what keeps [onObservedReadsChanged] firing on subsequent container changes.
   */
  private fun refreshBounds() {
    observeReads {
      containerSize = currentValueOf(LocalWindowInfo).containerSize
    }
    state.updateBounds(contentSize, containerSize)
  }

  fun update(state: DraggableOverlayState, hapticFeedbackEnabled: Boolean) {
    this.hapticFeedbackEnabled = hapticFeedbackEnabled

    if (this.state !== state) {
      this.state = state
      refreshBounds()
    }
  }
}
