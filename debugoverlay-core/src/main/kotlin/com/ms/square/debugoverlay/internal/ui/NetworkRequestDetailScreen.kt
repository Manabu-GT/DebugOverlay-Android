@file:Suppress("TooManyFunctions")

package com.ms.square.debugoverlay.internal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ms.square.debugoverlay.core.R
import com.ms.square.debugoverlay.internal.data.TextType
import com.ms.square.debugoverlay.internal.data.UrlParts
import com.ms.square.debugoverlay.internal.util.copyToClipboard
import com.ms.square.debugoverlay.internal.util.formatBytes
import com.ms.square.debugoverlay.internal.util.formatTimestamp
import com.ms.square.debugoverlay.internal.util.httpStatusColor
import com.ms.square.debugoverlay.internal.util.httpStatusMessage
import com.ms.square.debugoverlay.model.NetworkRequest

/**
 * Network request detail screen with TopAppBar and comprehensive information.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NetworkRequestDetailScreen(request: NetworkRequest, onBack: () -> Unit, modifier: Modifier = Modifier) {
  val clipboard = LocalClipboard.current
  val scope = rememberCoroutineScope()
  val domain = remember(request.url) { UrlParts.from(request.url).domain }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = {
          Column {
            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              MethodBadge(method = request.method)
              StatusCodeBadge(statusCode = request.statusCode)
            }
            Text(
              text = domain,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              fontFamily = FontFamily.Monospace,
              modifier = Modifier.padding(top = 2.dp)
            )
          }
        },
        navigationIcon = {
          BackButton(onClick = onBack)
        },
        actions = {
          IconButton(onClick = {
            scope.copyToClipboard(clipboard, request.url)
          }) {
            Icon(
              imageVector = Icons.Default.ContentCopy,
              contentDescription = stringResource(R.string.debugoverlay_copy)
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
      )
    }
  ) { paddingValues ->
    NetworkRequestDetailContent(
      request = request,
      modifier = Modifier.padding(paddingValues)
    )
  }
}

@Composable
private fun NetworkRequestDetailContent(request: NetworkRequest, modifier: Modifier = Modifier) {
  var selectedTab by remember { mutableIntStateOf(0) }

  Column(modifier = modifier.fillMaxSize()) {
    // Tabs
    SecondaryTabRow(
      selectedTabIndex = selectedTab,
      containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
      Tab(
        selected = selectedTab == 0,
        onClick = { selectedTab = 0 },
        text = { Text("Overview") }
      )
      Tab(
        selected = selectedTab == 1,
        onClick = { selectedTab = 1 },
        text = { Text("Headers") }
      )
      Tab(
        selected = selectedTab == 2,
        onClick = { selectedTab = 2 },
        text = { Text("Body") }
      )
    }

    // Content
    SelectionContainer(modifier = Modifier.fillMaxSize()) {
      when (selectedTab) {
        0 -> OverviewTab(request = request)
        1 -> HeadersTab(request = request)
        2 -> BodyTab(request = request)
      }
    }
  }
}

/**
 * Overview tab with URL, request info, and response summary.
 */
@Suppress("LongMethod") // Complex UI with multiple sections
@Composable
private fun OverviewTab(request: NetworkRequest, modifier: Modifier = Modifier) {
  val clipboard = LocalClipboard.current
  val scope = rememberCoroutineScope()

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(24.dp)
  ) {
    // Error Section (if applicable)
    if (request.error != null) {
      item {
        ErrorSection(error = request.error)
      }
    }

    // URL Section
    item {
      DetailSection(
        title = "URL",
        onCopy = {
          scope.copyToClipboard(clipboard, request.url)
        }
      ) {
        UrlDisplay(url = request.url)
      }
    }

    // Request Info
    item {
      DetailSection(title = "Request Info") {
        InfoCard {
          InfoRow("Method", request.method)
          InfoRow(
            "Status",
            request.statusCode?.let { "$it ${it.httpStatusMessage}" } ?: "Error",
            valueColor = request.statusCode.httpStatusColor
          )
          InfoRow("Duration", "${request.durationMs} ms")
          InfoRow("Response Size", formatBytes(request.responseSize))
          InfoRow("Request Size", formatBytes(request.requestSize))
          InfoRow("Timestamp", formatTimestamp(request.timestampMs), showDivider = false)
        }
      }
    }

    // Response Summary
    if (request.responseHeaders.isNotEmpty()) {
      item {
        DetailSection(title = "Response Summary") {
          InfoCard {
            var itemCount = 0
            request.responseHeaders["content-type"]?.let {
              InfoRow("Content-Type", it)
              itemCount++
            }
            request.responseHeaders["content-length"]?.let {
              InfoRow("Content-Length", it)
              itemCount++
            }
            request.responseHeaders["cache-control"]?.let {
              InfoRow("Cache", it, showDivider = false)
              itemCount++
            }
            if (itemCount == 0) {
              EmptyState(text = stringResource(R.string.debugoverlay_network_no_notable_headers))
            }
          }
        }
      }
    }
  }
}

