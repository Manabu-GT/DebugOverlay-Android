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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ms.square.debugoverlay.core.R
import com.ms.square.debugoverlay.internal.data.model.LogLevel
import com.ms.square.debugoverlay.internal.data.model.LogcatEntry
import com.ms.square.debugoverlay.internal.data.model.toColor
import kotlinx.coroutines.flow.Flow

// Constants
private const val TIMESTAMP_DISPLAY_LENGTH = 12 // HH:MM:SS.mmm

/**
 * Logcat tab content displaying filtered log entries with auto-scroll behavior.
 *
 * Features:
 * - Filter by log level (V, D, I, W, E)
 * - Auto-scroll to bottom for new entries
 * - Manual scroll pauses auto-scroll
 * - Detail screen navigation with back button
 * - FAB to resume auto-scroll when paused
 *
 * @param logsFlow Flow of logcat entries to collect and display.
 * @param modifier Modifier to be applied to the root layout
 */
@Composable
internal fun LogcatTabContent(logsFlow: Flow<List<LogcatEntry>>, modifier: Modifier = Modifier) {
  val logcatEntries by logsFlow.collectAsStateWithLifecycle(emptyList())

  var selectedLevel by remember { mutableStateOf(LogLevel.DEBUG) }
  var isPaused by remember { mutableStateOf(false) }
  var isProgrammaticScroll by remember { mutableStateOf(false) }
  var searchQuery by remember { mutableStateOf("") }
  var selectedLogEntry by remember { mutableStateOf<LogcatEntry?>(null) }

  val filteredEntries = remember(logcatEntries, selectedLevel, searchQuery) {
    logcatEntries.filter { entry ->
      val levelMatch = entry.level.ordinal >= selectedLevel.ordinal
      val searchMatch = searchQuery.isBlank() ||
        entry.message.contains(searchQuery, ignoreCase = true) ||
        entry.tag.contains(searchQuery, ignoreCase = true)
      levelMatch && searchMatch
    }
  }

  val listState = rememberLazyListState()

  AutoScrollManager(
    listState = listState,
    filteredEntries = filteredEntries,
    isProgrammaticScroll = isProgrammaticScroll,
    isPaused = isPaused,
    onProgrammaticScrollChanged = { isProgrammaticScroll = it },
    onPauseChanged = { isPaused = it }
  )

  // State-based navigation with shared DetailNavigation
  DetailNavigation(
    selectedItem = selectedLogEntry,
    onBack = { selectedLogEntry = null },
    listContent = {
      LogcatListScreen(
        searchQuery = searchQuery,
        onSearchQueryChanged = { searchQuery = it },
        selectedLevel = selectedLevel,
        onLevelSelected = { selectedLevel = it },
        filteredEntries = filteredEntries,
        listState = listState,
        onEntryClick = { selectedLogEntry = it },
        isPaused = isPaused,
        onResume = { isPaused = false }
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
 * Logcat list screen with filters and log entries.
 */
@Suppress("LongParameterList")
@Composable
private fun LogcatListScreen(
  searchQuery: String,
  onSearchQueryChanged: (String) -> Unit,
  selectedLevel: LogLevel,
  onLevelSelected: (LogLevel) -> Unit,
  filteredEntries: List<LogcatEntry>,
  listState: LazyListState,
  onEntryClick: (LogcatEntry) -> Unit,
  isPaused: Boolean,
  onResume: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier.fillMaxWidth()) {
    Column(modifier = Modifier.fillMaxWidth()) {
      LogcatFilterBar(
        searchQuery = searchQuery,
        onSearchQueryChanged = onSearchQueryChanged,
        selectedLevel = selectedLevel,
        onLevelSelected = onLevelSelected
      )

      LogcatContent(
        filteredEntries = filteredEntries,
        listState = listState,
        onEntryClick = onEntryClick
      )
    }

    ResumeScrollFab(
      visible = isPaused,
      onResume = onResume,
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(16.dp)
    )
  }
}

@Composable
private fun LogcatFilterBar(
  searchQuery: String,
  onSearchQueryChanged: (String) -> Unit,
  selectedLevel: LogLevel,
  onLevelSelected: (LogLevel) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxWidth().padding(top = 8.dp)) {
    SearchField(
      searchPlaceholder = stringResource(R.string.debugoverlay_search_logs),
      searchQuery = searchQuery,
      onSearchQueryChanged = onSearchQueryChanged
    )

    LogcatFilters(
      selectedLevel = selectedLevel,
      onLevelSelected = onLevelSelected
    )
  }
}

@Composable
private fun LogcatFilters(
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
private fun LogcatContent(
  filteredEntries: List<LogcatEntry>,
  listState: androidx.compose.foundation.lazy.LazyListState,
  onEntryClick: (LogcatEntry) -> Unit,
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
        LogcatEntryItem(
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
private fun LogcatEntryItem(entry: LogcatEntry, onClick: () -> Unit, modifier: Modifier = Modifier) {
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
        shape = RoundedCornerShape(8.dp)
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
      LogcatEntryMetadata(entry = entry)

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
private fun LogcatEntryMetadata(entry: LogcatEntry, modifier: Modifier = Modifier) {
  Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Timestamp
    Text(
      text = entry.timestamp.takeLast(TIMESTAMP_DISPLAY_LENGTH),
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
      shape = RoundedCornerShape(4.dp),
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
    },
    shape = RoundedCornerShape(16.dp),
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
