package com.ms.square.debugoverlay.internal.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.IntSize

/**
 * State holder for draggable overlay components (FAB, Panel, etc.).
 *
 * Owns the whole motion model — position, bounds, drag accumulation, velocity and settle behavior —
 * and is the only place the gravity END/TOP coordinate convention (x=0 is the right edge, y=0 is the
 * top) is applied. Callers and modifiers work in plain screen-space deltas; the conversion happens
 * here and nowhere else.
 *
 * @param initialOffset Initial offset from top-right edge
 */
@Stable
internal class DraggableOverlayState(initialOffset: Offset, private val decayAnimSpec: DecayAnimationSpec<Offset>) {

  val offset = Animatable(initialOffset, Offset.VectorConverter)

  var isDragging by mutableStateOf(false)
    private set

  /** Largest allowed offset; set by [updateBounds]. */
  private var maxOffset = Offset.Zero

  /**
   * Absolute drag target, accumulated synchronously by [onDrag]. Because flushes carry an absolute
   * position rather than a delta, a canceled or superseded [flushDrag] costs at most one frame —
   * the next flush catches up. A relative snapTo would lose that delta permanently, since
   * [Animatable.snapTo] runs under a MutatorMutex where each call cancels the previous one.
   */
  private var dragTarget = Offset.Zero

  /**
   * Unclamped running total of the gesture's deltas, fed to [velocityTracker].
   * Kept separate from [dragTarget] because it must stay unclamped: clamping would flatten
   * velocity to zero whenever a drag is held against an edge, killing the fling.
   */
  private var dragDistance = Offset.Zero

  private val velocityTracker = VelocityTracker()

  /** Syncs the accumulators with the animated position, which [settle] may have moved. */
  fun onDragStarted() {
    dragTarget = offset.value
    dragDistance = Offset.Zero
    velocityTracker.resetTracking()
    isDragging = true
  }

  /**
   * Accumulates a screen-space drag delta. Deliberately NOT suspending: this must run synchronously
   * inside the gesture callback, which is what makes losing a delta impossible.
   *
   * Clamps [dragTarget] against [maxOffset] — unclamped, dragging past an edge would build up a
   * target you then have to drag all the way back before the overlay moved again. [dragDistance] is
   * accumulated unclamped alongside it, for velocity.
   */
  fun onDrag(dragDeltaOffset: Offset, timeMillis: Long) {
    // END gravity: x=0 is the right edge, so a LEFT drag (negative delta) increases x.
    // Gravity is TOP, so DOWN (positive delta) increases y.
    dragTarget = Offset(
      x = (dragTarget.x - dragDeltaOffset.x).coerceIn(0f, maxOffset.x),
      y = (dragTarget.y + dragDeltaOffset.y).coerceIn(0f, maxOffset.y)
    )
    dragDistance += dragDeltaOffset
    velocityTracker.addPosition(timeMillis, dragDistance)
  }

  /**
   * Ends the gesture. Call before launching [settle] — clearing this first is what stops a late
   * [flushDrag] from fighting the settle animation.
   */
  fun onDragStopped() {
    isDragging = false
  }

  /** Applies the accumulated [dragTarget]. Safe to cancel or skip entirely. */
  suspend fun flushDrag() {
    // A flush queued during the final onDrag can be scheduled after settle has already started;
    // without this it would snap back mid-animation. settle applies dragTarget itself, so skipping
    // here loses nothing.
    if (!isDragging) return
    offset.snapTo(dragTarget)
  }

  /**
   * Flings and snaps to the nearest horizontal edge, using the velocity accumulated during the
   * gesture. Bounds come from the last [updateBounds] call.
   */
  suspend fun settle() {
    // Apply the final accumulated position first: the last flushDrag may have been skipped by the
    // !isDragging guard, and decay must be computed from where the finger actually left off.
    offset.snapTo(dragTarget)

    val velocity = velocityTracker.calculateVelocity()
    // Horizontal only, and the y component is deliberately dropped rather than converted.
    //
    // Bounds on a 2-D Animatable are an animation *terminator*, not a per-axis clamp: runAnimation
    // calls cancelAnimation() as soon as clampToBounds alters ANY component. Since the target below
    // keeps y where the user left it, any y velocity can only push y out of bounds — which would
    // abort the x snap mid-flight and strand the overlay mid-screen. Releasing while dragging
    // against the top or bottom edge did exactly that. Zero y velocity keeps y in bounds, so the
    // only reachable bound-hit is x arriving at an edge, which is the intended terminator.
    //
    // x is negated because gesture velocity is in screen coords (+x = rightward) while offset uses
    // END gravity (x=0 is the right edge, increasing leftward).
    val initialVelocity = Offset(-velocity.x, 0f)
    val decay = decayAnimSpec.calculateTargetValue(Offset.VectorConverter, offset.value, initialVelocity)

    val maxX = maxOffset.x
    val toRightEdge = decay.x < maxX / 2f
    // Decay lands on an edge only if it overshoots it; bounds clamp the remainder.
    val decayReachesEdge = if (toRightEdge) decay.x <= 0f else decay.x >= maxX
    if (decayReachesEdge) {
      offset.animateDecay(initialVelocity = initialVelocity, animationSpec = decayAnimSpec)
    } else {
      offset.animateTo(
        targetValue = Offset(if (toRightEdge) 0f else maxX, offset.value.y),
        initialVelocity = initialVelocity
      )
    }
  }

  /**
   * Recomputes the draggable region and re-clamps the current offset when screen/content size changes (e.g., rotation).
   */
  fun updateBounds(contentSize: IntSize, containerSize: IntSize) {
    if (contentSize.width <= 0 || contentSize.height <= 0) return
    // coerceAtLeast(0) is required: Animatable.updateBounds() throws if lower > upper, which happens
    // whenever the content is larger than its container.
    maxOffset = Offset(
      x = (containerSize.width - contentSize.width).coerceAtLeast(0).toFloat(),
      y = (containerSize.height - contentSize.height).coerceAtLeast(0).toFloat()
    )
    offset.updateBounds(Offset.Zero, maxOffset)
  }

  override fun toString(): String = "DraggableOverlayState(offset=${offset.value}, isDragging=$isDragging, " +
    "maxOffset=$maxOffset, dragTarget=$dragTarget, dragDistance=$dragDistance)"
}

@Composable
internal fun rememberDraggableOverlayState(initialOffset: Offset = Offset.Zero): DraggableOverlayState {
  val decay = rememberSplineBasedDecay<Offset>()
  return remember {
    DraggableOverlayState(initialOffset, decay)
  }
}
