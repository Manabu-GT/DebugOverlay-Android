@file:Suppress("MagicNumber", "TooManyFunctions")

package com.ms.square.debugoverlay.internal.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.ms.square.debugoverlay.core.R
import com.ms.square.debugoverlay.internal.data.model.DebugOverlayPanelMetrics
import com.ms.square.debugoverlay.internal.data.model.Metrics
import com.ms.square.debugoverlay.internal.data.model.ThermalState
import com.ms.square.debugoverlay.internal.data.model.ThermalStatus
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.roundToInt

private val STATUS_COLOR_NORMAL = Color(0xFF4CAF50)
private val STATUS_COLOR_WARNING = Color(0xFFFF9800)
private val STATUS_COLOR_CRITICAL = Color(0xFFF44336)

@Composable
internal fun DraggableOverlayPanel(
  metrics: DebugOverlayPanelMetrics?,
  thermalState: ThermalState?,
  initialOffsetX: Float,
  initialOffsetY: Float,
  modifier: Modifier = Modifier,
  onPositionChanged: (x: Int, y: Int) -> Unit,
  onClick: () -> Unit,
) {
  val windowInfo = LocalWindowInfo.current
  val view = LocalView.current
  val scope = rememberCoroutineScope()
  val currentOnPositionChanged by rememberUpdatedState(onPositionChanged)
  val currentOnClick by rememberUpdatedState(onClick)

  var panelSize by remember { mutableStateOf(IntSize.Zero) }
  val screenSize = remember(windowInfo.containerSize) {
    IntSize(windowInfo.containerSize.width, windowInfo.containerSize.height)
  }

  val state = rememberDraggableOverlayState(
    initialOffsetX = initialOffsetX,
    initialOffsetY = initialOffsetY
  )

  // Clamp position when screen size changes (e.g., rotation)
  LaunchedEffect(screenSize, panelSize) {
    state.clampToBounds(panelSize, screenSize)
  }

  // Report position changes for WindowManager updates
  LaunchedEffect(Unit) {
    snapshotFlow { state.offsetX.value.roundToInt() to state.offsetY.value.roundToInt() }
      .distinctUntilChanged()
      .collect { (x, y) -> currentOnPositionChanged(x, y) }
  }

  Box(
    modifier = modifier
      .onSizeChanged { panelSize = it }
      .draggableOverlay(
        state = state,
        screenSize = screenSize,
        contentSize = panelSize,
        scope = scope,
        onHapticFeedback = { view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) }
      )
      .pointerInput(Unit) {
        // Tap detection (separate from drag)
        detectTapGestures(
          onTap = {
            if (!state.isDragging) {
              currentOnClick()
            }
          }
        )
      }
  ) {
    DebugOverlayPanel(
      metrics = metrics,
      thermalState = thermalState
    )
  }
}

@Composable
internal fun DebugOverlayPanel(
  metrics: DebugOverlayPanelMetrics?,
  modifier: Modifier = Modifier,
  thermalState: ThermalState? = null,
) {
  // Disable font scaling to maintain consistent overlay panel size regardless of system font settings.
  // Debug overlay panel is for developers, so not supporting font scaling is acceptable.
  CompositionLocalProvider(
    LocalDensity provides Density(
      density = LocalDensity.current.density,
      fontScale = 1f
    )
  ) {
    metrics?.let {
      Surface(
        modifier = modifier
          .padding(all = 8.dp)
          .border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
            shape = MaterialTheme.shapes.medium
          ),
        shape = MaterialTheme.shapes.medium,
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
          if (thermalState != null && thermalState.status != ThermalStatus.UNSUPPORTED) {
            ThermalRow(thermalState.status)
          }
        }
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

/**
 * Bespoke row layout (no `MetricRow` / `LineGraph`) because thermal status is a categorical
 * enum rather than a continuous time-series — a sparkline would have nothing meaningful to plot.
 */
@Composable
private fun ThermalRow(status: ThermalStatus) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(6.dp)
          .background(status.toStatusColor(), CircleShape)
      )
      Text(
        text = stringResource(R.string.debugoverlay_thermal_compact_label),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
    Text(
      text = stringResource(status.compactLabelResId()),
      style = MaterialTheme.typography.titleSmall,
      color = MaterialTheme.colorScheme.onSurface,
      fontWeight = FontWeight.SemiBold
    )
  }
}

private fun ThermalStatus.toStatusColor(): Color = when (this) {
  ThermalStatus.NONE, ThermalStatus.LIGHT -> STATUS_COLOR_NORMAL
  ThermalStatus.MODERATE, ThermalStatus.SEVERE -> STATUS_COLOR_WARNING
  ThermalStatus.CRITICAL,
  ThermalStatus.EMERGENCY,
  ThermalStatus.SHUTDOWN,
  -> STATUS_COLOR_CRITICAL
  ThermalStatus.UNSUPPORTED -> STATUS_COLOR_NORMAL
}

private fun ThermalStatus.compactLabelResId(): Int = when (this) {
  ThermalStatus.NONE -> R.string.debugoverlay_thermal_status_none_abbr
  ThermalStatus.LIGHT -> R.string.debugoverlay_thermal_status_light_abbr
  ThermalStatus.MODERATE -> R.string.debugoverlay_thermal_status_moderate_abbr
  ThermalStatus.SEVERE -> R.string.debugoverlay_thermal_status_severe_abbr
  ThermalStatus.CRITICAL -> R.string.debugoverlay_thermal_status_critical_abbr
  ThermalStatus.EMERGENCY -> R.string.debugoverlay_thermal_status_emergency_abbr
  ThermalStatus.SHUTDOWN -> R.string.debugoverlay_thermal_status_shutdown_abbr
  // Defensive fallback; ThermalRow is hidden when status is UNSUPPORTED so this never renders.
  ThermalStatus.UNSUPPORTED -> R.string.debugoverlay_thermal_status_none_abbr
}
