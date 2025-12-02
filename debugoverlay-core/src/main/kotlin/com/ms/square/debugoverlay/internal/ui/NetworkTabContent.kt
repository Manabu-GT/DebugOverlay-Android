package com.ms.square.debugoverlay.internal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ms.square.debugoverlay.DebugOverlay
import com.ms.square.debugoverlay.core.R
import com.ms.square.debugoverlay.internal.data.model.NetworkStats

/**
 * Network tab showing HTTP requests with stats.
 *
 * Features:
 * - Download/Upload stats
 */
@Composable
internal fun NetworkTabContent(modifier: Modifier = Modifier) {
  val networkStats by DebugOverlay.overlayDataRepository.netStats.collectAsStateWithLifecycle(
    initialValue = NetworkStats.INITIAL_VALUE
  )

  Column(modifier = modifier.fillMaxSize()) {
    // Stats Header
    when {
      networkStats == NetworkStats.UNSUPPORTED -> {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Text(
            text = stringResource(R.string.debugoverlay_netstat_unavailable),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
      else -> {
        NetworkStatsHeader(networkStats)
      }
    }
  }
}

/**
 * Stats header showing download/upload totals.
 */
@Composable
private fun NetworkStatsHeader(networkStats: NetworkStats, modifier: Modifier = Modifier) {
  Surface(
    modifier = modifier
      .fillMaxWidth()
      .padding(16.dp),
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surfaceContainerHighest,
    tonalElevation = 2.dp
  ) {
    // Main stats row
    Row(
      modifier = modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 16.dp),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      // Downloaded
      NetworkStatsHeaderValue(
        horizontalAlignment = Alignment.Start,
        color = MaterialTheme.colorScheme.tertiary,
        title = stringResource(R.string.debugoverlay_netstat_downloaded),
        label = "↓",
        value = formatBytes(networkStats.totalDownloaded)
      )

      // Uploaded
      NetworkStatsHeaderValue(
        horizontalAlignment = Alignment.End,
        color = MaterialTheme.colorScheme.primary,
        title = stringResource(R.string.debugoverlay_netstat_uploaded),
        label = "↑",
        value = formatBytes(networkStats.totalUploaded)
      )
    }
  }
}

@Composable
private fun NetworkStatsHeaderValue(
  horizontalAlignment: Alignment.Horizontal,
  color: Color,
  title: String,
  label: String,
  value: String,
) {
  Column(horizontalAlignment = horizontalAlignment) {
    Text(
      text = title,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      letterSpacing = 0.5.sp
    )
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.headlineSmall,
        color = color
      )
      Text(
        text = value,
        style = MaterialTheme.typography.headlineSmall,
        color = color,
        fontWeight = FontWeight.Bold
      )
    }
  }
}

private const val BYTES_PER_KB = 1024L
private const val BYTES_PER_MB = 1024L * 1024L
private const val BYTES_PER_GB = 1024L * 1024L * 1024L

/**
 * Format bytes to human-readable string.
 */
private fun formatBytes(bytes: Long): String = when {
  bytes < 0 -> "—"
  bytes < BYTES_PER_KB -> "$bytes B"
  bytes < BYTES_PER_MB -> {
    val kb = bytes / BYTES_PER_KB.toDouble()
    @Suppress("MagicNumber")
    if (kb < 10) "%.2f KB".format(kb) else "%.1f KB".format(kb)
  }
  bytes < BYTES_PER_GB -> "%.1f MB".format(bytes / BYTES_PER_MB.toDouble())
  else -> "%.1f GB".format(bytes / BYTES_PER_GB.toDouble())
}
