package com.ms.square.debugoverlay.internal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ms.square.debugoverlay.internal.data.model.DebugOverlayPanelMetrics
import com.ms.square.debugoverlay.internal.data.model.Metrics
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val STATUS_COLOR_NORMAL = Color(0xFF4CAF50)
private val STATUS_COLOR_WARNING = Color(0xFFFF9800)
private val STATUS_COLOR_CRITICAL = Color(0xFFF44336)

@Composable
internal fun DebugOverlayPanel(
  metrics: DebugOverlayPanelMetrics?,
  modifier: Modifier = Modifier,
  onClick: () -> Unit = {}
) {
  metrics?.let {
    Surface(
      modifier = modifier
        .pointerInput(Unit) {
          detectTapGestures(
            onTap = {
              onClick()
            }
          )
        }
        .padding(all = 8.dp)
        .border(
          width = 1.dp,
          color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
          shape = RoundedCornerShape(12.dp)
        ),
      shape = RoundedCornerShape(12.dp),
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
      tonalElevation = 3.dp,
      shadowElevation = 8.dp
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

// ==========================================================================
// Composable Previews with static data (no performance monitoring)
@Composable
fun DebugOverlayPanelPreview(
  modifier: Modifier = Modifier,
  cpuPercent: Float = 15f,
  heapPercent: Float = 72f,
  pss: Float = 256f,
  fps: Float = 60f,
  onClick: () -> Unit = {},
) {
  // Mock data that updates for preview
  var metrics by remember {
    mutableStateOf(
      DebugOverlayPanelMetrics(
        cpuMetrics = Metrics(cpuPercent, persistentListOf(12f, 15f, 18f, 15f, 13f, 15f, 17f, cpuPercent)),
        heapMetrics = Metrics(heapPercent, persistentListOf(65f, 68f, 70f, 72f, 71f, 70f, 72f, heapPercent)),
        pssMetrics = Metrics(pss, persistentListOf(165f, 168f, 170f, 172f, 171f, 170f, 202f, pss)),
        fpsMetrics = Metrics(fps, persistentListOf(60f, 59f, 60f, 60f, 58f, 60f, 59f, fps)),
        targetFps = 90f,
        maxFps = 90f,
        maxPss = 512f  // Typical mid-range device
      )
    )
  }

  // Simulate live updates in preview
  LaunchedEffect(Unit) {
    while (true) {
      delay(2000)
      val newCpu = (25..80).random().toFloat()
      val newHeap = (30..85).random().toFloat()
      val newPss = (100..256).random().toFloat()
      val newFps = (40..90).random().toFloat()

      metrics = metrics.copy(
        cpuMetrics = Metrics(
          newCpu,
          (metrics.cpuMetrics.valueHistory.toMutableList().drop(1) + newCpu).toImmutableList()
        ),
        heapMetrics = Metrics(
          newHeap,
          (metrics.heapMetrics.valueHistory.toMutableList().drop(1) + newHeap).toImmutableList()
        ),
        pssMetrics = Metrics(
          newPss,
          (metrics.pssMetrics.valueHistory.toMutableList().drop(1) + newPss).toImmutableList()
        ),
        fpsMetrics = Metrics(
          newFps,
          (metrics.fpsMetrics.valueHistory.toMutableList().drop(1) + newFps).toImmutableList()
        )
      )
    }
  }

  DebugOverlayPanel(
    metrics = metrics,
    modifier = modifier,
    onClick = onClick
  )
}

@Preview(name = "Light Theme", showBackground = true)
@Composable
private fun DebugOverlayPreviewLight() {
  MaterialTheme {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(16.dp),
      contentAlignment = Alignment.BottomEnd
    ) {
      // Show some app content behind
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(bottom = 80.dp)
      ) {
        repeat(3) { index ->
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 12.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 1.dp
          ) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(16.dp)
            ) {
              Text(
                text = "App Content ${index + 1}",
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }

      DebugOverlayPanelPreview(
        cpuPercent = 15f,
        heapPercent = 72f,
        pss = 180f,
        fps = 60f
      )
    }
  }
}

@Preview(name = "Dark Theme", showBackground = true)
@Composable
private fun DebugOverlayPreviewDark() {
  MaterialTheme(
    colorScheme = darkColorScheme()
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(16.dp),
      contentAlignment = Alignment.BottomEnd
    ) {
      // Show some app content behind
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(bottom = 80.dp)
      ) {
        repeat(3) { index ->
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 12.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 1.dp
          ) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(16.dp)
            ) {
              Text(
                text = "App Content ${index + 1}",
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }

      DebugOverlayPanelPreview(
        cpuPercent = 18f,
        heapPercent = 78f,
        pss = 110f,
        fps = 58f
      )
    }
  }
}

@Preview(name = "Panel Only - High Load", showBackground = true, widthDp = 200, heightDp = 180)
@Composable
private fun DebugOverlayPreviewHighLoad() {
  MaterialTheme {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(16.dp),
      contentAlignment = Alignment.Center
    ) {
      DebugOverlayPanelPreview(
        cpuPercent = 85f,
        heapPercent = 92f,
        pss = 800f,
        fps = 25f
      )
    }
  }
}
