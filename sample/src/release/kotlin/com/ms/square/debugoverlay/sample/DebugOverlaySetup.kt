package com.ms.square.debugoverlay.sample

import android.content.Context

/**
 * No-op implementation for release builds.
 * DebugOverlay is not included in release builds.
 */
object DebugOverlaySetup {
  @Suppress("UNUSED_PARAMETER")
  fun init(context: Context) {
    // No-op in release
  }
}
