package com.ms.square.debugoverlay.internal.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Badge
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import com.ms.square.debugoverlay.core.R
import com.ms.square.debugoverlay.internal.util.toColor
import com.ms.square.debugoverlay.model.LogLevel

/**
 * Filter bar for [LogTabContent]: a search field plus log-level filters.
 *
 * On short-height windows ([isCompactHeight]), collapses to [CompactLogFilterRow] to reclaim
 * vertical space for the log list; otherwise renders the always-visible search field and
 * filter-chip row.
 */
@Composable
internal fun LogFilterBar(
  searchQuery: String,
  onSearchQueryChanged: (String) -> Unit,
  selectedLevel: LogLevel,
  onLevelSelected: (LogLevel) -> Unit,
  isCompactHeight: Boolean,
  modifier: Modifier = Modifier,
) {
  if (isCompactHeight) {
    CompactLogFilterRow(
      searchQuery = searchQuery,
      onSearchQueryChanged = onSearchQueryChanged,
      selectedLevel = selectedLevel,
      onLevelSelected = onLevelSelected,
      modifier = modifier
        .fillMaxWidth()
        .padding(top = 8.dp)
    )
  } else {
    Column(modifier = modifier.fillMaxWidth().padding(top = 8.dp)) {
      SearchField(
        searchPlaceholder = stringResource(R.string.debugoverlay_search_logs),
        searchQuery = searchQuery,
        onSearchQueryChanged = onSearchQueryChanged,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp)
      )

      LogLevelFilters(
        selectedLevel = selectedLevel,
        onLevelSelected = onLevelSelected
      )
    }
  }
}

/**
 * Compact-height variant of [LogFilterBar]: the search field and level filters share a single
 * row (filters collapse into a dropdown menu) instead of stacking on two — reclaiming the
 * vertical space [LogFilterBar]'s two-row layout would otherwise cost on short-height windows
 * (e.g. landscape on a small device). The search field itself stays full width since landscape
 * width isn't the constrained dimension here — only merging the rows saves height.
 */
@Composable
private fun CompactLogFilterRow(
  searchQuery: String,
  onSearchQueryChanged: (String) -> Unit,
  selectedLevel: LogLevel,
  onLevelSelected: (LogLevel) -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.padding(horizontal = 8.dp, vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    SearchField(
      searchPlaceholder = stringResource(R.string.debugoverlay_search_logs),
      searchQuery = searchQuery,
      onSearchQueryChanged = onSearchQueryChanged,
      modifier = Modifier.weight(1f)
    )

    LogLevelFilterMenu(
      selectedLevel = selectedLevel,
      onLevelSelected = onLevelSelected
    )
  }
}

@Composable
private fun LogLevelFilterMenu(
  selectedLevel: LogLevel,
  onLevelSelected: (LogLevel) -> Unit,
  modifier: Modifier = Modifier,
) {
  var isMenuExpanded by remember { mutableStateOf(false) }
  val filterDescription = stringResource(
    R.string.debugoverlay_filter_logs_content_description,
    selectedLevel.name
  )

  Box(modifier = modifier) {
    IconButton(
      onClick = { isMenuExpanded = true },
      modifier = Modifier.semantics { contentDescription = filterDescription }
    ) {
      Icon(imageVector = Icons.Default.FilterList, contentDescription = null)
    }
    // Dot in the selected level's color, matching the dropdown items' color coding, so the
    // active level is glanceable without opening the menu.
    Badge(
      modifier = Modifier.align(Alignment.TopEnd),
      containerColor = selectedLevel.toColor()
    )
    DropdownMenu(
      expanded = isMenuExpanded,
      onDismissRequest = { isMenuExpanded = false }
    ) {
      LogLevel.entries.forEach { level ->
        val levelDescription = stringResource(R.string.debugoverlay_filter_chip_description, level.name)
        DropdownMenuItem(
          text = { Text(level.name) },
          leadingIcon = {
            Box(
              modifier = Modifier
                .size(10.dp)
                .background(color = level.toColor(), shape = CircleShape)
            )
          },
          trailingIcon = {
            if (level == selectedLevel) {
              Icon(imageVector = Icons.Default.Check, contentDescription = null)
            }
          },
          modifier = Modifier.semantics {
            role = Role.RadioButton
            contentDescription = levelDescription
            selected = level == selectedLevel
          },
          onClick = {
            onLevelSelected(level)
            isMenuExpanded = false
          }
        )
      }
    }
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
    border = BorderStroke(
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
