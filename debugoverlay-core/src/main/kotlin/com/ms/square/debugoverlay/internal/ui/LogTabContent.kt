package com.ms.square.debugoverlay.internal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ms.square.debugoverlay.core.R
import com.ms.square.debugoverlay.internal.util.formatTimestamp
import com.ms.square.debugoverlay.internal.util.toColor
import com.ms.square.debugoverlay.model.LogEntry
import com.ms.square.debugoverlay.model.LogLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Log tab content displaying filtered log entries with auto-scroll behavior.
 *
 * Features:
 * - Filter by log level (V, D, I, W, E)
 * - Auto-scroll to bottom for new entries
 * - Manual scroll pauses auto-scroll
 * - Detail screen navigation with back button
 * - FAB to resume auto-scroll when paused
 * - Source indicator showing "System Logcat" or custom source name (e.g., "Timber")
 *
 * @param logsFlow Flow of log entries to collect and display.
 * @param logSourceNameFlow Flow of current log source name (null = system logcat).
 * @param modifier Modifier to be applied to the root layout
 */
@Composable
internal fun LogTabContent(
  logsFlow: Flow<List<LogEntry>>,
  logSourceNameFlow: StateFlow<String?>,
  modifier: Modifier = Modifier,
) {
  val logEntries by logsFlow.collectAsStateWithLifecycle(emptyList())
  val logSourceName by logSourceNameFlow.collectAsStateWithLifecycle()

  var selectedLevel by remember { mutableStateOf(LogLevel.DEBUG) }
  var searchQuery by remember { mutableStateOf("") }
  var selectedLogEntry by remember { mutableStateOf<LogEntry?>(null) }
  var isAutoScrollEnabled by remember { mutableStateOf(true) }

  val filteredEntries = remember(logEntries, selectedLevel, searchQuery) {
    logEntries.filter { entry ->
      val levelMatch = entry.level.ordinal >= selectedLevel.ordinal
      val searchMatch = searchQuery.isBlank() ||
        entry.message.contains(searchQuery, ignoreCase = true) ||
        entry.tag.contains(searchQuery, ignoreCase = true)
      levelMatch && searchMatch
    }
  }

  val listState = rememberLazyListState()

  // How this works:
  // - User scrolls away from bottom → disable auto-scroll
  // - User scrolls back to bottom → re-enable auto-scroll
  // - FAB click → re-enable auto-scroll
  // - New entries + auto-scroll enabled → scroll to bottom
  AutoScrollManager(
    listState = listState,
    filteredEntries = filteredEntries,
    isAutoScrollEnabled = isAutoScrollEnabled,
    enableAutoScroll = { isAutoScrollEnabled = true },
    disableAutoScroll = { isAutoScrollEnabled = false }
  )

  // State-based navigation with shared DetailNavigation
  DetailNavigation(
    selectedItem = selectedLogEntry,
    onBack = { selectedLogEntry = null },
    listContent = {
      LogListScreen(
        logSourceName = logSourceName,
        searchQuery = searchQuery,
        onSearchQueryChanged = { searchQuery = it },
        selectedLevel = selectedLevel,
        onLevelSelected = { selectedLevel = it },
        filteredEntries = filteredEntries,
        listState = listState,
        onEntryClick = { selectedLogEntry = it },
        onFabClick = { isAutoScrollEnabled = true }
      )
    },
    detailContent = { entry ->
      LogEntryDetailScreen(
        logEntry = entry,
        onBack = { selectedLogEntry = null },
        onFilterTag = { tag ->
          searchQuery = tag
          selectedLogEntry = null
        }
      )
    },
    modifier = modifier
  )
}

/**
 * Log list screen with filters and log entries.
 */
@Suppress("LongParameterList")
@Composable
private fun LogListScreen(
  logSourceName: String?,
  searchQuery: String,
  onSearchQueryChanged: (String) -> Unit,
  selectedLevel: LogLevel,
  onLevelSelected: (LogLevel) -> Unit,
  filteredEntries: List<LogEntry>,
  listState: LazyListState,
  onEntryClick: (LogEntry) -> Unit,
  onFabClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier.fillMaxWidth()) {
    Column(modifier = Modifier.fillMaxWidth()) {
      LogFilterBar(
        logSourceName = logSourceName,
        searchQuery = searchQuery,
        onSearchQueryChanged = onSearchQueryChanged,
        selectedLevel = selectedLevel,
        onLevelSelected = onLevelSelected
      )

      LogContent(
        filteredEntries = filteredEntries,
        listState = listState,
        onEntryClick = onEntryClick
      )
    }

    ScrollToBottomFab(
      visible = listState.canScrollForward,
      onClick = onFabClick,
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(16.dp)
    )
  }
}

@Composable
private fun LogFilterBar(
  logSourceName: String?,
  searchQuery: String,
  onSearchQueryChanged: (String) -> Unit,
  selectedLevel: LogLevel,
  onLevelSelected: (LogLevel) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxWidth().padding(top = 8.dp)) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      LogSourceIndicator(sourceName = logSourceName)
      SearchField(
        searchPlaceholder = stringResource(R.string.debugoverlay_search_logs),
        searchQuery = searchQuery,
        onSearchQueryChanged = onSearchQueryChanged,
        modifier = Modifier.weight(1f)
      )
    }

    LogLevelFilters(
      selectedLevel = selectedLevel,
      onLevelSelected = onLevelSelected
    )
  }
}

