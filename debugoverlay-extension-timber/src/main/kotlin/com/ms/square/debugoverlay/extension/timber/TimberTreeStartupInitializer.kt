package com.ms.square.debugoverlay.extension.timber

import android.app.Application
import android.content.Context
import androidx.startup.Initializer
import com.ms.square.debugoverlay.internal.InternalDebugOverlayApi
import com.ms.square.debugoverlay.internal.util.isMainProcess
import timber.log.Timber

/**
 * AndroidX Startup initializer that automatically plants [DebugOverlayTimberTree].
 *
 * When the `debugoverlay-extension-timber` dependency is added, this initializer
 * automatically plants the Timber tree during app startup. No manual setup required.
 *
 * The tree auto-registers with DebugOverlay via [com.ms.square.debugoverlay.DebugOverlay.configure] which works
 * regardless of initialization order - config is stored and applied when DebugOverlay installs.
 */
public class TimberTreeStartupInitializer : Initializer<Unit> {
  override fun create(context: Context) {
    val application =
      context.applicationContext as? Application ?: error("Can not cast the given context an Application")
    @OptIn(InternalDebugOverlayApi::class)
    if (!isMainProcess(application)) {
      // Just return early without any work if it's not running in the main app process
      return
    }
    Timber.plant(DebugOverlayTimberTree())
  }

  override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
