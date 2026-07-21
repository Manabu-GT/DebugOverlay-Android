package com.ms.square.debugoverlay.internal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.North
import androidx.compose.material.icons.filled.South
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ms.square.debugoverlay.core.R
import com.ms.square.debugoverlay.internal.data.model.NetworkStats
import com.ms.square.debugoverlay.internal.util.formatBytes

/**
 * Stats header showing download/upload totals, pinned above the request list (see
 * [NetworkListScreen]) rather than scrolling with it — aggregate stats, especially the error
 * count, are ambient signal worth keeping glanceable while scrolling requests, not a control
 * the user reaches for and tucks away.
 *
 * On short-height windows ([isCompactHeight]), condenses to a single-line summary
 * ([CompactNetworkStatsSummary]) instead of the full two-row card, to reclaim room for the
 * request list while still keeping the (denser) stats pinned and visible.
 */
@Composable
internal fun NetworkStatsHeader(networkStats: NetworkStats, isCompactHeight: Boolean, modifier: Modifier = Modifier) {
  Surface(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = if (isCompactHeight) 8.dp else 16.dp),
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.surfaceContainerHighest,
    tonalElevation = 2.dp
  ) {
    if (isCompactHeight) {
      CompactNetworkStatsSummary(
        networkStats = networkStats,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
      )
    } else {
      Column(
        modifier = Modifier.padding(16.dp)
      ) {
        // Main stats row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          // Downloaded
          NetworkStatsHeaderValue(
            horizontalAlignment = Alignment.Start,
            color = MaterialTheme.colorScheme.tertiary,
            title = stringResource(R.string.debugoverlay_netstat_downloaded),
            icon = Icons.Default.South,
            value = formatBytes(networkStats.totalDownloaded)
          )

          // Uploaded
          NetworkStatsHeaderValue(
            horizontalAlignment = Alignment.End,
            color = MaterialTheme.colorScheme.primary,
            title = stringResource(R.string.debugoverlay_netstat_uploaded),
            icon = Icons.Default.North,
            value = formatBytes(networkStats.totalUploaded)
          )
        }
        NetworkStatsHeaderSubRow(networkStats)
      }
    }
  }
}

@Composable
private fun NetworkStatsHeaderSubRow(networkStats: NetworkStats) {
  if (!(networkStats.totalRequests == null || networkStats.errorCount == null || networkStats.avgDuration == null)) {
    Spacer(modifier = Modifier.height(8.dp))

    // Secondary stats row
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(
        text = stringResource(R.string.debugoverlay_netstat_requests, networkStats.totalRequests),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      if (networkStats.errorCount > 0) {
        Text(
          text = stringResource(R.string.debugoverlay_netstat_errors, networkStats.errorCount),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.error
        )
      }

      Text(
        text = stringResource(R.string.debugoverlay_netstat_avg_duration, networkStats.avgDuration),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@Composable
private fun NetworkStatsHeaderValue(
  horizontalAlignment: Alignment.Horizontal,
  color: Color,
  title: String,
  icon: ImageVector,
  value: String,
) {
  Column(horizontalAlignment = horizontalAlignment) {
    Text(
      text = title,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(20.dp),
        tint = color
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

/**
 * Compact-height variant: every stat on one line (icon + value, no title captions, dot
 * separators) so the pinned header costs a single row instead of a full card, while keeping
 * download/upload/requests/errors/duration all still glanceable without scrolling.
 * Wrapped in [FlowRow] as a defensive fallback for windows that are compact in both dimensions.
 */
@Composable
private fun CompactNetworkStatsSummary(networkStats: NetworkStats, modifier: Modifier = Modifier) {
  FlowRow(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalArrangement = Arrangement.spacedBy(2.dp)
  ) {
    CompactStatValue(
      icon = Icons.Default.South,
      color = MaterialTheme.colorScheme.tertiary,
      value = formatBytes(networkStats.totalDownloaded)
    )
    Dot()
    CompactStatValue(
      icon = Icons.Default.North,
      color = MaterialTheme.colorScheme.primary,
      value = formatBytes(networkStats.totalUploaded)
    )
    if (!(networkStats.totalRequests == null || networkStats.errorCount == null || networkStats.avgDuration == null)) {
      Dot()
      Text(
        text = stringResource(R.string.debugoverlay_netstat_requests, networkStats.totalRequests),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      if (networkStats.errorCount > 0) {
        Dot()
        Text(
          text = stringResource(R.string.debugoverlay_netstat_errors, networkStats.errorCount),
          style = MaterialTheme.typography.bodySmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.error
        )
      }
      Dot()
      Text(
        text = stringResource(R.string.debugoverlay_netstat_avg_duration, networkStats.avgDuration),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@Composable
private fun CompactStatValue(icon: ImageVector, color: Color, value: String, modifier: Modifier = Modifier) {
  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(2.dp)
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      modifier = Modifier.size(14.dp),
      tint = color
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodySmall,
      color = color,
      fontWeight = FontWeight.Bold
    )
  }
}
