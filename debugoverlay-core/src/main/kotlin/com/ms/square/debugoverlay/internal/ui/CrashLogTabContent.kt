package com.ms.square.debugoverlay.internal.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ms.square.debugoverlay.core.R
import com.ms.square.debugoverlay.internal.crash.CrashRecord
import com.ms.square.debugoverlay.internal.crash.CrashRecordInfo
import com.ms.square.debugoverlay.internal.util.formatRelativeTime
import kotlinx.coroutines.flow.StateFlow

/**
 * Crash tab content displaying [CrashRecordInfo]s persisted by
 * [com.ms.square.debugoverlay.internal.crash.CrashHandler] from previous app runs.
 *
 * The tab is always present, so the empty state is the common case until the first crash —
 * it's what tells the reader the feature exists and that a record appears only after a restart.
 *
 * @param crashRecordsFlow emits null until the records have been read from disk, which renders
 *   as blank rather than as the empty state — otherwise "No Crashes Recorded" flashes for a
 *   frame every time the tab opens, including when records do exist.
 */
@Composable
internal fun CrashLogTabContent(
  crashRecordsFlow: StateFlow<List<CrashRecordInfo>?>,
  onDeleteCrashRecord: (CrashRecordInfo) -> Unit,
  modifier: Modifier = Modifier,
) {
  val crashRecords by crashRecordsFlow.collectAsStateWithLifecycle()
  var selected by remember { mutableStateOf<CrashRecordInfo?>(null) }

  DetailNavigation(
    selectedItem = selected,
    onBack = { selected = null },
    listContent = {
      when {
        // Not loaded yet: a directory listing is fast enough that a spinner would itself flash.
        crashRecords == null -> Box(modifier = Modifier.fillMaxSize())
        crashRecords.isNullOrEmpty() -> EmptyCrashHistoryState()
        else -> CrashLogListScreen(
          crashRecords = crashRecords.orEmpty(),
          onItemClick = { selected = it }
        )
      }
    },
    detailContent = { info ->
      CrashLogDetailScreen(
        record = info.record,
        onBack = { selected = null },
        onDelete = {
          // Fire-and-forget: the repository owns the deletion's lifetime, so navigating away
          // immediately can't strand it half-done.
          onDeleteCrashRecord(info)
          selected = null
        }
      )
    },
    modifier = modifier
  )
}

@Composable
private fun CrashLogListScreen(
  crashRecords: List<CrashRecordInfo>,
  onItemClick: (CrashRecordInfo) -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    items(crashRecords, key = { it.record.id }) { info ->
      CrashLogItem(record = info.record, onClick = { onItemClick(info) })
    }
  }
}

@Composable
private fun CrashLogItem(record: CrashRecord, onClick: () -> Unit, modifier: Modifier = Modifier) {
  val timeStamp = remember(record.timestampMs) { formatRelativeTime(record.timestampMs) }
  val itemDescription = stringResource(
    R.string.debugoverlay_crash_log_item_description,
    record.exceptionType,
    timeStamp
  )

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .semantics(mergeDescendants = true) {
        contentDescription = itemDescription
        role = Role.Button
      }
      .clickable { onClick() },
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.surfaceContainerLowest,
    tonalElevation = 1.dp
  ) {
    Column(modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp)) {
      Text(
        text = record.exceptionType,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.error,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      record.message?.let { message ->
        Text(
          text = message,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.padding(top = 4.dp)
        )
      }
      Text(
        text = timeStamp,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp)
      )
    }
  }
}

@Composable
private fun EmptyCrashHistoryState(modifier: Modifier = Modifier) {
  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text("✅", fontSize = 48.sp)
      Spacer(Modifier.height(16.dp))
      Text(
        text = stringResource(R.string.debugoverlay_crash_log_empty_title),
        style = MaterialTheme.typography.titleMedium
      )
      Text(
        text = stringResource(R.string.debugoverlay_crash_log_empty_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 8.dp)
      )
    }
  }
}
