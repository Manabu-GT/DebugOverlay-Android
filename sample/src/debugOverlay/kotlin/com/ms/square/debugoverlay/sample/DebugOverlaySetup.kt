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

    val customTabs = listOf(
      DebugTab(title = "SharedPrefs") { SharedPrefsTabContent(appContext) }
    )

    // Add a custom tab showing SharedPreferences.
    // Swap to OverlayMode.Hidden(customTabs) to hide the on-screen overlay entirely
    // and trigger the panel via DebugOverlay.openPanel(context) — see "Open Debug Panel"
    // card on the Overlay Tests screen.
    //
    // showThermal = true adds a thermal-status row to the compact overlay. Requires Android 11+
    // with a working thermal HAL; the row stays hidden on older devices and on devices whose
    // HAL doesn't expose getThermalHeadroom data.
    DebugOverlay.configure {
      overlayMode = OverlayMode.FullMetrics(
        customTabs = customTabs,
        showThermal = true
      )
    }

    // Also contribute the same data to bug reports
    DebugOverlay.addBugReportContributor(SharedPreferencesContributor(appContext))
  }
}
