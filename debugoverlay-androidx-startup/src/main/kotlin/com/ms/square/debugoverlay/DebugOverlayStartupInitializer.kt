package com.ms.square.debugoverlay

import android.app.Application
import android.content.Context
import androidx.startup.Initializer

class DebugOverlayStartupInitializer : Initializer<DebugOverlayStartupInitializer> {
  override fun create(context: Context) = apply {
    val application =
      context.applicationContext as? Application ?: error("Can not cast the given context an Application")
    DebugOverlay.with(application).install()
  }
  override fun dependencies() = emptyList<Class<out Initializer<*>>>()
}
