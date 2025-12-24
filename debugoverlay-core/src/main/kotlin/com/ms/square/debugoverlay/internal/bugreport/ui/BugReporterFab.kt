package com.ms.square.debugoverlay.internal.bugreport.ui

import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ms.square.debugoverlay.DebugOverlay
import com.ms.square.debugoverlay.core.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal val FAB_SIZE = 56.dp

private const val FEEDBACK_DISPLAY_DURATION_MS = 1500L
private const val COLOR_ANIMATION_DURATION_MS = 200

/**
 * Floating Action Button for quick bug reporting.
 *
 * Flow: Tap FAB → capture screenshot → launch [BugReportActivity] for metadata dialog.
 *
 * Visual states:
 * - Idle: Normal appearance, ready for tap
 * - Processing: Spinner overlay while capturing screenshot
 * - Error: Red tint to indicate capture failure
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

  // Reset state after error feedback (success is handled by BugReportActivity)
  LaunchedEffect(fabState) {
    if (fabState is BugReporterFabState.Error) {
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

  val stateDescription = fabStateDescription(fabState)

  FloatingActionButton(
    onClick = {
      if (fabState == BugReporterFabState.Idle) {
        scope.launch {
          fabState = BugReporterFabState.Processing
          DebugOverlay.bugReportGenerator.captureToFolder()
            .onSuccess { folder ->
              // Launch activity with folder path for metadata dialog
              val intent = Intent(context, BugReportActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(INTENT_EXTRA_CAPTURE_FOLDER, folder.absolutePath)
              }
              context.startActivity(intent)
              fabState = BugReporterFabState.Idle
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
      .semantics { contentDescription = stateDescription }
      .border(
        width = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
        shape = CircleShape
      ),
    shape = CircleShape,
    containerColor = containerColor,
    contentColor = contentColor,
    elevation = FloatingActionButtonDefaults.elevation(
      defaultElevation = 0.dp,
      pressedElevation = 0.dp,
      focusedElevation = 0.dp,
      hoveredElevation = 0.dp
    )
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
          contentDescription = null // Handled by FAB semantics
        )
      }
    }
  }
}

/** Returns container color based on FAB state using M3 semantic colors. */
@Composable
private fun fabContainerColor(fabState: BugReporterFabState) = when (fabState) {
  is BugReporterFabState.Error -> MaterialTheme.colorScheme.errorContainer
  else -> MaterialTheme.colorScheme.surfaceContainerLow
}

/** Returns content color based on FAB state using M3 semantic colors. */
@Composable
private fun fabContentColor(fabState: BugReporterFabState) = when (fabState) {
  is BugReporterFabState.Error -> MaterialTheme.colorScheme.onErrorContainer
  else -> MaterialTheme.colorScheme.onSurface
}

/** Returns accessibility description based on FAB state. */
@Composable
private fun fabStateDescription(fabState: BugReporterFabState) = when (fabState) {
  BugReporterFabState.Idle -> stringResource(R.string.debugoverlay_bug_report)
  BugReporterFabState.Processing -> stringResource(R.string.debugoverlay_bug_report_generating)
  is BugReporterFabState.Error -> stringResource(R.string.debugoverlay_bug_report_failed)
}
