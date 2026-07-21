package com.ms.square.debugoverlay.internal.ui

import android.view.View
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ClearAll
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
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import com.ms.square.debugoverlay.internal.data.DEFAULT_CUSTOM_LOG_SOURCE_NAME
import com.ms.square.debugoverlay.internal.data.DebugOverlayDataRepository
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private enum class BuiltInTab(@param:StringRes val titleResId: Int) {
  LOGCAT(R.string.debugoverlay_tab_logcat),
  CUSTOM_LOG(R.string.debugoverlay_tab_custom_log), // Fallback; UI uses dynamic title from source
  APP_EXITS(R.string.debugoverlay_tab_app_exits),
  NETWORK(R.string.debugoverlay_tab_network),
  JANKSTATS(R.string.debugoverlay_tab_jankstats),
  UI(R.string.debugoverlay_tab_ui),
  DEVICE_INFO(R.string.debugoverlay_tab_device_info),
}

/**
 * A visible tab in the panel — either a built-in tab or a custom [DebugTab].
 */
private sealed class PanelTab {
  data class BuiltIn(val tab: BuiltInTab) : PanelTab()
  data class Custom(val tab: DebugTab) : PanelTab()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DebugPanelDialog(onDismiss: () -> Unit, isCompactHeight: Boolean = false) {
  val snackBarHostState = remember { SnackbarHostState() }
  // Only collapse the top bar on scroll when vertical space is tight; otherwise keep it static.
  val scrollBehavior = if (isCompactHeight) TopAppBarDefaults.enterAlwaysScrollBehavior() else null

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
      modifier = Modifier
        .fillMaxSize()
        .let { if (scrollBehavior != null) it.nestedScroll(scrollBehavior.nestedScrollConnection) else it },
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
          onDismiss = onDismiss,
          scrollBehavior = scrollBehavior
        )
      }
    ) { paddingValues ->
      DebugPanelContent(
        isCompactHeight = isCompactHeight,
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
  overlayDataRepository: DebugOverlayDataRepository = DebugOverlay.overlayDataRepository,
  bugReportGenerator: BugReportGenerator = DebugOverlay.bugReportGenerator,
  snackBarHostState: SnackbarHostState,
  onDismiss: () -> Unit,
  scrollBehavior: TopAppBarScrollBehavior? = null,
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
      ClearLogsButton(
        onClick = { overlayDataRepository.clearAllLogs() }
      )
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
    ),
    scrollBehavior = scrollBehavior
  )
}

@Composable
private fun ClearLogsButton(onClick: () -> Unit) {
  val description = stringResource(R.string.debugoverlay_clear_logs_content_description)
  IconButton(
    onClick = onClick,
    modifier = Modifier.semantics { contentDescription = description }
  ) {
    Icon(
      imageVector = Icons.Default.ClearAll,
      contentDescription = null // Handled by IconButton semantics
    )
  }
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
private fun DebugPanelContent(isCompactHeight: Boolean, modifier: Modifier = Modifier) {
  val repository = DebugOverlay.overlayDataRepository
  val hasCustomLogSource by repository.hasCustomLogSource.collectAsStateWithLifecycle()
  val customLogSourceName by repository.customLogSourceName.collectAsStateWithLifecycle()
  val customTabs = (DebugOverlay.config.overlayMode as? OverlayMode.WithCustomTabs)?.customTabs.orEmpty()

  // Build visible tabs: built-in tabs (with CUSTOM_LOG conditionally shown) + custom tabs
  val visibleTabs = remember(hasCustomLogSource, customTabs) {
    val builtIn = BuiltInTab.entries
      .filter { it != BuiltInTab.CUSTOM_LOG || hasCustomLogSource }
      .map { PanelTab.BuiltIn(it) }
    builtIn + customTabs.map { PanelTab.Custom(it) }
  }

  var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
  selectedIndex = selectedIndex.coerceIn(0, visibleTabs.lastIndex)

  Column(modifier = modifier.fillMaxSize()) {
    DebugPanelTabRow(
      visibleTabs = visibleTabs,
      selectedIndex = selectedIndex,
      customLogSourceName = customLogSourceName,
      onTabSelected = { selectedIndex = it }
    )
    DebugPanelTabContent(
      selectedTab = visibleTabs[selectedIndex],
      repository = repository,
      isCompactHeight = isCompactHeight
    )
  }
}

@Composable
private fun DebugPanelTabRow(
  visibleTabs: List<PanelTab>,
  selectedIndex: Int,
  customLogSourceName: String?,
  onTabSelected: (Int) -> Unit,
) {
  PrimaryScrollableTabRow(
    selectedTabIndex = selectedIndex,
    modifier = Modifier.fillMaxWidth(),
    containerColor = Color.Transparent
  ) {
    visibleTabs.forEachIndexed { index, panelTab ->
      Tab(
        selected = index == selectedIndex,
        onClick = { onTabSelected(index) },
        text = {
          Text(
            text = when (panelTab) {
              is PanelTab.BuiltIn -> if (panelTab.tab == BuiltInTab.CUSTOM_LOG) {
                customLogSourceName ?: DEFAULT_CUSTOM_LOG_SOURCE_NAME
              } else {
                stringResource(panelTab.tab.titleResId)
              }
              is PanelTab.Custom -> panelTab.tab.title
            },
            style = MaterialTheme.typography.labelLarge
          )
        }
      )
    }
  }
}

@Composable
private fun DebugPanelTabContent(
  selectedTab: PanelTab,
  repository: DebugOverlayDataRepository,
  isCompactHeight: Boolean,
) {
  when (selectedTab) {
    is PanelTab.BuiltIn -> when (selectedTab.tab) {
      BuiltInTab.LOGCAT -> LogTabContent(logsFlow = repository.logcatLogs, isCompactHeight = isCompactHeight)
      BuiltInTab.CUSTOM_LOG -> LogTabContent(
        logsFlow = repository.customLogSourceLogs,
        isCompactHeight = isCompactHeight
      )
      BuiltInTab.APP_EXITS -> AppExitTabContent(
        exitInfosFlow = repository.appExitInfos,
        isSupported = repository.isAppExitSupported
      )
      BuiltInTab.NETWORK -> NetworkTabContent(
        netStatsFlow = repository.netStats,
        networkRequestsFlow = repository.networkRequests,
        isCompactHeight = isCompactHeight
      )
      BuiltInTab.JANKSTATS -> JankStatsTabContent(jankStatsFlow = repository.jankStats)
      BuiltInTab.UI -> UiTabContent()
      BuiltInTab.DEVICE_INFO -> DeviceInfoTabContent(deviceInfoFlow = repository.deviceInfo)
    }
    is PanelTab.Custom -> key(selectedTab.tab) { selectedTab.tab.content() }
  }
}
