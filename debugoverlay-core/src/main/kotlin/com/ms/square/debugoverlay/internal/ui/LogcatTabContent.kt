package com.ms.square.debugoverlay.internal.ui

import android.content.ClipData
import android.os.Process
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.toLowerCase
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ms.square.debugoverlay.core.R
import com.ms.square.debugoverlay.internal.data.model.LogLevel
import com.ms.square.debugoverlay.internal.data.model.LogcatEntry
import kotlinx.coroutines.launch
import java.util.Locale

// Constants
private const val TIMESTAMP_DISPLAY_LENGTH = 12 // HH:MM:SS.mmm

/**
 * Logcat tab content displaying filtered log entries with auto-scroll behavior.
 *
 * Features:
 * - Filter by log level (V, D, I, W, E)
 * - Toggle between all logs and current app logs
 * - Auto-scroll to bottom for new entries
 * - Manual scroll pauses auto-scroll
 * - Tap log entry to copy to clipboard
 * - FAB to resume auto-scroll when paused
 *
 * @param logcatEntries List of all available log entries
 * @param modifier Modifier to be applied to the root layout
 */
@Composable
internal fun LogcatTabContent(logcatEntries: List<LogcatEntry>, modifier: Modifier = Modifier) {
  var selectedLevel by remember { mutableStateOf(LogLevel.DEBUG) }
  var showOnlyMyApp by remember { mutableStateOf(true) }
  var isPaused by remember { mutableStateOf(false) }
  var isProgrammaticScroll by remember { mutableStateOf(false) }
  var searchQuery by remember { mutableStateOf("") }

  val myPid = remember { Process.myPid() }

  val filteredEntries = remember(logcatEntries, selectedLevel, showOnlyMyApp, searchQuery) {
    logcatEntries.filter { entry ->
      val levelMatch = entry.level.ordinal >= selectedLevel.ordinal
      val pidMatch = !showOnlyMyApp || entry.pid == myPid
      val searchMatch = searchQuery.isBlank() ||
        entry.message.contains(searchQuery, ignoreCase = true) ||
        entry.tag.contains(searchQuery, ignoreCase = true)
      levelMatch && pidMatch && searchMatch
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

  Box(modifier = modifier.fillMaxWidth()) {
    Column(modifier = Modifier.fillMaxWidth()) {
      SearchField(
        searchQuery = searchQuery,
        onSearchQueryChanged = { searchQuery = it }
      )

      LogcatFilters(
        selectedLevel = selectedLevel,
        onLevelSelected = { selectedLevel = it },
        showOnlyMyApp = showOnlyMyApp,
        onShowOnlyMyAppChanged = { showOnlyMyApp = it }
      )

      LogcatContent(
        filteredEntries = filteredEntries,
        listState = listState
      )
    }

    ResumeScrollFab(
      visible = isPaused,
      onResume = { isPaused = false },
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(16.dp)
    )
  }
}

@Composable
private fun LogcatFilters(
  selectedLevel: LogLevel,
  onLevelSelected: (LogLevel) -> Unit,
  showOnlyMyApp: Boolean,
  onShowOnlyMyAppChanged: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  val switchContentDescription = stringResource(R.string.debugoverlay_filter_my_app_description)

  FlowRow(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 4.dp, vertical = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    LogLevel.entries.forEach { level ->
      FilterChip(
        label = level.name,
        color = level.toColor(),
        selected = selectedLevel == level,
        onClick = { onLevelSelected(level) }
      )
    }

    Row(
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(start = 4.dp)
    ) {
      Text(
        text = stringResource(R.string.debugoverlay_my_app_filter),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface
      )
      Switch(
        checked = showOnlyMyApp,
        onCheckedChange = onShowOnlyMyAppChanged,
        modifier = Modifier.semantics {
          contentDescription = switchContentDescription
        },
        colors = SwitchDefaults.colors(
          checkedThumbColor = Color.White,
          checkedTrackColor = MaterialTheme.colorScheme.primary
        )
      )
    }
  }
}

@Composable
private fun LogcatContent(
  filteredEntries: List<LogcatEntry>,
  listState: androidx.compose.foundation.lazy.LazyListState,
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
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp)
    ) {
      items(filteredEntries, key = { it.id }) { entry ->
        LogcatEntryItem(entry = entry)
      }
    }
  }
}

