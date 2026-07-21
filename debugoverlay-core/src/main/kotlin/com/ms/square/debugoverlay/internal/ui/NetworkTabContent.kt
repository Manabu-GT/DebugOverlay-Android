package com.ms.square.debugoverlay.internal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ms.square.debugoverlay.core.R
import com.ms.square.debugoverlay.internal.data.UrlParts
import com.ms.square.debugoverlay.internal.data.model.NetworkStats
import com.ms.square.debugoverlay.internal.util.HTTP_CLIENT_ERROR_START
import com.ms.square.debugoverlay.internal.util.formatBytes
import com.ms.square.debugoverlay.internal.util.formatRelativeTime
import com.ms.square.debugoverlay.model.NetworkRequest
import kotlinx.coroutines.flow.Flow
import kotlin.math.roundToInt

/**
 * Network tab showing HTTP requests with stats.
 *
 * Features:
 * - Download/Upload stats
 * - Request list (newest first)
 * - Search/filter by URL or method
 * - Detail screen navigation with back button
 *
 * @param netStatsFlow Flow of network statistics to collect and display.
 * @param networkRequestsFlow Flow of network requests to collect and display.
 * @param modifier Modifier to be applied to the root layout.
 * @param isCompactHeight Whether the window has limited vertical space (e.g. landscape on a
 *   small device), condensing the stats header to a single-line summary to reclaim room for
 *   the request list, while keeping it pinned — see [NetworkStatsHeader].
 */
@Composable
internal fun NetworkTabContent(
  netStatsFlow: Flow<NetworkStats>,
  networkRequestsFlow: Flow<List<NetworkRequest>>,
  modifier: Modifier = Modifier,
  isCompactHeight: Boolean = false,
) {
  val networkStats by netStatsFlow.collectAsStateWithLifecycle(
    initialValue = NetworkStats.INITIAL_VALUE
  )
  val networkRequests by networkRequestsFlow.collectAsStateWithLifecycle(
    initialValue = emptyList()
  )
  var searchQuery by remember { mutableStateOf("") }
  var selectedRequest by remember { mutableStateOf<NetworkRequest?>(null) }

  val augmentedNetworkStats = remember(networkStats, networkRequests) {
    networkStats.augmentNetworkStatsWith(networkRequests)
  }

  // Filter requests (newest first)
  val filteredRequests = remember(networkRequests, searchQuery) {
    val filtered = if (searchQuery.isEmpty()) {
      networkRequests
    } else {
      networkRequests.filter { request ->
        request.url.contains(searchQuery, ignoreCase = true) ||
          request.method.contains(searchQuery, ignoreCase = true)
      }
    }
    filtered.asReversed()
  }

  // State-based navigation with shared DetailNavigation
  DetailNavigation(
    selectedItem = selectedRequest,
    onBack = { selectedRequest = null },
    listContent = {
      NetworkListScreen(
        augmentedNetworkStats = augmentedNetworkStats,
        filteredRequests = filteredRequests,
        searchQuery = searchQuery,
        onSearchQueryChanged = { searchQuery = it },
        onRequestClick = { selectedRequest = it },
        isCompactHeight = isCompactHeight
      )
    },
    detailContent = { request ->
      NetworkRequestDetailScreen(
        request = request,
        onBack = { selectedRequest = null }
      )
    },
    modifier = modifier
  )
}

/**
 * Network list screen with stats header and request list.
 *
 * The stats header stays pinned above the [LazyColumn] (not folded into scroll) since aggregate
 * stats — especially the error count — are ambient signal worth keeping glanceable while
 * scrolling the request list, not a control the user reaches for and tucks away. See
 * [NetworkStatsHeader] for how it condenses on short-height windows instead.
 */
@Composable
private fun NetworkListScreen(
  augmentedNetworkStats: NetworkStats,
  filteredRequests: List<NetworkRequest>,
  searchQuery: String,
  onSearchQueryChanged: (String) -> Unit,
  onRequestClick: (NetworkRequest) -> Unit,
  isCompactHeight: Boolean,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxSize()) {
    // Stats Header (only shown when supported)
    if (augmentedNetworkStats != NetworkStats.UNSUPPORTED) {
      NetworkStatsHeader(augmentedNetworkStats, isCompactHeight = isCompactHeight)
    }

    SearchField(
      searchPlaceholder = stringResource(R.string.debugoverlay_search_requests),
      searchQuery = searchQuery,
      onSearchQueryChanged = onSearchQueryChanged
    )

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
          onClick = { onRequestClick(request) }
        )
      }
    }
  }
}

/**
 * Individual network request item.
 */
@Composable
private fun NetworkRequestItem(request: NetworkRequest, onClick: () -> Unit, modifier: Modifier = Modifier) {
  val urlParts = remember(request.url) { UrlParts.from(request.url) }
  Surface(
    onClick = onClick,
    modifier = modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.small,
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
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        // Method badge + URL path
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          MethodBadge(method = request.method)

          Text(
            text = urlParts.pathWithQuery,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        NetworkRequestMetadata(
          domain = urlParts.domain,
          timestampMs = request.timestampMs,
          durationMs = request.durationMs,
          responseSize = request.responseSize
        )
      }

      // Right side: Status code
      StatusCodeBadge(statusCode = request.statusCode)
    }
  }
}

@Composable
private fun NetworkRequestMetadata(
  domain: String,
  timestampMs: Long,
  durationMs: Long,
  responseSize: Long?,
  modifier: Modifier = Modifier,
) {
  val formattedTime = remember(timestampMs) { formatRelativeTime(timestampMs) }
  val formattedBytes = remember(responseSize) { formatBytes(responseSize) }

  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(2.dp)
  ) {
    // Domain
    Text(
      text = domain,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      fontFamily = FontFamily.Monospace,
      maxLines = 1
    )

    // Relative time + Duration + Size
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Text(
        text = formattedTime,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontFamily = FontFamily.Monospace
      )

      Dot()

      Text(
        text = "${durationMs}ms",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontFamily = FontFamily.Monospace
      )

      Dot()

      Text(
        text = formattedBytes,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontFamily = FontFamily.Monospace
      )
    }
  }
}

@Composable
internal fun Dot() {
  Text(
    text = "•",
    style = MaterialTheme.typography.bodySmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
  )
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
