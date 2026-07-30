package com.ms.square.debugoverlay.internal.bugreport.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.ms.square.debugoverlay.internal.ui.draggableOverlay
import com.ms.square.debugoverlay.internal.ui.rememberDraggableOverlayState
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
  val currentOnPositionChanged by rememberUpdatedState(onPositionChanged)

  val state = rememberDraggableOverlayState(
    initialOffset = Offset(initialOffsetX, initialOffsetY)
  )

  // Report position changes for WindowManager updates.
  // Rounding inside snapshotFlow means only whole-pixel changes are emitted — snapshotFlow drops
  // emissions equal to the previous one, so this avoids a WindowManager call per animation frame.
  LaunchedEffect(Unit) {
    snapshotFlow {
      IntOffset(state.offset.value.x.roundToInt(), state.offset.value.y.roundToInt())
    }.collect { position -> currentOnPositionChanged(position.x, position.y) }
  }

  Box(
    modifier = modifier.draggableOverlay(
      state = state,
      draggingScale = 1.1f
    ),
    contentAlignment = Alignment.Center
  ) {
    // Inner Box with padding to prevent clipping during drag scale (1.1x).
    //
    // draggingScale is a graphicsLayer transform, so it is draw-only and leaves the measured size
    // unchanged. This overlay's window is WRAP_CONTENT, so its surface is sized to that measured
    // size, and the compositor clips anything drawn beyond the surface. That clip is outside
    // Compose's control: graphicsLayer's clip = false does not affect it, and neither does
    // clipChildren (AndroidComposeView already sets it to false on itself). The only fix is real
    // layout space — verified on device, the FAB visibly clips without this.
    //
    // The padding MUST be inside the outer Box so the size draggableOverlay observes via
    // onRemeasured() includes it.
    // Layout structure:
    // Outer Box [draggableOverlay observes total size including padding]
    //   └─ Inner Box [padding reserves space for 1.1x scale]
    //        └─ BugReporterFab [56dp, scales to ~62dp during drag]
    Box(modifier = Modifier.padding(FAB_DRAG_PADDING)) {
      BugReporterFab(onError = onError)
    }
  }
}
