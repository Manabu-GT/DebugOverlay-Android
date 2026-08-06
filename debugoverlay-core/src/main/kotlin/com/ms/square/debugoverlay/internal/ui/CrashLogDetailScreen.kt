package com.ms.square.debugoverlay.internal.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import com.ms.square.debugoverlay.core.R
import com.ms.square.debugoverlay.internal.crash.CrashRecord
import com.ms.square.debugoverlay.internal.crash.CrashRecordExporter
import com.ms.square.debugoverlay.internal.crash.CrashRecordInfo
import com.ms.square.debugoverlay.internal.crash.formatCrashRecordAsText
import com.ms.square.debugoverlay.internal.util.copyToClipboard
import com.ms.square.debugoverlay.internal.util.formatFullTimestamp
import com.ms.square.debugoverlay.internal.util.toClipboardText
import kotlinx.coroutines.launch

/**
 * Detail screen for a single persisted crash record.
 *
 * Shows the exception type/message/thread, full stack trace, and the Logcat/custom
 * log/network requests context captured at crash time, plus Copy/Share/Delete actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CrashLogDetailScreen(
  info: CrashRecordInfo,
  onBack: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val record = info.record
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = record.exceptionType,
              style = MaterialTheme.typography.titleSmall,
              color = MaterialTheme.colorScheme.error,
              maxLines = 1
            )
            Text(
              text = formatFullTimestamp(record.timestampMs),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(top = 4.dp)
            )
          }
        },
        navigationIcon = { BackButton(onClick = onBack) },
        actions = {
          IconButton(onClick = { scope.launch { CrashRecordExporter.share(context, record) } }) {
            Icon(
              imageVector = Icons.Default.Share,
              contentDescription = stringResource(R.string.debugoverlay_share_crash_log)
            )
          }
          IconButton(onClick = onDelete) {
            Icon(
              imageVector = Icons.Default.Delete,
              contentDescription = stringResource(R.string.debugoverlay_delete_crash_log)
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
      )
    }
  ) { paddingValues ->
    CrashLogDetailContent(info = info, modifier = Modifier.padding(paddingValues))
  }
}

@Composable
private fun CrashLogDetailContent(info: CrashRecordInfo, modifier: Modifier = Modifier) {
  val record = info.record

  Column(modifier = modifier.fillMaxSize()) {
    SelectionContainer(
      modifier = Modifier
        .weight(1f)
        .verticalScroll(rememberScrollState())
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        record.message?.let { message ->
          Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = message,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onErrorContainer,
              modifier = Modifier.padding(12.dp)
            )
          }
        }

        MonospaceSection(
          title = stringResource(R.string.debugoverlay_crash_log_stack_trace),
          text = record.stackTrace
        )

        if (record.logcatLogs.isNotEmpty()) {
          MonospaceSection(
            title = stringResource(R.string.debugoverlay_crash_log_logcat, record.logcatLogs.size),
            text = record.logcatLogs.joinToString("\n") { it.toClipboardText() }
          )
        }

        record.customLogSourceData?.let { customLogs ->
          if (customLogs.logs.isNotEmpty()) {
            MonospaceSection(
              title = "${customLogs.sourceName} (${customLogs.logs.size})",
              text = customLogs.logs.joinToString("\n") { it.toClipboardText() }
            )
          }
        }

        if (record.networkRequests.isNotEmpty()) {
          MonospaceSection(
            title = stringResource(R.string.debugoverlay_crash_log_network_requests, record.networkRequests.size),
            text = record.networkRequests.joinToString("\n") {
              "${formatFullTimestamp(it.timestampMs)} ${it.method} ${it.url} -> " +
                "${it.statusCode ?: "?"} (${it.durationMs}ms)"
            }
          )
        }
      }
    }

    CopyButton(record = record)
  }
}

@Composable
private fun MonospaceSection(title: String, text: String, modifier: Modifier = Modifier) {
  Column(modifier = modifier.fillMaxWidth()) {
    Text(
      text = title,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier.padding(bottom = 8.dp)
    )
    Surface(
      shape = MaterialTheme.shapes.small,
      color = MaterialTheme.colorScheme.surfaceContainer,
      modifier = Modifier.fillMaxWidth()
    ) {
      Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
          .horizontalScroll(rememberScrollState())
          .padding(12.dp)
      )
    }
  }
}

@Composable
private fun CopyButton(record: CrashRecord, modifier: Modifier = Modifier) {
  val clipboard = LocalClipboard.current
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  Button(
    onClick = {
      val clipboardLabel = context.getString(R.string.debugoverlay_crash_log_clipboard_label)
      scope.copyToClipboard(clipboard, formatCrashRecordAsText(record), clipboardLabel)
    },
    modifier = modifier
      .fillMaxWidth()
      .padding(16.dp)
  ) {
    Text(stringResource(R.string.debugoverlay_copy))
  }
}