@Composable
private fun ResumeScrollFab(visible: Boolean, onResume: () -> Unit, modifier: Modifier = Modifier) {
  AnimatedVisibility(
    visible = visible,
    modifier = modifier,
    enter = fadeIn() + scaleIn(),
    exit = fadeOut() + scaleOut()
  ) {
    FloatingActionButton(
      onClick = onResume,
      containerColor = MaterialTheme.colorScheme.primaryContainer
    ) {
      Icon(
        imageVector = Icons.Default.ArrowDownward,
        contentDescription = stringResource(R.string.debugoverlay_scroll_to_bottom)
      )
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
private fun LogcatEntryItem(entry: LogcatEntry, modifier: Modifier = Modifier) {
  val resource = LocalResources.current
  val clipboard = LocalClipboard.current
  val coroutineScope = rememberCoroutineScope()
  val entryDescription = stringResource(
    R.string.debugoverlay_log_entry_description,
    entry.tag,
    entry.message
  )
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
      .clickable(onClickLabel = stringResource(R.string.debugoverlay_copy_to_clipboard)) {
        coroutineScope.launch {
          // Copy full log line to clipboard
          val clipboardLabel = resource.getString(R.string.debugoverlay_clipboard_label)
          val clipEntry = ClipEntry(ClipData.newPlainText(clipboardLabel, entry.rawLine))
          clipboard.setClipEntry(clipEntry)
        }
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
        fontSize = 11.sp,
        lineHeight = 14.sp,
        maxLines = 4,
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
      style = MaterialTheme.typography.bodySmall,
      fontFamily = FontFamily.Monospace,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      fontSize = 10.sp
    )

    // Bullet separator
    Text(
      text = "•",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
      fontSize = 10.sp
    )

    // Thread name
    Text(
      text = entry.threadName,
      style = MaterialTheme.typography.bodySmall,
      fontFamily = FontFamily.Monospace,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      fontSize = 10.sp,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )

    // Bullet separator
    Text(
      text = "•",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
      fontSize = 10.sp
    )

    // Tag in colored chip
    Surface(
      shape = RoundedCornerShape(4.dp),
      color = entry.level.toColor().copy(alpha = 0.2f),
      contentColor = entry.level.toColor()
    ) {
      Text(
        text = entry.tag,
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
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

// Log level colors for light theme
private val VERBOSE_COLOR_LIGHT = Color(0xFF757575)
private val DEBUG_COLOR_LIGHT = Color(0xFF2196F3)
private val INFO_COLOR_LIGHT = Color(0xFF4CAF50)
private val WARN_COLOR_LIGHT = Color(0xFFFF9800)
private val ERROR_COLOR_LIGHT = Color(0xFFF44336)

// Log level colors for dark theme
private val VERBOSE_COLOR_DARK = Color(0xFFBDBDBD)
private val DEBUG_COLOR_DARK = Color(0xFF64B5F6)
private val INFO_COLOR_DARK = Color(0xFF81C784)
private val WARN_COLOR_DARK = Color(0xFFFFB74D)
private val ERROR_COLOR_DARK = Color(0xFFE57373)

@Composable
private fun LogLevel.toColor(): Color {
  val isDark = isSystemInDarkTheme()
  return when (this) {
    LogLevel.VERBOSE -> if (isDark) VERBOSE_COLOR_DARK else VERBOSE_COLOR_LIGHT
    LogLevel.DEBUG -> if (isDark) DEBUG_COLOR_DARK else DEBUG_COLOR_LIGHT
    LogLevel.INFO -> if (isDark) INFO_COLOR_DARK else INFO_COLOR_LIGHT
    LogLevel.WARN -> if (isDark) WARN_COLOR_DARK else WARN_COLOR_LIGHT
    LogLevel.ERROR -> if (isDark) ERROR_COLOR_DARK else ERROR_COLOR_LIGHT
  }
}

@Composable
private fun SearchField(searchQuery: String, onSearchQueryChanged: (String) -> Unit, modifier: Modifier = Modifier) {
  OutlinedTextField(
    value = searchQuery,
    onValueChange = onSearchQueryChanged,
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 8.dp),
    placeholder = {
      Text(
        text = stringResource(R.string.debugoverlay_search_logs),
        style = MaterialTheme.typography.bodyMedium
      )
    },
    leadingIcon = {
      Icon(
        imageVector = Icons.Default.Search,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant
      )
    },
    trailingIcon = {
      if (searchQuery.isNotEmpty()) {
        IconButton(onClick = { onSearchQueryChanged("") }) {
          Icon(
            imageVector = Icons.Default.Clear,
            contentDescription = stringResource(R.string.debugoverlay_clear_search),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    },
    singleLine = true,
    shape = RoundedCornerShape(12.dp),
    colors = TextFieldDefaults.colors(
      focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
      unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
      focusedIndicatorColor = Color.Transparent,
      unfocusedIndicatorColor = Color.Transparent
    )
  )
}
