package com.ms.square.debugoverlay.internal.ui

import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ms.square.debugoverlay.DebugOverlay
import com.ms.square.debugoverlay.DebugTab
import com.ms.square.debugoverlay.OverlayMode
import com.ms.square.debugoverlay.core.R
import com.ms.square.debugoverlay.internal.bugreport.BugReportGenerator
import com.ms.square.debugoverlay.internal.bugreport.ui.BugReportActivity
import com.ms.square.debugoverlay.internal.bugreport.ui.DraftCountBadge
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DebugPanelDialog(onDismiss: () -> Unit) {
  val snackBarHostState = remember { SnackbarHostState() }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(
      usePlatformDefaultWidth = false,
      dismissOnBackPress = true,
      dismissOnClickOutside = false
    )
  ) {
    // Tag the Dialog's window DecorView for UI hierarchy filtering
    val view = LocalView.current
    LaunchedEffect(Unit) {
      var v: View? = view
      while ((v?.parent as? View) != null) {
        v = v.parent as? View
      }
      v?.setTag(R.id.debugoverlay_window_marker, true)
    }

    Scaffold(
      modifier = Modifier.fillMaxSize(),
      containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
      snackbarHost = {
        SnackbarHost(hostState = snackBarHostState) { snackBarData ->
          Snackbar(
            snackbarData = snackBarData,
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
          )
        }
      },
      topBar = {
        DebugPanelTopAppBar(
          snackBarHostState = snackBarHostState,
          onDismiss = onDismiss
        )
      }
    ) { paddingValues ->
      DebugPanelContent(
        modifier = Modifier
          .fillMaxSize()
          .padding(paddingValues)
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DebugPanelTopAppBar(
  bugReportGenerator: BugReportGenerator = DebugOverlay.bugReportGenerator,
  snackBarHostState: SnackbarHostState,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var isCapturing by remember { mutableStateOf(false) }

  // Observe draft count for badge and click behavior
  val draftCount by bugReportGenerator.draftCount.collectAsStateWithLifecycle(initialValue = 0)

  TopAppBar(
    title = {
      Text(
        text = stringResource(R.string.debugoverlay_debug_panel),
        style = MaterialTheme.typography.titleLarge
      )
    },
    actions = {
      BugReportButton(
        isCapturing = isCapturing,
        draftCount = draftCount,
        onClick = {
          if (draftCount > 0) {
            BugReportActivity.launchWithDraftPicker(context)
          } else {
            // Start new capture
            scope.launch {
              isCapturing = true
              try {
                bugReportGenerator.captureToFolder()
                  .onSuccess { folder ->
                    // Check if still active before launching activity
                    if (!isActive) return@onSuccess
                    BugReportActivity.launchWithMetadataDialog(context, folder.absolutePath)
                  }.onFailure {
                    snackBarHostState
                      .showSnackbar(context.getString(R.string.debugoverlay_bug_report_error))
                  }
              } finally {
                isCapturing = false
              }
            }
          }
        }
      )
      IconButton(onClick = onDismiss) {
        Icon(
          imageVector = Icons.Default.Close,
          contentDescription = stringResource(R.string.debugoverlay_close)
        )
      }
    },
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
  )
}

@Composable
private fun BugReportButton(isCapturing: Boolean, draftCount: Int, onClick: () -> Unit) {
  // Dynamic accessibility description matching FAB behavior
  val stateDescription = bugReportButtonDescription(isCapturing, draftCount)

  Box {
    IconButton(
      onClick = onClick,
      enabled = !isCapturing,
      modifier = Modifier.semantics { contentDescription = stateDescription }
    ) {
      if (isCapturing) {
        CircularProgressIndicator(
          modifier = Modifier.size(24.dp),
          strokeWidth = 2.dp
        )
      } else {
        Icon(
          imageVector = Icons.Default.BugReport,
          contentDescription = null // Handled by IconButton semantics
        )
      }
    }

    // Badge positioned at top-right corner
    if (draftCount > 0 && !isCapturing) {
      DraftCountBadge(
        draftCount = draftCount,
        modifier = Modifier.align(Alignment.TopEnd)
      )
    }
  }
}

/** Returns accessibility description based on button state and draft count. */
@Composable
private fun bugReportButtonDescription(isCapturing: Boolean, draftCount: Int) = when {
  isCapturing -> stringResource(R.string.debugoverlay_bug_report_generating)
  draftCount > 0 -> stringResource(R.string.debugoverlay_bug_report_drafts_badge, draftCount)
  else -> stringResource(R.string.debugoverlay_bug_report)
}

@Composable
private fun DebugPanelContent(modifier: Modifier = Modifier) {
  // DebugPanelDialog is only shown in FullMetrics mode; safe cast is defensive
  val configTabs = (DebugOverlay.config.overlayMode as? OverlayMode.FullMetrics)?.tabs ?: return
  val repository = DebugOverlay.overlayDataRepository
  val hasCustomLogSource by repository.hasCustomLogSource.collectAsStateWithLifecycle()

  val tabs = remember(configTabs, hasCustomLogSource) {
    resolveVisibleTabs(configTabs, hasCustomLogSource)
  }

  if (tabs.isEmpty()) return

  var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
  // Clamp to valid range when the tab list shrinks (e.g., custom log source removed).
  // Intentionally lands on the last tab rather than resetting to 0 — acceptable for a debug tool.
  selectedIndex = selectedIndex.coerceIn(0, tabs.lastIndex)

  Column(modifier = modifier.fillMaxSize()) {
    DebugPanelTabRow(
      tabs = tabs,
      selectedIndex = selectedIndex,
      onTabSelected = { selectedIndex = it }
    )
    RenderTabContent(tabs[selectedIndex], repository)
  }
}

@Composable
private fun DebugPanelTabRow(tabs: List<DebugTab>, selectedIndex: Int, onTabSelected: (Int) -> Unit) {
  PrimaryScrollableTabRow(
    selectedTabIndex = selectedIndex,
    modifier = Modifier.fillMaxWidth(),
    containerColor = Color.Transparent
  ) {
    tabs.forEachIndexed { index, tab ->
      Tab(
        selected = index == selectedIndex,
        onClick = { onTabSelected(index) },
        text = {
          Text(
            text = resolveTitle(tab),
            style = MaterialTheme.typography.labelLarge
          )
        }
      )
    }
  }
}
