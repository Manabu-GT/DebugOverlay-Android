@file:Suppress("MagicNumber")

package com.ms.square.debugoverlay.internal.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import com.ms.square.debugoverlay.internal.data.model.DebugOverlayPanelMetrics
import com.ms.square.debugoverlay.internal.data.model.Metrics
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val STATUS_COLOR_NORMAL = Color(0xFF4CAF50)
private val STATUS_COLOR_WARNING = Color(0xFFFF9800)
private val STATUS_COLOR_CRITICAL = Color(0xFFF44336)

@Suppress("LongMethod")
@Composable
internal fun DraggableOverlayPanel(
  metrics: DebugOverlayPanelMetrics?,
  initialOffsetX: Float,
  initialOffsetY: Float,
  modifier: Modifier = Modifier,
  onPositionChanged: (x: Int, y: Int) -> Unit,
  onClick: () -> Unit,
) {
  val windowInfo = LocalWindowInfo.current
  val view = LocalView.current
  val scope = rememberCoroutineScope()

  val screenWidth = windowInfo.containerSize.width.toFloat()
  val screenHeight = windowInfo.containerSize.height.toFloat()

  val offsetX = remember { Animatable(initialOffsetX) }
  val offsetY = remember { Animatable(initialOffsetY) }
  var isDragging by remember { mutableStateOf(false) }

  // Continuously update WindowManager position whenever animated values change
  LaunchedEffect(Unit) {
    snapshotFlow { offsetX.value to offsetY.value }
      .onEach { (x, y) ->
        onPositionChanged(x.roundToInt(), y.roundToInt())
      }
      .launchIn(this)
  }

  Box(
    modifier = modifier
      .alpha(if (isDragging) 0.7f else 1f) // Transparency while dragging
      .scale(if (isDragging) 1.05f else 1f) // Slightly larger while dragging
      .pointerInput(Unit) {
        detectDragGesturesAfterLongPress(
          onDragStart = {
            isDragging = true
            // Trigger haptic feedback
            view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
          },
          onDragEnd = {
            // Snap to nearest edge with animation
            val snapX = when {
              offsetX.value < (screenWidth - size.width) / 2 -> 0f // Snap to END (right)
              else -> (screenWidth - size.width) // Snap to START (left)
            }
            scope.launch {
              offsetX.animateTo(
                targetValue = snapX,
                animationSpec = tween()
              )
            }
            isDragging = false
          },
          onDragCancel = {
            isDragging = false
          },
          onDrag = { change, dragAmount ->
            change.consume()

            scope.launch {
              // LEFT is positive (flip sign because gravity is END, where x=0 is at right edge)
              offsetX.snapTo((offsetX.value - dragAmount.x).coerceIn(0f, screenWidth - size.width))
              // DOWN is positive since gravity is TOP
              offsetY.snapTo((offsetY.value + dragAmount.y).coerceIn(0f, screenHeight - size.height))
            }
          }
        )
      }
      .pointerInput(Unit) {
        // Tap detection (separate from drag)
        detectTapGestures(
          onTap = {
            if (!isDragging) {
              onClick()
            }
          }
        )
      }
  ) {
    DebugOverlayPanel(
      metrics = metrics
    )
  }
}

@Composable
internal fun DebugOverlayPanel(metrics: DebugOverlayPanelMetrics?, modifier: Modifier = Modifier) {
  metrics?.let {
    Surface(
      modifier = modifier
        .padding(all = 8.dp)
        .border(
          width = 1.dp,
          color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
          shape = RoundedCornerShape(12.dp)
        ),
      shape = RoundedCornerShape(12.dp),
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
      tonalElevation = 3.dp
    ) {
      Column(
        modifier = Modifier
          .padding(all = 8.dp)
          .widthIn(max = 120.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        CpuRow(it.cpuMetrics)
        HeapRow(it.heapMetrics)
        PssRow(it.pssMetrics, it.maxPss)
        FpsRow(it.fpsMetrics, it.targetFps, it.maxFps)
      }
    }
  }
}

@Composable
private fun CpuRow(cpuMetrics: Metrics) {
  MetricRow(
    label = "Cpu",
    value = "${cpuMetrics.value.roundToInt()}%",
    statusColor = cpuMetrics.value.toCpuStatusColor(),
    lineGraphData = cpuMetrics.valueHistory,
    lineGraphColor = Color(0xFF3B82F6),
    lineGraphMinValue = 0f,
    lineGraphMaxValue = 100f
  )
}

@Composable
private fun HeapRow(heapMetrics: Metrics) {
  MetricRow(
    label = "Heap",
    value = "${heapMetrics.value.roundToInt()}%",
    statusColor = heapMetrics.value.toMemHeapStatusColor(),
    lineGraphData = heapMetrics.valueHistory,
    lineGraphColor = Color(0xFFF59E0B),
    lineGraphMinValue = 0f,
    lineGraphMaxValue = 100f
  )
}

@Composable
private fun PssRow(pssMetrics: Metrics, maxPss: Float) {
  MetricRow(
    label = "Pss",
    value = "${pssMetrics.value.roundToInt()}",
    statusColor = pssMetrics.value.toMemPssStatusColor(),
    lineGraphData = pssMetrics.valueHistory,
    lineGraphColor = Color(0xFF00BCD4),
    lineGraphMinValue = 0f,
    lineGraphMaxValue = maxPss
  )
}

@Composable
private fun FpsRow(fpsMetrics: Metrics, targetFps: Float, maxFps: Float) {
  MetricRow(
    label = "Fps",
    value = "${fpsMetrics.value.roundToInt()}",
    statusColor = fpsMetrics.value.toFpsStatusColor(targetFps),
    lineGraphData = fpsMetrics.valueHistory,
    lineGraphColor = Color(0xFF10B981),
    lineGraphMinValue = 0f,
    lineGraphMaxValue = maxFps
  )
}

private fun Float.toCpuStatusColor(): Color = when {
  this > 80 -> STATUS_COLOR_CRITICAL
  this > 50 -> STATUS_COLOR_WARNING
  else -> STATUS_COLOR_NORMAL
}

private fun Float.toFpsStatusColor(targetFps: Float): Color {
  val fpsRatio = this / targetFps
  return when {
    fpsRatio < .5f -> STATUS_COLOR_CRITICAL
    fpsRatio < .8f -> STATUS_COLOR_WARNING
    else -> STATUS_COLOR_NORMAL
  }
}

private fun Float.toMemHeapStatusColor(): Color = when {
  this > 85 -> STATUS_COLOR_CRITICAL
  this > 70 -> STATUS_COLOR_WARNING
  else -> STATUS_COLOR_NORMAL
}

private fun Float.toMemPssStatusColor(): Color = when {
  this > 750 -> STATUS_COLOR_CRITICAL
  this > 500 -> STATUS_COLOR_WARNING
  else -> STATUS_COLOR_NORMAL
}
