package com.ms.square.debugoverlay.internal.ui

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
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ms.square.debugoverlay.DebugOverlay
import com.ms.square.debugoverlay.core.R

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
  var selectedTabIndex by remember { mutableIntStateOf(0) }
  val tabs = remember {
    listOf(
      R.string.debugoverlay_tab_logcat,
      R.string.debugoverlay_tab_network,
      R.string.debugoverlay_tab_device_info
    )
  }

  Column(
    modifier = modifier.fillMaxSize()
  ) {
    // Tabs
    PrimaryTabRow(
      selectedTabIndex = selectedTabIndex,
      modifier = Modifier.fillMaxWidth(),
      containerColor = Color.Transparent
    ) {
      tabs.forEachIndexed { index, titleResId ->
        Tab(
          selected = selectedTabIndex == index,
          onClick = { selectedTabIndex = index },
          text = {
            Text(
              text = stringResource(titleResId),
              style = MaterialTheme.typography.labelLarge
            )
          }
        )
      }
    }
    // Tab content
    when (selectedTabIndex) {
      0 -> LogcatTabContent(logsFlow = DebugOverlay.overlayDataRepository.logs)
      1 -> NetworkTabContent(
        netStatsFlow = DebugOverlay.overlayDataRepository.netStats,
        networkRequestsFlow = DebugOverlay.overlayDataRepository.networkRequests
      )
      2 -> DeviceInfoTabContent(deviceInfoFlow = DebugOverlay.overlayDataRepository.deviceInfo)
    }
  }
}
