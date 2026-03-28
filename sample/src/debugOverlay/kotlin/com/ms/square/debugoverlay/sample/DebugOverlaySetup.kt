package com.ms.square.debugoverlay.sample

import android.content.Context
import com.ms.square.debugoverlay.DebugOverlay
import com.ms.square.debugoverlay.DebugTab
import com.ms.square.debugoverlay.OverlayMode

/**
 * Configures DebugOverlay with custom tabs and contributors.
 * This file is in the debugOverlay source set, shared by debug and releaseWithOverlay builds.
 */
object DebugOverlaySetup {
  fun init(context: Context) {
    val appContext = context.applicationContext

    // Add a custom tab showing SharedPreferences
    DebugOverlay.configure {
      overlayMode = OverlayMode.FullMetrics(
        customTabs = listOf(
          DebugTab(title = "SharedPrefs") { SharedPrefsTabContent(appContext) }
        )
      )
    }

    // Also contribute the same data to bug reports
    DebugOverlay.addBugReportContributor(SharedPreferencesContributor(appContext))
  }
}
