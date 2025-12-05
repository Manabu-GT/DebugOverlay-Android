package com.ms.square.debugoverlay.internal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ms.square.debugoverlay.DebugOverlay
import com.ms.square.debugoverlay.core.R
import com.ms.square.debugoverlay.internal.data.UrlParts
import com.ms.square.debugoverlay.internal.data.model.NetworkStats
import com.ms.square.debugoverlay.internal.util.HTTP_CLIENT_ERROR_START
import com.ms.square.debugoverlay.internal.util.formatBytes
import com.ms.square.debugoverlay.model.NetworkRequest
import kotlin.math.roundToInt

/**
 * Network tab showing HTTP requests with stats.
 *
 * Features:
 * - Download/Upload stats
 */
@Suppress("LongMethod") // Added detail bottom sheet handling
@Composable
internal fun NetworkTabContent(modifier: Modifier = Modifier) {
  val networkStats by DebugOverlay.overlayDataRepository.netStats.collectAsStateWithLifecycle(
    initialValue = NetworkStats.INITIAL_VALUE
  )
  val networkRequests by DebugOverlay.overlayDataRepository.networkRequests.collectAsStateWithLifecycle(
    initialValue = emptyList()
  )
  var searchQuery by remember { mutableStateOf("") }
  var selectedRequest by remember { mutableStateOf<NetworkRequest?>(null) }

  val augmentedNetworkStats = remember(networkStats, networkRequests) {
    networkStats.augmentNetworkStatsWith(networkRequests)
  }

  // Filter requests
  val filteredRequests = remember(networkRequests, searchQuery) {
    if (searchQuery.isEmpty()) {
      networkRequests
    } else {
      networkRequests.filter { request ->
        request.url.contains(searchQuery, ignoreCase = true) ||
          request.method.contains(searchQuery, ignoreCase = true)
      }
    }
  }

  Column(modifier = modifier.fillMaxSize()) {
    // Stats Header
    when {
      augmentedNetworkStats == NetworkStats.UNSUPPORTED -> {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Text(
            text = stringResource(R.string.debugoverlay_netstat_unavailable),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
      else -> {
        NetworkStatsHeader(augmentedNetworkStats)
      }
    }

    SearchField(
      searchPlaceholder = stringResource(R.string.debugoverlay_search_requests),
      searchQuery = searchQuery,
      onSearchQueryChanged = { searchQuery = it }
    )

    // Request List
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      items(
        items = filteredRequests,
        key = { it.id }
      ) { request ->
        NetworkRequestItem(
          request = request,
          onClick = { selectedRequest = request }
        )
      }
    }
  }

  // Show detail bottom sheet
  selectedRequest?.let { request ->
    NetworkRequestDetailBottomSheet(
      request = request,
      onDismiss = { selectedRequest = null }
    )
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
    Column(
      modifier = Modifier.padding(16.dp)
    ) {
      // Main stats row
      Row(
        modifier = modifier
          .fillMaxWidth(),
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
      NetworkStatsHeaderSubRow(networkStats)
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
        text = "${networkStats.totalRequests} requests",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      if (networkStats.errorCount > 0) {
        Text(
          text = "${networkStats.errorCount} errors",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.error
        )
      }

      Text(
        text = "Avg ${networkStats.avgDuration}ms",
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

/**
 * Individual network request item.
 */
@Composable
private fun NetworkRequestItem(request: NetworkRequest, onClick: () -> Unit, modifier: Modifier = Modifier) {
  val urlPath = remember(request.url) { UrlParts.from(request.url).path }
  Surface(
    onClick = onClick,
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(8.dp),
    color = MaterialTheme.colorScheme.surfaceContainerHighest
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Left side: Method badge + URL + metadata
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        // Method badge + URL
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          MethodBadge(method = request.method)

          Text(
            text = urlPath,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = FontFamily.Monospace,
            maxLines = 1
          )
        }

        // Duration + Size
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            text = "${request.durationMs}ms",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace
          )

          Text(
            text = "•",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
          )

          Text(
            text = formatBytes(request.responseSize),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace
          )
        }
      }

      // Right side: Status code
      StatusCodeBadge(statusCode = request.statusCode)
    }
  }
}

/**
 * Augment network statistics from requests.
 */
private fun NetworkStats.augmentNetworkStatsWith(requests: List<NetworkRequest>): NetworkStats {
  if (requests.isEmpty()) {
    return this
  }
  val errorCount = requests.count { it.statusCode != null && it.statusCode >= HTTP_CLIENT_ERROR_START }
  val avgDuration = requests.map { it.durationMs }.average().roundToInt().toLong()

  if (this == NetworkStats.UNSUPPORTED) {
    // since it might not have all the requests due to its size capping, this is just approximate.
    val totalDownloaded = requests.mapNotNull { it.responseSize }.filter { it >= 0 }.sumOf { it }
    val totalUploaded = requests.mapNotNull { it.requestSize }.filter { it >= 0 }.sumOf { it }
    return NetworkStats(
      totalDownloaded = totalDownloaded,
      totalUploaded = totalUploaded,
      totalRequests = requests.size,
      errorCount = errorCount,
      avgDuration = avgDuration
    )
  } else {
    return copy(
      totalRequests = requests.size,
      errorCount = errorCount,
      avgDuration = avgDuration
    )
  }
}
