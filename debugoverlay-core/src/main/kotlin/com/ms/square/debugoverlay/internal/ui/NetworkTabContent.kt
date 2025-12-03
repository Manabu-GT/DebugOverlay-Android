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
import com.ms.square.debugoverlay.internal.data.model.NetworkStats
import com.ms.square.debugoverlay.model.HttpMethod
import com.ms.square.debugoverlay.model.NetworkRequest
import kotlin.math.roundToInt

// HTTP Status Code ranges
private const val HTTP_SUCCESS_START = 200
private const val HTTP_SUCCESS_END = 299
private const val HTTP_REDIRECT_START = 300
private const val HTTP_REDIRECT_END = 399
private const val HTTP_CLIENT_ERROR_START = 400
private const val HTTP_CLIENT_ERROR_END = 499
private const val HTTP_SERVER_ERROR_START = 500
private const val HTTP_SERVER_ERROR_END = 599

// Status code colors
private val STATUS_COLOR_SUCCESS = Color(0xFF4CAF50) // Green
private val STATUS_COLOR_REDIRECT = Color(0xFF2196F3) // Blue
private val STATUS_COLOR_CLIENT_ERROR = Color(0xFFF44336) // Red
private val STATUS_COLOR_SERVER_ERROR = Color(0xFFFF5722) // Deep orange
private val STATUS_COLOR_UNKNOWN = Color(0xFF757575) // Gray

// HTTP method colors
private val METHOD_COLOR_GET = Color(0xFF03DAC6) // Cyan
private val METHOD_COLOR_POST = Color(0xFFFFC107) // Amber
private val METHOD_COLOR_PUT = Color(0xFF2196F3) // Blue
private val METHOD_COLOR_DELETE = Color(0xFFF44336) // Red
private val METHOD_COLOR_PATCH = Color(0xFF9C27B0) // Purple
private val METHOD_COLOR_UNKNOWN = Color(0xFF757575) // Gray

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
  val networkRequests by DebugOverlay.overlayDataRepository.networkRequests.collectAsStateWithLifecycle(
    initialValue = emptyList()
  )
  var searchQuery by remember { mutableStateOf("") }

  val augmentedNetworkStats = remember(networkStats, networkRequests) {
    networkStats.augmentNetworkStatsWith(networkRequests)
  }

  // Filter requests
  val filteredRequests = remember(networkRequests, searchQuery) {
    if (searchQuery.isEmpty()) {
      networkRequests
    } else {
      networkRequests.filter { request ->
        request.shortUrl.contains(searchQuery, ignoreCase = true) ||
          request.method.toString().contains(searchQuery, ignoreCase = true)
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
          onClick = { /* No-op: Detail view not implemented yet */ }
        )
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
            text = request.shortUrl,
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
 * HTTP method badge (GET, POST, etc.)
 */
@Composable
private fun MethodBadge(method: HttpMethod, modifier: Modifier = Modifier) {
  Surface(
    modifier = modifier,
    color = method.toColor(),
    shape = RoundedCornerShape(4.dp)
  ) {
    Text(
      text = method.name,
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
      style = MaterialTheme.typography.labelSmall,
      color = Color.Black,
      fontWeight = FontWeight.Bold,
      fontSize = 10.sp
    )
  }
}

/**
 * Status code badge with color coding.
 */
@Composable
private fun StatusCodeBadge(statusCode: Int?, modifier: Modifier = Modifier) {
  val color = when (statusCode) {
    in HTTP_SUCCESS_START..HTTP_SUCCESS_END -> STATUS_COLOR_SUCCESS
    in HTTP_REDIRECT_START..HTTP_REDIRECT_END -> STATUS_COLOR_REDIRECT
    in HTTP_CLIENT_ERROR_START..HTTP_CLIENT_ERROR_END -> STATUS_COLOR_CLIENT_ERROR
    in HTTP_SERVER_ERROR_START..HTTP_SERVER_ERROR_END -> STATUS_COLOR_SERVER_ERROR
    else -> STATUS_COLOR_UNKNOWN
  }

  Text(
    text = statusCode?.toString() ?: "ERR",
    modifier = modifier,
    style = MaterialTheme.typography.titleMedium,
    color = color,
    fontWeight = FontWeight.Bold,
    fontFamily = FontFamily.Monospace
  )
}

private fun HttpMethod.toColor(): Color = when (this) {
  HttpMethod.GET -> METHOD_COLOR_GET
  HttpMethod.POST -> METHOD_COLOR_POST
  HttpMethod.PUT -> METHOD_COLOR_PUT
  HttpMethod.DELETE -> METHOD_COLOR_DELETE
  HttpMethod.PATCH -> METHOD_COLOR_PATCH
  else -> METHOD_COLOR_UNKNOWN
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
    val totalDownloaded = requests.filter { it.responseSize >= 0 }.sumOf { it.responseSize }
    val totalUploaded = requests.filter { it.requestSize >= 0 }.sumOf { it.requestSize }
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
