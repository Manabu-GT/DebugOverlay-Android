package com.ms.square.debugoverlay.internal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ms.square.debugoverlay.internal.data.model.LogcatEntry

private const val BOTTOM_SHEET_HEIGHT_FRACTION = 0.8f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DebugPanelBottomSheet(logcatEntries: List<LogcatEntry>, onDismiss: () -> Unit) {
  val sheetState = rememberModalBottomSheetState(
    skipPartiallyExpanded = true
  )

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    tonalElevation = 3.dp,
    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    dragHandle = {
      Box(
        modifier = Modifier
          .padding(top = 12.dp, bottom = 8.dp)
          .size(width = 32.dp, height = 4.dp)
          .background(
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            shape = RoundedCornerShape(2.dp)
          )
      )
    }
  ) {
    DebugBottomSheetContent(
      logcatEntries = logcatEntries,
      onDismiss = onDismiss
    )
  }
}

@Composable
private fun DebugBottomSheetContent(
  logcatEntries: List<LogcatEntry>,
  modifier: Modifier = Modifier,
  onDismiss: () -> Unit = {},
) {
  var selectedTabIndex by remember { mutableIntStateOf(0) }
  val tabs = remember { listOf("Logcat") }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .fillMaxHeight(BOTTOM_SHEET_HEIGHT_FRACTION)
  ) {
    // Header with title and close button
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Debug Panel",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.weight(1f)
      )
      IconButton(
        onClick = onDismiss
      ) {
        Icon(
          imageVector = Icons.Default.Close,
          contentDescription = "Close",
          tint = MaterialTheme.colorScheme.onSurface
        )
      }
    }

    // Tabs
    PrimaryTabRow(
      selectedTabIndex = selectedTabIndex,
      modifier = Modifier.fillMaxWidth(),
      containerColor = Color.Transparent
    ) {
      tabs.forEachIndexed { index, title ->
        Tab(
          selected = selectedTabIndex == index,
          onClick = { selectedTabIndex = index },
          text = {
            Text(
              text = title,
              style = MaterialTheme.typography.labelLarge
            )
          }
        )
      }
    }
    // Tab content
    when (selectedTabIndex) {
      0 -> LogcatTabContent(logcatEntries = logcatEntries)
    }
  }
}
