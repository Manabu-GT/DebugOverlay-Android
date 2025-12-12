package com.ms.square.debugoverlay.internal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ms.square.debugoverlay.core.R
import com.ms.square.debugoverlay.internal.data.model.LogcatEntry
import com.ms.square.debugoverlay.internal.util.copyToClipboard
import com.ms.square.debugoverlay.internal.util.toColor

private const val TIMESTAMP_DISPLAY_LENGTH = 12 // HH:MM:SS.mmm

/**
 * Log entry detail screen with TopAppBar and comprehensive information.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LogEntryDetailScreen(
  logEntry: LogcatEntry,
  onBack: () -> Unit,
  onFilterTag: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val levelColor = logEntry.level.toColor()

  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = {
          Column {
            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
              Text(
                text = logEntry.tag,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
            Text(
              text = logEntry.timestamp.takeLast(TIMESTAMP_DISPLAY_LENGTH),
              style = MaterialTheme.typography.bodySmall,
              fontFamily = FontFamily.Monospace,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(top = 2.dp)
            )
          }
        },
        navigationIcon = {
          BackButton(onClick = onBack)
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
      )
    }
  ) { paddingValues ->
    LogEntryDetailContent(
      logEntry = logEntry,
      onFilterTag = onFilterTag,
      modifier = Modifier.padding(paddingValues)
    )
  }
}

@Composable
private fun LogEntryDetailContent(
  logEntry: LogcatEntry,
  onFilterTag: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxSize()) {
    SelectionContainer(
      modifier = Modifier
        .weight(1f)
        .verticalScroll(rememberScrollState())
    ) {
      Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
        DetailMessageSection(message = logEntry.message)
      }
    }

    DetailActionButtons(
      logEntry = logEntry,
      onFilterTag = onFilterTag
    )
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
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.onSurface,
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
        val clipboardLabel = context.getString(R.string.debugoverlay_clipboard_label_logcat)
        scope.copyToClipboard(clipboard, logEntry.rawLine, clipboardLabel)
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
