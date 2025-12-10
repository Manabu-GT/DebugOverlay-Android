package com.ms.square.debugoverlay

import android.app.Application
import android.content.Context
import androidx.startup.Initializer

public class DebugOverlayStartupInitializer : Initializer<DebugOverlayStartupInitializer> {
  override fun create(context: Context): DebugOverlayStartupInitializer = apply {
    val application =
      context.applicationContext as? Application ?: error("Can not cast the given context an Application")
    DebugOverlay.install(application)
  }
  override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