/**
 * Headers tab with request and response headers.
 */
@Suppress("LongMethod") // Complex UI with request and response sections
@Composable
private fun HeadersTab(request: NetworkRequest, modifier: Modifier = Modifier) {
  val clipboard = LocalClipboard.current
  val scope = rememberCoroutineScope()
  var requestHeadersExpanded by remember { mutableStateOf(true) }
  var responseHeadersExpanded by remember { mutableStateOf(true) }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(24.dp)
  ) {
    // Request Headers
    item {
      DetailSection(
        title = "Request Headers (${request.requestHeaders.size})",
        isExpandable = true,
        isExpanded = requestHeadersExpanded,
        onToggleExpand = { requestHeadersExpanded = !requestHeadersExpanded },
        onCopy = if (request.requestHeaders.isNotEmpty()) {
          {
            val text = request.requestHeaders.entries.joinToString("\n") {
              "${it.key}: ${it.value}"
            }
            scope.copyToClipboard(clipboard, text)
          }
        } else {
          null
        }
      ) {
        if (requestHeadersExpanded) {
          if (request.requestHeaders.isEmpty()) {
            EmptyState(text = stringResource(R.string.debugoverlay_network_no_request_headers))
          } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              request.requestHeaders.forEach { (name, value) ->
                HeaderItem(name = name, value = value)
              }
            }
          }
        }
      }
    }

    // Response Headers
    item {
      DetailSection(
        title = "Response Headers (${request.responseHeaders.size})",
        isExpandable = true,
        isExpanded = responseHeadersExpanded,
        onToggleExpand = { responseHeadersExpanded = !responseHeadersExpanded },
        onCopy = if (request.responseHeaders.isNotEmpty()) {
          {
            val text = request.responseHeaders.entries.joinToString("\n") {
              "${it.key}: ${it.value}"
            }
            scope.copyToClipboard(clipboard, text)
          }
        } else {
          null
        }
      ) {
        if (responseHeadersExpanded) {
          if (request.responseHeaders.isEmpty()) {
            EmptyState(text = stringResource(R.string.debugoverlay_network_no_response_headers))
          } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              request.responseHeaders.forEach { (name, value) ->
                HeaderItem(name = name, value = value)
              }
            }
          }
        }
      }
    }
  }
}

/**
 * Body tab with request and response bodies.
 */
@Composable
private fun BodyTab(request: NetworkRequest, modifier: Modifier = Modifier) {
  val clipboard = LocalClipboard.current
  val scope = rememberCoroutineScope()

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(24.dp)
  ) {
    // Request Body
    item {
      DetailSection(
        title = "Request Body",
        onCopy = request.requestBody?.let {
          {
            scope.copyToClipboard(clipboard, it)
          }
        }
      ) {
        if (request.requestBody != null) {
          BodyPreview(
            body = request.requestBody,
            contentType = request.requestHeaders["content-type"]
          )
        } else {
          EmptyState(text = stringResource(R.string.debugoverlay_network_no_request_body))
        }
      }
    }

    // Response Body
    item {
      DetailSection(
        title = "Response Body",
        onCopy = request.responseBody?.let {
          {
            scope.copyToClipboard(clipboard, it)
          }
        }
      ) {
        if (request.responseBody != null) {
          BodyPreview(
            body = request.responseBody,
            contentType = request.responseHeaders["content-type"]
          )
        } else {
          EmptyState(text = stringResource(R.string.debugoverlay_network_no_response_body))
        }
      }
    }
  }
}

