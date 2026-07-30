package com.ms.square.debugoverlay.internal.ui

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Makes a composable draggable via long-press, with snap-to-edge and tap handling.
 *
 * Owns only the platform plumbing — gestures, haptics, sizes, the visual layer. The motion model and
 * the END/TOP coordinate convention live in [DraggableOverlayState].
 *
 * [draggingScale] is draw-only and does not enlarge the measured size, so a caller in a WRAP_CONTENT
 * window must reserve layout room for it itself (see `FAB_DRAG_PADDING`) or the window surface clips
 * the scaled draw — a clip outside Compose's control. Left to the caller because it is specific to
 * that host.
 *
 * @param state The draggable state holder
 * @param draggingAlpha Opacity applied while dragging
 * @param draggingScale Scale applied while dragging
 * @param hapticFeedbackEnabled Whether to buzz on drag start
 * @param onClick Invoked on tap. Null disables tap detection entirely.
 */
internal fun Modifier.draggableOverlay(
  state: DraggableOverlayState,
  draggingAlpha: Float = 0.7f,
  draggingScale: Float = 1.05f,
  hapticFeedbackEnabled: Boolean = true,
  onClick: (() -> Unit)? = null,
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
  .then(DraggableOverlayElement(state, hapticFeedbackEnabled, onClick))

private data class DraggableOverlayElement(
  val state: DraggableOverlayState,
  val hapticFeedbackEnabled: Boolean,
  val onClick: (() -> Unit)?,
) : ModifierNodeElement<DraggableOverlayNode>() {

  override fun create(): DraggableOverlayNode = DraggableOverlayNode(state, hapticFeedbackEnabled, onClick)

  override fun update(node: DraggableOverlayNode) {
    node.update(state, hapticFeedbackEnabled, onClick)
  }

  override fun InspectorInfo.inspectableProperties() {
    name = "draggableOverlay"
    properties["hapticFeedbackEnabled"] = hapticFeedbackEnabled
    properties["onClick"] = onClick
    properties["state"] = state
  }
}

private class DraggableOverlayNode(
  private var state: DraggableOverlayState,
  private var hapticFeedbackEnabled: Boolean,
  private var onClick: (() -> Unit)?,
) : DelegatingNode(),
  LayoutAwareModifierNode,
  CompositionLocalConsumerModifierNode,
  ObserverModifierNode {

  private var contentSize = IntSize.Zero
  private var containerSize = IntSize.Zero

  private val pointerInputNode = delegate(SuspendingPointerInputModifierNode { detectGestures() })

  /**
   * Both detectors run in parallel on the same events, arbitrating by consumption: once the long
   * press fires the drag consumes every MOVE, which makes the tap detector's `waitForUpOrCancellation`
   * return null, so it skips `onTap` and waits for the next gesture. Neither detector ever returns —
   * both loop until this handler is reset.
   */
  private suspend fun PointerInputScope.detectGestures() {
    coroutineScope {
      launch { detectDrags() }
      // Skipped entirely when there is no onClick, so callers like the FAB — whose content has its
      // own clickable — do not get a down-consumer they have no use for.
      if (onClick != null) launch { detectTaps() }
    }
  }

  /**
   * [detectTapGestures] with no `onLongPress` applies no long-press timeout: it fires on release
   * regardless of hold duration. So a long-press that never moves would otherwise register as a tap,
   * which is what the [DraggableOverlayState.isDragging] guard rules out.
   */
  private suspend fun PointerInputScope.detectTaps() {
    detectTapGestures(onTap = { if (!state.isDragging) onClick?.invoke() })
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

  fun update(state: DraggableOverlayState, hapticFeedbackEnabled: Boolean, onClick: (() -> Unit)?) {
    this.hapticFeedbackEnabled = hapticFeedbackEnabled

    // Only the null-ness matters to the handler; the lambda itself is read fresh on every tap, so it
    // can never go stale and a mere identity change needs no restart.
    val tapDetectionChanged = (this.onClick == null) != (onClick == null)
    this.onClick = onClick
    if (tapDetectionChanged) {
      pointerInputNode.resetPointerInputHandler()
    }

    if (this.state !== state) {
      this.state = state
      refreshBounds()
    }
  }
}
