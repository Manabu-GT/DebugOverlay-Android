package com.ms.square.debugoverlay.internal.ui

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.HorizontalDivider
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
import com.ms.square.debugoverlay.internal.data.model.AppExitInfo
import com.ms.square.debugoverlay.internal.data.model.AppExitReason
import com.ms.square.debugoverlay.internal.util.toColor
import com.ms.square.debugoverlay.internal.util.copyToClipboard
import com.ms.square.debugoverlay.internal.util.formatFullTimestamp
import com.ms.square.debugoverlay.internal.util.formatMemoryKbToMb

/**
 * Detail screen for a single app exit event.
 *
 * Shows:
 * - Exit reason with severity badge
 * - Timestamp
 * - Summary info (process, description, memory, importance)
 * - Stack trace or ANR trace (if available)
 * - Copy button
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppExitDetailScreen(
  exitInfo: AppExitInfo,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val severityColor = exitInfo.reason.severity.toColor()

  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = {
          Column {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = severityColor.copy(alpha = 0.2f)
            ) {
              Text(
                text = exitInfo.reason.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = severityColor,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
              )
            }
            Text(
              text = formatFullTimestamp(exitInfo.timestampMs),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(top = 4.dp)
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
    AppExitDetailContent(
      exitInfo = exitInfo,
      modifier = Modifier.padding(paddingValues)
    )
  }
}

@Composable
private fun AppExitDetailContent(
  exitInfo: AppExitInfo,
  modifier: Modifier = Modifier,
) {
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
        ExplanationSection(exitInfo.reason)

        SummarySection(exitInfo)

        if (!exitInfo.trace.isNullOrBlank()) {
          TraceSection(
            trace = exitInfo.trace,
            isAnr = exitInfo.reason == AppExitReason.ANR
          )
        }
      }
    }

    CopyButton(exitInfo = exitInfo)
  }
}

@Composable
private fun ExplanationSection(
  reason: AppExitReason,
  modifier: Modifier = Modifier,
) {
  val severityColor = reason.severity.toColor()

  Surface(
    shape = RoundedCornerShape(12.dp),
    color = severityColor.copy(alpha = 0.1f),
    modifier = modifier.fillMaxWidth()
  ) {
    Text(
      text = reason.explanation,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.padding(12.dp)
    )
  }
}

@Composable
private fun SummarySection(
  exitInfo: AppExitInfo,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxWidth()) {
    Text(
      text = stringResource(R.string.debugoverlay_app_exits_summary),
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier.padding(bottom = 8.dp)
    )

    Surface(
      shape = RoundedCornerShape(12.dp),
      color = MaterialTheme.colorScheme.surfaceContainerLowest,
      tonalElevation = 1.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Column {
        SummaryRow(
          label = stringResource(R.string.debugoverlay_app_exits_process),
          value = exitInfo.processName
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        exitInfo.description?.let { description ->
          SummaryRow(
            label = stringResource(R.string.debugoverlay_app_exits_description),
            value = description
          )
          HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }

        SummaryRow(
          label = stringResource(R.string.debugoverlay_app_exits_pss),
          value = formatMemoryKbToMb(exitInfo.pssKb)
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        SummaryRow(
          label = stringResource(R.string.debugoverlay_app_exits_rss),
          value = formatMemoryKbToMb(exitInfo.rssKb)
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        SummaryRow(
          label = stringResource(R.string.debugoverlay_app_exits_importance),
          value = exitInfo.importance.label
        )
      }
    }
  }
}

@Composable
private fun SummaryRow(
  label: String,
  value: String,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 12.dp),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodyMedium,
      fontFamily = FontFamily.Monospace,
      color = MaterialTheme.colorScheme.onSurface
    )
  }
}

@Composable
private fun TraceSection(
  trace: String,
  isAnr: Boolean,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxWidth()) {
    Text(
      text = stringResource(
        if (isAnr) R.string.debugoverlay_app_exits_anr_trace else R.string.debugoverlay_app_exits_trace
      ),
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier.padding(bottom = 8.dp)
    )

    Surface(
      shape = RoundedCornerShape(8.dp),
      color = MaterialTheme.colorScheme.surfaceContainer,
      modifier = Modifier.fillMaxWidth()
    ) {
      Text(
        text = trace,
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
private fun CopyButton(
  exitInfo: AppExitInfo,
  modifier: Modifier = Modifier,
) {
  val clipboard = LocalClipboard.current
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  Button(
    onClick = {
      val content = buildExitInfoText(exitInfo)
      val clipboardLabel = context.getString(R.string.debugoverlay_app_exits_clipboard_label)
      scope.copyToClipboard(clipboard, content, clipboardLabel)
    },
    modifier = modifier
      .fillMaxWidth()
      .padding(16.dp)
  ) {
    Text(stringResource(R.string.debugoverlay_copy))
  }
}

private fun buildExitInfoText(exitInfo: AppExitInfo): String = buildString {
  appendLine("Exit Reason: ${exitInfo.reason.label}")
  appendLine("Timestamp: ${formatFullTimestamp(exitInfo.timestampMs)}")
  appendLine("Process: ${exitInfo.processName}")
  exitInfo.description?.let { appendLine("Description: $it") }
  appendLine("PSS: ${formatMemoryKbToMb(exitInfo.pssKb)}")
  appendLine("RSS: ${formatMemoryKbToMb(exitInfo.rssKb)}")
  appendLine("Importance: ${exitInfo.importance.label}")

  if (!exitInfo.trace.isNullOrBlank()) {
    appendLine()
    appendLine("Trace:")
    append(exitInfo.trace)
  }
}
