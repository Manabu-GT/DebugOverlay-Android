@file:Suppress("MagicNumber", "UnusedPrivateMember")

package com.ms.square.debugoverlay.internal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ms.square.debugoverlay.internal.data.model.DebugOverlayPanelMetrics
import com.ms.square.debugoverlay.internal.data.model.Metrics
import com.ms.square.debugoverlay.internal.data.model.ThermalState
import com.ms.square.debugoverlay.internal.data.model.ThermalStatus
import kotlinx.coroutines.delay

// Composable Previews with static data (no performance monitoring)
@Composable
private fun DebugOverlayPanelPreview(
  modifier: Modifier = Modifier,
  cpuPercent: Float = 15f,
  heapPercent: Float = 72f,
  pss: Float = 256f,
  fps: Float = 60f,
  thermalState: ThermalState? = null,
) {
  // Mock data that updates for preview
  var metrics by remember {
    mutableStateOf(
      DebugOverlayPanelMetrics(
        cpuMetrics = Metrics(cpuPercent, listOf(12f, 15f, 18f, 15f, 13f, 15f, 17f, cpuPercent)),
        heapMetrics = Metrics(heapPercent, listOf(65f, 68f, 70f, 72f, 71f, 70f, 72f, heapPercent)),
        pssMetrics = Metrics(pss, listOf(165f, 168f, 170f, 172f, 171f, 170f, 202f, pss)),
        fpsMetrics = Metrics(fps, listOf(60f, 59f, 60f, 60f, 58f, 60f, 59f, fps)),
        targetFps = 90f,
        maxFps = 90f,
        maxPss = 512f // Typical mid-range device
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
          (metrics.cpuMetrics.valueHistory.toMutableList().drop(1) + newCpu).toList()
        ),
        heapMetrics = Metrics(
          newHeap,
          (metrics.heapMetrics.valueHistory.toMutableList().drop(1) + newHeap).toList()
        ),
        pssMetrics = Metrics(
          newPss,
          (metrics.pssMetrics.valueHistory.toMutableList().drop(1) + newPss).toList()
        ),
        fpsMetrics = Metrics(
          newFps,
          (metrics.fpsMetrics.valueHistory.toMutableList().drop(1) + newFps).toList()
        )
      )
    }
  }

  DebugOverlayPanel(
    metrics = metrics,
    modifier = modifier,
    thermalState = thermalState
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
            shape = MaterialTheme.shapes.medium,
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
            shape = MaterialTheme.shapes.medium,
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

@Preview(name = "Panel Only - With Thermal (Severe)", showBackground = true, widthDp = 200, heightDp = 200)
@Composable
private fun DebugOverlayPreviewWithThermal() {
  MaterialTheme {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(16.dp),
      contentAlignment = Alignment.Center
    ) {
      DebugOverlayPanelPreview(
        cpuPercent = 72f,
        heapPercent = 65f,
        pss = 380f,
        fps = 42f,
        thermalState = ThermalState(ThermalStatus.SEVERE)
      )
    }
  }
}
