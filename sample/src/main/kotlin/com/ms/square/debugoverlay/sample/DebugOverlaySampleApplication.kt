package com.ms.square.debugoverlay.sample

import android.app.Application
import com.ms.square.debugoverlay.DebugOverlay

class DebugOverlaySampleApplication : Application() {
  override fun onCreate() {
    super.onCreate()
    // Simplest way to use
    DebugOverlay.with(this).install()
  }
}
