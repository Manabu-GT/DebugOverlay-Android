package com.ms.square.debugoverlay.internal.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ms.square.debugoverlay.core.R
import com.ms.square.debugoverlay.internal.data.model.AppExitInfo
import com.ms.square.debugoverlay.internal.data.model.toColor
import com.ms.square.debugoverlay.internal.util.formatMemoryKbToMb
import com.ms.square.debugoverlay.internal.util.formatRelativeTime

/**
 * App Exit tab content displaying historical app termination reasons.
 *
 * Features:
 * - Lists app exit events sorted by timestamp (most recent first)
 * - Color-coded severity indicators (critical/warning/info)
 * - Shows PSS and process importance
 * - Detail screen with full stack trace for crashes/ANRs
 * - Fallback message for Android 10 and below
 */
@Composable
internal fun AppExitTabContent(
  exitInfos: List<AppExitInfo>,
  isSupported: Boolean,
  modifier: Modifier = Modifier,
) {
  var selectedExitInfo by remember { mutableStateOf<AppExitInfo?>(null) }

  DetailNavigation(
    selectedItem = selectedExitInfo,
    onBack = { selectedExitInfo = null },
    listContent = {
      when {
        !isSupported -> ApiNotSupportedState()
        exitInfos.isEmpty() -> EmptyExitHistoryState()
        else -> AppExitListScreen(
          exitInfos = exitInfos,
          onItemClick = { selectedExitInfo = it }
        )
      }
    },
    detailContent = { exitInfo ->
      AppExitDetailScreen(
        exitInfo = exitInfo,
        onBack = { selectedExitInfo = null }
      )
    },
    modifier = modifier
  )
}

@Composable
private fun AppExitListScreen(
  exitInfos: List<AppExitInfo>,
  onItemClick: (AppExitInfo) -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    items(exitInfos, key = { it.id }) { exitInfo ->
      AppExitItem(
        exitInfo = exitInfo,
        onClick = { onItemClick(exitInfo) }
      )
    }
  }
}

@Composable
private fun AppExitItem(
  exitInfo: AppExitInfo,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val severityColor = exitInfo.reason.severity.toColor()
  val timeStamp = remember(exitInfo.timestampMs) {
    formatRelativeTime(exitInfo.timestampMs)
  }
  val itemDescription = stringResource(
    R.string.debugoverlay_app_exits_item_description,
    exitInfo.reason.label,
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
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLowest,
    tonalElevation = 1.dp
  ) {
    Row(
      modifier = Modifier
        .height(IntrinsicSize.Min)
        .padding(vertical = 12.dp, horizontal = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      // Severity indicator bar
      Box(
        modifier = Modifier
          .width(4.dp)
          .fillMaxHeight()
          .background(
            color = severityColor,
            shape = RoundedCornerShape(2.dp)
          )
      )

      Column(modifier = Modifier.weight(1f)) {
        // Header row: Reason + Time
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = exitInfo.reason.label,
            style = MaterialTheme.typography.titleSmall,
            color = severityColor
          )
          Text(
            text = formatRelativeTime(exitInfo.timestampMs),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        // Description
        exitInfo.description?.let { description ->
          Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp)
          )
        }

        // Metadata: PSS + Importance
        Row(
          modifier = Modifier.padding(top = 8.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            text = "PSS: ${formatMemoryKbToMb(exitInfo.pssKb)}",
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = "\u00B7",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
          )
          Text(
            text = exitInfo.importance.label,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
  }
}

@Composable
private fun EmptyExitHistoryState(modifier: Modifier = Modifier) {
  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text("\uD83D\uDCCA", fontSize = 48.sp) // 📊
      Spacer(Modifier.height(16.dp))
      Text(
        text = stringResource(R.string.debugoverlay_app_exits_empty_title),
        style = MaterialTheme.typography.titleMedium
      )
      Spacer(Modifier.height(8.dp))
      Text(
        text = stringResource(R.string.debugoverlay_app_exits_empty_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
      )
    }
  }
}

@Composable
private fun ApiNotSupportedState(modifier: Modifier = Modifier) {
  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text("\uD83D\uDCF1", fontSize = 48.sp) // 📱
      Spacer(Modifier.height(16.dp))
      Text(
        text = stringResource(R.string.debugoverlay_app_exits_requires_api30_title),
        style = MaterialTheme.typography.titleMedium
      )
      Spacer(Modifier.height(8.dp))
      Text(
        text = stringResource(
          R.string.debugoverlay_app_exits_requires_api30_subtitle,
          Build.VERSION.SDK_INT
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
      )
    }
  }
}
