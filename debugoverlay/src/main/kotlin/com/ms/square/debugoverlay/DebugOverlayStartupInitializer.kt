package com.ms.square.debugoverlay

import android.app.Application
import android.content.Context
import androidx.startup.Initializer
import com.ms.square.debugoverlay.internal.InternalDebugOverlayApi

public class DebugOverlayStartupInitializer : Initializer<DebugOverlayStartupInitializer> {
  override fun create(context: Context): DebugOverlayStartupInitializer = apply {
    val application =
      context.applicationContext as? Application ?: error("Can not cast the given context an Application")
    @OptIn(InternalDebugOverlayApi::class)
    DebugOverlay.install(application)
  }
  override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
