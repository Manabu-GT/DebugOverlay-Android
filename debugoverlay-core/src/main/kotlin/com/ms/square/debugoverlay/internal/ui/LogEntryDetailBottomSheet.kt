package com.ms.square.debugoverlay.internal.ui

import android.content.ClipData
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ms.square.debugoverlay.core.R
import com.ms.square.debugoverlay.internal.data.model.LogcatEntry
import com.ms.square.debugoverlay.internal.data.model.toColor
import kotlinx.coroutines.launch

private const val BOTTOM_SHEET_HEIGHT_FRACTION = 0.8f
private const val TIMESTAMP_DISPLAY_LENGTH = 12 // HH:MM:SS.mmm

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LogEntryDetailBottomSheet(logEntry: LogcatEntry, onDismiss: () -> Unit, onFilterTag: (String) -> Unit) {
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
    LogEntryDetailContent(
      logEntry = logEntry,
      onDismiss = onDismiss,
      onFilterTag = onFilterTag
    )
  }
}

@Composable
private fun LogEntryDetailContent(
  logEntry: LogcatEntry,
  onDismiss: () -> Unit,
  onFilterTag: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .fillMaxHeight(BOTTOM_SHEET_HEIGHT_FRACTION)
  ) {
    DetailHeader(onDismiss = onDismiss)

    Column(
      modifier = Modifier
        .weight(1f)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp)
    ) {
      DetailMetadataRow(logEntry = logEntry)
      DetailMessageSection(message = logEntry.message)
    }

    DetailActionButtons(
      logEntry = logEntry,
      onFilterTag = onFilterTag
    )
  }
}

@Composable
private fun DetailHeader(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = stringResource(R.string.debugoverlay_log_details),
      style = MaterialTheme.typography.titleMedium,
      modifier = Modifier.weight(1f)
    )
    IconButton(onClick = onDismiss) {
      Icon(
        imageVector = Icons.Default.Close,
        contentDescription = stringResource(R.string.debugoverlay_close_description),
        tint = MaterialTheme.colorScheme.onSurface
      )
    }
  }
}

@Composable
private fun DetailMetadataRow(logEntry: LogcatEntry, modifier: Modifier = Modifier) {
  val levelColor = logEntry.level.toColor()
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 12.dp),
    horizontalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Level
    Column {
      Text(
        text = stringResource(R.string.debugoverlay_level_label),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = levelColor.copy(alpha = 0.2f)
      ) {
        Text(
          text = logEntry.level.name,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = levelColor,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
      }
    }

    // Tag
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = stringResource(R.string.debugoverlay_tag_label),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Text(
        text = logEntry.tag,
        style = MaterialTheme.typography.bodyMedium,
        fontFamily = FontFamily.Monospace,
        color = levelColor,
        fontWeight = FontWeight.SemiBold
      )
    }

    // Time
    Column {
      Text(
        text = stringResource(R.string.debugoverlay_time_label),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Text(
        text = logEntry.timestamp.takeLast(TIMESTAMP_DISPLAY_LENGTH),
        style = MaterialTheme.typography.bodyMedium,
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.onSurface
      )
    }
  }
}

@Composable
private fun DetailMessageSection(message: String, modifier: Modifier = Modifier) {
  Column(modifier = modifier.fillMaxWidth()) {
    Text(
      text = stringResource(R.string.debugoverlay_message_label),
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(bottom = 8.dp)
    )
    Surface(
      shape = RoundedCornerShape(8.dp),
      color = MaterialTheme.colorScheme.surfaceContainer,
      modifier = Modifier.fillMaxWidth()
    ) {
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        modifier = Modifier.padding(12.dp)
      )
    }
  }
}

@Composable
private fun DetailActionButtons(logEntry: LogcatEntry, onFilterTag: (String) -> Unit, modifier: Modifier = Modifier) {
  val clipboard = LocalClipboard.current
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(16.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    // Copy button
    Button(
      onClick = {
        scope.launch {
          val clipboardLabel = context.getString(R.string.debugoverlay_clipboard_label)
          val clipEntry = ClipEntry(ClipData.newPlainText(clipboardLabel, logEntry.rawLine))
          clipboard.setClipEntry(clipEntry)
        }
      },
      modifier = Modifier.weight(1f)
    ) {
      Text(stringResource(R.string.debugoverlay_copy))
    }

    // Filter Tag button
    Button(
      onClick = { onFilterTag(logEntry.tag) },
      modifier = Modifier.weight(1f)
    ) {
      Text(stringResource(R.string.debugoverlay_filter_tag))
    }
  }
}
