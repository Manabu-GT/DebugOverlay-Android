package com.ms.square.debugoverlay.internal.ui

import android.content.Intent
import android.view.View
import androidx.annotation.StringRes
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ms.square.debugoverlay.DebugOverlay
import com.ms.square.debugoverlay.core.R
import kotlinx.coroutines.launch

private enum class DebugTab(@param:StringRes val titleResId: Int) {
  LOG(R.string.debugoverlay_tab_log),
  APP_EXITS(R.string.debugoverlay_tab_app_exits),
  NETWORK(R.string.debugoverlay_tab_network),
  JANKSTATS(R.string.debugoverlay_tab_jankstats),
  UI(R.string.debugoverlay_tab_ui),
  DEVICE_INFO(R.string.debugoverlay_tab_device_info),
}

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
private fun DebugPanelTopAppBar(snackBarHostState: SnackbarHostState, onDismiss: () -> Unit) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var isCapturing by remember { mutableStateOf(false) }

  TopAppBar(
    title = {
      Text(
        text = stringResource(R.string.debugoverlay_debug_panel),
        style = MaterialTheme.typography.titleLarge
      )
    },
    actions = {
      BugReportButton(
        isGenerating = isCapturing,
        onGenerateReport = {
          scope.launch {
            isCapturing = true
            try {
              val result = DebugOverlay.bugReportGenerator.captureToFolder()
              result.onSuccess { folder ->
                // Launch BugReportActivity for metadata dialog
                val intent = Intent(context, BugReportActivity::class.java).apply {
                  putExtra(EXTRA_CAPTURE_FOLDER, folder.absolutePath)
                }
                context.startActivity(intent)
              }.onFailure {
                snackBarHostState.showSnackbar(
                  context.getString(R.string.debugoverlay_bug_report_error)
                )
              }
            } finally {
              isCapturing = false
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
private fun BugReportButton(isGenerating: Boolean, onGenerateReport: () -> Unit) {
  IconButton(
    onClick = onGenerateReport,
    enabled = !isGenerating
  ) {
    if (isGenerating) {
      CircularProgressIndicator(
        modifier = Modifier.size(24.dp),
        strokeWidth = 2.dp
      )
    } else {
      Icon(
        imageVector = Icons.Default.BugReport,
        contentDescription = stringResource(R.string.debugoverlay_bug_report)
      )
    }
  }
}

@Composable
private fun DebugPanelContent(modifier: Modifier = Modifier) {
  var selectedTab by remember { mutableStateOf(DebugTab.LOG) }

  Column(
    modifier = modifier.fillMaxSize()
  ) {
    // Tabs
    PrimaryScrollableTabRow(
      selectedTabIndex = selectedTab.ordinal,
      modifier = Modifier.fillMaxWidth(),
      containerColor = Color.Transparent
    ) {
      DebugTab.entries.forEach { tab ->
        Tab(
          selected = selectedTab == tab,
          onClick = { selectedTab = tab },
          text = {
            Text(
              text = stringResource(tab.titleResId),
              style = MaterialTheme.typography.labelLarge
            )
          }
        )
      }
    }
    // Tab content
    when (selectedTab) {
      DebugTab.LOG -> LogTabContent(
        logsFlow = DebugOverlay.overlayDataRepository.logs,
        logSourceNameFlow = DebugOverlay.overlayDataRepository.logSourceName
      )
      DebugTab.APP_EXITS -> AppExitTabContent(
        exitInfosFlow = DebugOverlay.overlayDataRepository.appExitInfos,
        isSupported = DebugOverlay.overlayDataRepository.isAppExitSupported
      )
      DebugTab.NETWORK -> NetworkTabContent(
        netStatsFlow = DebugOverlay.overlayDataRepository.netStats,
        networkRequestsFlow = DebugOverlay.overlayDataRepository.networkRequests
      )
      DebugTab.JANKSTATS -> JankStatsTabContent(
        jankStatsFlow = DebugOverlay.overlayDataRepository.jankStats
      )
      DebugTab.UI -> UiTabContent()
      DebugTab.DEVICE_INFO -> DeviceInfoTabContent(deviceInfoFlow = DebugOverlay.overlayDataRepository.deviceInfo)
    }
  }
}