/**
 * Badge showing the current log source (e.g., "System Logcat" or "Timber").
 */
@Composable
private fun LogSourceIndicator(sourceName: String?, modifier: Modifier = Modifier) {
  val displayName = sourceName ?: stringResource(R.string.debugoverlay_log_source_system)
  val sourceDescription = stringResource(R.string.debugoverlay_log_source_description, displayName)

  Surface(
    modifier = modifier.semantics {
      contentDescription = sourceDescription
    },
    shape = MaterialTheme.shapes.extraSmall,
    color = MaterialTheme.colorScheme.tertiaryContainer
  ) {
    Text(
      text = displayName,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onTertiaryContainer,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    )
  }
}

@Composable
private fun LogLevelFilters(
  selectedLevel: LogLevel,
  onLevelSelected: (LogLevel) -> Unit,
  modifier: Modifier = Modifier,
) {
  FlowRow(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 8.dp, vertical = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    LogLevel.entries.forEach { level ->
      FilterChip(
        label = level.name,
        color = level.toColor(),
        selected = selectedLevel == level,
        onClick = { onLevelSelected(level) }
      )
    }
  }
}

@Composable
private fun LogContent(
  filteredEntries: List<LogEntry>,
  listState: androidx.compose.foundation.lazy.LazyListState,
  onEntryClick: (LogEntry) -> Unit,
) {
  if (filteredEntries.isEmpty()) {
    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      NoLogEntryPlaceHolder()
    }
  } else {
    LazyColumn(
      state = listState,
      verticalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
      items(filteredEntries, key = { it.id }) { entry ->
        LogEntryItem(
          entry = entry,
          onClick = { onEntryClick(entry) }
        )
      }
    }
  }
}

@Composable
private fun NoLogEntryPlaceHolder(modifier: Modifier = Modifier) {
  Text(
    text = stringResource(R.string.debugoverlay_no_log_entries),
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = modifier
  )
}

@Composable
private fun LogEntryItem(entry: LogEntry, onClick: () -> Unit, modifier: Modifier = Modifier) {
  val entryDescription = stringResource(
    R.string.debugoverlay_log_entry_description,
    entry.tag,
    entry.message
  )
  val viewDetailsLabel = stringResource(R.string.debugoverlay_view_details)

  Row(
    modifier = modifier
      .fillMaxWidth()
      .background(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.small
      )
      .height(IntrinsicSize.Min) // Allow children to query intrinsic height
      .semantics(mergeDescendants = true) {
        contentDescription = entryDescription
        role = Role.Button
      }
      .clickable(onClickLabel = viewDetailsLabel) {
        onClick()
      }
      .padding(vertical = 4.dp, horizontal = 8.dp), // Add horizontal padding too
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    // Vertical color bar indicator
    Box(
      modifier = Modifier
        .width(3.dp)
        .fillMaxHeight() // Dynamically matches the Row's height
        .background(
          color = entry.level.toColor(),
          shape = RoundedCornerShape(2.dp)
        )
    )
    // Content (timestamp + thread + tag + message)
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
      LogEntryMetadata(entry = entry)

      // Message
      Text(
        text = entry.message,
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
private fun LogEntryMetadata(entry: LogEntry, modifier: Modifier = Modifier) {
  Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Timestamp
    Text(
      text = formatTimestamp(entry.timestampMs),
      style = MaterialTheme.typography.labelSmall,
      fontFamily = FontFamily.Monospace,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    // Bullet separator
    Text(
      text = "•",
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    )

    // Thread name
    Text(
      text = entry.threadName,
      style = MaterialTheme.typography.labelSmall,
      fontFamily = FontFamily.Monospace,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )

    // Bullet separator
    Text(
      text = "•",
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    )

    // Tag in colored chip
    Surface(
      shape = MaterialTheme.shapes.extraSmall,
      color = entry.level.toColor().copy(alpha = 0.2f),
      contentColor = entry.level.toColor()
    ) {
      Text(
        text = entry.tag,
        style = MaterialTheme.typography.labelSmall,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
private fun FilterChip(
  label: String,
  color: Color,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val chipDescription = stringResource(R.string.debugoverlay_filter_chip_description, label)
  Surface(
    onClick = onClick,
    modifier = modifier.semantics {
      role = Role.RadioButton
      contentDescription = chipDescription
      this.selected = selected
    },
    shape = MaterialTheme.shapes.large,
    color = if (selected) color.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceContainerHigh,
    border = androidx.compose.foundation.BorderStroke(
      width = 1.dp,
      color = if (selected) color else Color.Transparent
    )
  ) {
    Text(
      text = label,
      modifier = Modifier.padding(6.dp),
      style = MaterialTheme.typography.labelSmall,
      color = if (selected) color else MaterialTheme.colorScheme.onSurface
    )
  }
}