// ============================================================================
// Helper Composables
// ============================================================================

/**
 * Section with title and optional copy button.
 */
@Composable
private fun DetailSection(
  title: String,
  modifier: Modifier = Modifier,
  isExpandable: Boolean = false,
  isExpanded: Boolean = true,
  onToggleExpand: (() -> Unit)? = null,
  onCopy: (() -> Unit)? = null,
  content: @Composable () -> Unit,
) {
  Column(modifier = modifier) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (isExpandable && onToggleExpand != null) {
          IconButton(
            onClick = onToggleExpand,
            modifier = Modifier.size(24.dp)
          ) {
            Icon(
              imageVector = if (isExpanded) {
                Icons.Default.ExpandMore
              } else {
                Icons.Default.ChevronRight
              },
              contentDescription = if (isExpanded) "Collapse" else "Expand",
              tint = MaterialTheme.colorScheme.primary
            )
          }
        }

        Text(
          text = title,
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Bold
        )
      }

      if (onCopy != null) {
        IconButton(
          onClick = onCopy,
          modifier = Modifier.size(32.dp)
        ) {
          Icon(
            imageVector = Icons.Default.ContentCopy,
            contentDescription = stringResource(R.string.debugoverlay_copy),
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }

    content()
  }
}

/**
 * Info card container.
 */
@Composable
private fun InfoCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.surfaceContainerHighest,
    tonalElevation = 2.dp
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      content = content
    )
  }
}

/**
 * Info row with label and value.
 */
@Composable
private fun InfoRow(
  label: String,
  value: String,
  modifier: Modifier = Modifier,
  valueColor: Color = MaterialTheme.colorScheme.onSurface,
  showDivider: Boolean = true,
) {
  Column(modifier = modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.Top
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Text(
        text = value,
        style = MaterialTheme.typography.bodySmall,
        color = valueColor,
        fontWeight = FontWeight.Medium,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(start = 16.dp)
      )
    }

    if (showDivider) {
      HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
        modifier = Modifier.padding(top = 8.dp)
      )
    }
  }
}

/**
 * URL display with scheme, domain, and path.
 */
@Composable
private fun UrlDisplay(url: String, modifier: Modifier = Modifier) {
  val parts = remember(url) {
    UrlParts.from(url)
  }

  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.surfaceContainerHighest,
    tonalElevation = 2.dp
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        text = parts.scheme,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontFamily = FontFamily.Monospace
      )

      Text(
        text = parts.domain,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Medium,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(vertical = 4.dp)
      )

      Text(
        text = parts.path,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.tertiary,
        fontFamily = FontFamily.Monospace
      )
    }
  }
}

/**
 * Header item with name and value.
 */
@Composable
private fun HeaderItem(name: String, value: String, modifier: Modifier = Modifier) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.small,
    color = MaterialTheme.colorScheme.surfaceContainerHighest
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Text(
        text = name,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace
      )

      Text(
        text = value,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(top = 4.dp)
      )
    }
  }
}

/**
 * Body preview.
 */
@Composable
private fun BodyPreview(body: String, contentType: String?) {
  val textType = remember(body, contentType) { TextType.from(body, contentType) }
  TextPreview(body, textType)
}

/**
 * Error section display.
 */
@Composable
private fun ErrorSection(error: com.ms.square.debugoverlay.model.NetworkError, modifier: Modifier = Modifier) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
    tonalElevation = 2.dp
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.Error,
          contentDescription = "Error",
          tint = MaterialTheme.colorScheme.error
        )
        Text(
          text = error.title,
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.error,
          fontWeight = FontWeight.Bold
        )
      }

      Text(
        text = error.message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 8.dp)
      )

      if (error.stackTrace != null) {
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
          shape = MaterialTheme.shapes.small,
          color = MaterialTheme.colorScheme.surfaceContainerLowest
        ) {
          // No verticalScroll - LazyColumn parent handles vertical scrolling
          Text(
            text = error.stackTrace,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace
          )
        }
      }
    }
  }
}

/**
 * Empty state.
 */
@Composable
private fun EmptyState(text: String, modifier: Modifier = Modifier) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .padding(40.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      Icon(
        imageVector = Icons.Default.Info,
        contentDescription = null,
        modifier = Modifier.size(48.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
      )
      Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
      )
    }
  }
}
