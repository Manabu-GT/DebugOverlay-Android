package com.ms.square.debugoverlay.internal.util

import com.ms.square.debugoverlay.core.R
import com.ms.square.debugoverlay.internal.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import radiography.Radiography
import radiography.ScanScopes.AllWindowsScope
import radiography.ScannableView
import radiography.ViewStateRenderers.DefaultsNoPii

/**
 * Captures the UI hierarchy using Radiography, excluding debug overlay windows.
 *
 * @return The UI hierarchy as a string, or null if capture failed
 */
internal suspend fun captureUiHierarchy(): String? = withContext(Dispatchers.Default) {
  runCatching {
    Radiography.scan(
      viewStateRenderers = DefaultsNoPii,
      scanScope = excludeDebugOverlayScope
    )
  }.onFailure { e ->
    Logger.w("Failed to capture UI hierarchy", e)
  }.getOrNull()
}

/**
 * A ScanScope that excludes windows marked with [R.id.debugoverlay_window_marker].
 */
private val excludeDebugOverlayScope = {
  AllWindowsScope.findRoots()
    .filter { scannableView ->
      val view = (scannableView as? ScannableView.AndroidView)?.view
      view?.getTag(R.id.debugoverlay_window_marker) != true
    }
    .toList()
}
