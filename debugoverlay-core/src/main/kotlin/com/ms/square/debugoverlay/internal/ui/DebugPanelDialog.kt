package com.ms.square.debugoverlay.internal.ui

import android.view.View
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ms.square.debugoverlay.DebugOverlay
import com.ms.square.debugoverlay.core.R

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
      topBar = {
        TopAppBar(
          title = {
            Text(
              text = stringResource(R.string.debugoverlay_debug_panel),
              style = MaterialTheme.typography.titleLarge
            )
          },
          actions = {
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
    ) { paddingValues ->
      DebugPanelContent(
        modifier = Modifier
          .fillMaxSize()
          .padding(paddingValues)
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
