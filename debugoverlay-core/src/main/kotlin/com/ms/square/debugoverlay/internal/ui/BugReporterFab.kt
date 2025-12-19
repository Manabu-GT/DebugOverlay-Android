package com.ms.square.debugoverlay.internal.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ms.square.debugoverlay.DebugOverlay
import com.ms.square.debugoverlay.core.R
import com.ms.square.debugoverlay.internal.bugreport.BugReportResult
import com.ms.square.debugoverlay.internal.bugreport.BugReportSnapshot
import com.ms.square.debugoverlay.internal.bugreport.IntentShareExporter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal val FAB_SIZE = 56.dp

private const val FEEDBACK_DISPLAY_DURATION_MS = 1500L
private const val COLOR_ANIMATION_DURATION_MS = 200
private const val SCALE_ANIMATION_DURATION_MS = 150

/**
 * Floating Action Button for quick bug reporting.
 *
 * Flow: Tap FAB → capture screenshot → show metadata dialog → generate report → share.
 *
 * Visual states:
 * - Idle: Normal appearance, ready for tap
 * - Processing: Spinner overlay while capturing/writing
 * - Success: Green tint with scale animation
 * - Error: Red tint to indicate failure
 *
 * @param modifier Modifier for the FAB
 * @param onError Called when an error occurs with the error message
 */
@Suppress("LongMethod")
@Composable
internal fun BugReporterFab(modifier: Modifier = Modifier, onError: (String) -> Unit = {}) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var fabState by remember { mutableStateOf<BugReporterFabState>(BugReporterFabState.Idle) }
  var currentSnapshot by remember { mutableStateOf<BugReportSnapshot?>(null) }

  // Show metadata dialog when snapshot is available
  currentSnapshot?.let { snapshot ->
    BugReportMetadataDialog(
      screenshot = snapshot.screenshot,
      isSubmitting = fabState == BugReporterFabState.Processing,
      onConfirm = { metadata ->
        scope.launch {
          fabState = BugReporterFabState.Processing
          when (val result = DebugOverlay.bugReportGenerator.writeReport(snapshot, metadata)) {
            is BugReportResult.Success -> {
              val exported = IntentShareExporter(context).export(result.zipFile)
              fabState = if (exported) {
                BugReporterFabState.Success
              } else {
                val errorMsg = context.getString(R.string.debugoverlay_share_bug_report_error)
                onError(errorMsg)
                BugReporterFabState.Error(errorMsg)
              }
            }
            is BugReportResult.Error.IoError -> {
              val errorMsg = context.getString(R.string.debugoverlay_bug_report_error)
              onError(errorMsg)
              fabState = BugReporterFabState.Error(errorMsg)
            }
          }
          currentSnapshot = null
        }
      },
      onDismiss = {
        currentSnapshot = null
        fabState = BugReporterFabState.Idle
      }
    )
  }

  // Reset state after success/error feedback
  LaunchedEffect(fabState) {
    if (fabState is BugReporterFabState.Success || fabState is BugReporterFabState.Error) {
      delay(FEEDBACK_DISPLAY_DURATION_MS)
      fabState = BugReporterFabState.Idle
    }
  }

  // Animate colors based on state
  val containerColor by animateColorAsState(
    targetValue = fabContainerColor(fabState),
    animationSpec = tween(COLOR_ANIMATION_DURATION_MS),
    label = "containerColor"
  )

  val contentColor by animateColorAsState(
    targetValue = fabContentColor(fabState),
    animationSpec = tween(COLOR_ANIMATION_DURATION_MS),
    label = "contentColor"
  )

  // Scale animation for success state
  val scale by animateFloatAsState(
    targetValue = if (fabState is BugReporterFabState.Success) 1.1f else 1f,
    animationSpec = tween(SCALE_ANIMATION_DURATION_MS),
    label = "scale"
  )

  val stateDescription = fabStateDescription(fabState)

  FloatingActionButton(
    onClick = {
      if (fabState == BugReporterFabState.Idle) {
        scope.launch {
          fabState = BugReporterFabState.Processing
          val result = DebugOverlay.bugReportGenerator.captureSnapshot()
          result.onSuccess { snapshot ->
            currentSnapshot = snapshot
            fabState = BugReporterFabState.Idle // Dialog will show
          }.onFailure {
            val errorMsg = context.getString(R.string.debugoverlay_bug_report_error)
            onError(errorMsg)
            fabState = BugReporterFabState.Error(errorMsg)
          }
        }
      }
    },
    modifier = modifier
      .size(FAB_SIZE)
      .scale(scale),
    containerColor = containerColor,
    contentColor = contentColor
  ) {
    Box(contentAlignment = Alignment.Center) {
      if (fabState == BugReporterFabState.Processing) {
        CircularProgressIndicator(
          modifier = Modifier.size(24.dp),
          strokeWidth = 2.dp,
          color = contentColor
        )
      } else {
        Icon(
          imageVector = Icons.Default.BugReport,
          contentDescription = stateDescription
        )
      }
    }
  }
}

/** Returns container color based on FAB state using M3 semantic colors. */
@Composable
private fun fabContainerColor(fabState: BugReporterFabState) = when (fabState) {
  is BugReporterFabState.Success -> MaterialTheme.colorScheme.primaryContainer
  is BugReporterFabState.Error -> MaterialTheme.colorScheme.errorContainer
  else -> MaterialTheme.colorScheme.tertiaryContainer
}

/** Returns content color based on FAB state using M3 semantic colors. */
@Composable
private fun fabContentColor(fabState: BugReporterFabState) = when (fabState) {
  is BugReporterFabState.Success -> MaterialTheme.colorScheme.onPrimaryContainer
  is BugReporterFabState.Error -> MaterialTheme.colorScheme.onErrorContainer
  else -> MaterialTheme.colorScheme.onTertiaryContainer
}

/** Returns accessibility description based on FAB state. */
@Composable
private fun fabStateDescription(fabState: BugReporterFabState) = when (fabState) {
  BugReporterFabState.Idle -> stringResource(R.string.debugoverlay_bug_report)
  BugReporterFabState.Processing -> stringResource(R.string.debugoverlay_bug_report_generating)
  is BugReporterFabState.Success -> stringResource(R.string.debugoverlay_bug_report_success)
  is BugReporterFabState.Error -> stringResource(R.string.debugoverlay_bug_report_failed)
}
