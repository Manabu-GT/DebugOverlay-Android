package com.ms.square.debugoverlay.internal.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ms.square.debugoverlay.DebugOverlay
import com.ms.square.debugoverlay.DebugTab
import com.ms.square.debugoverlay.core.R
import com.ms.square.debugoverlay.internal.data.DEFAULT_CUSTOM_LOG_SOURCE_NAME
import com.ms.square.debugoverlay.internal.data.DebugOverlayDataRepository

/** Internal CustomLog tab — auto-injected when a custom log source is configured. */
internal val CustomLog: DebugTab = DebugTab("", {})

/**
 * Resolves the visible tab list from the configured tabs.
 * Auto-injects [CustomLog] after [DebugTab.Logcat] when a custom log source is configured.
 */
internal fun resolveVisibleTabs(configTabs: List<DebugTab>, hasCustomLogSource: Boolean): List<DebugTab> {
  if (!hasCustomLogSource) return configTabs

  // Insert after Logcat (matching the existing tab order), or first if Logcat absent
  val logcatIndex = configTabs.indexOfFirst { it === DebugTab.Logcat }
  val insertIndex = if (logcatIndex >= 0) logcatIndex + 1 else 0
  return configTabs.toMutableList().apply { add(insertIndex, CustomLog) }
}

/** Resolves the display title for a tab. Built-in tabs use string resources. */
@Composable
internal fun resolveTitle(
  tab: DebugTab,
  repository: DebugOverlayDataRepository = DebugOverlay.overlayDataRepository,
): String {
  // Hoist collectAsStateWithLifecycle unconditionally to avoid conditional hook calls
  val customLogName by repository.customLogSourceName.collectAsStateWithLifecycle()

  return when {
    tab === DebugTab.Logcat -> stringResource(R.string.debugoverlay_tab_logcat)
    tab === DebugTab.Network -> stringResource(R.string.debugoverlay_tab_network)
    tab === DebugTab.JankStats -> stringResource(R.string.debugoverlay_tab_jankstats)
    tab === DebugTab.AppExits -> stringResource(R.string.debugoverlay_tab_app_exits)
    tab === DebugTab.Ui -> stringResource(R.string.debugoverlay_tab_ui)
    tab === DebugTab.DeviceInfo -> stringResource(R.string.debugoverlay_tab_device_info)
    tab === CustomLog -> customLogName ?: DEFAULT_CUSTOM_LOG_SOURCE_NAME
    else -> tab.title
  }
}

/** Renders the content for a tab. Built-in tabs dispatch to their composables. */
@Composable
internal fun RenderTabContent(tab: DebugTab, repository: DebugOverlayDataRepository) {
  when {
    tab === DebugTab.Logcat -> LogTabContent(logsFlow = repository.logcatLogs)
    tab === DebugTab.Network -> NetworkTabContent(
      netStatsFlow = repository.netStats,
      networkRequestsFlow = repository.networkRequests
    )
    tab === DebugTab.JankStats -> JankStatsTabContent(jankStatsFlow = repository.jankStats)
    tab === DebugTab.AppExits -> AppExitTabContent(
      exitInfosFlow = repository.appExitInfos,
      isSupported = repository.isAppExitSupported
    )
    tab === DebugTab.Ui -> UiTabContent()
    tab === DebugTab.DeviceInfo -> DeviceInfoTabContent(deviceInfoFlow = repository.deviceInfo)
    tab === CustomLog -> LogTabContent(logsFlow = repository.customLogSourceLogs)
    else -> tab.content()
  }
}
