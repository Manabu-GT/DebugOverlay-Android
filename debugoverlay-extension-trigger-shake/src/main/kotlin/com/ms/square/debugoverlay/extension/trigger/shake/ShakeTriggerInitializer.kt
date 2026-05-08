package com.ms.square.debugoverlay.extension.trigger.shake

import android.app.Application
import android.content.Context
import androidx.lifecycle.ProcessLifecycleInitializer
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.startup.Initializer
import com.ms.square.debugoverlay.internal.InternalDebugOverlayApi
import com.ms.square.debugoverlay.internal.util.isMainProcess

/**
 * AndroidX Startup initializer that auto-installs a foreground-only shake-to-open-panel handler.
 *
 * Adding the `debugoverlay-extension-trigger-shake` dependency is the entire opt-in: shaking the
 * device while the app is foregrounded calls
 * [com.ms.square.debugoverlay.DebugOverlay.openPanel].
 *
 * Hardcoded defaults (no public knobs by design — file an issue if you need overrides):
 * - Sensitivity: [ShakeDetector.SENSITIVITY_MEDIUM] (matches upstream Seismic default)
 * - Action: [com.ms.square.debugoverlay.DebugOverlay.openPanel]
 * - Listening window: foreground only ([androidx.lifecycle.ProcessLifecycleOwner])
 */
public class ShakeTriggerInitializer : Initializer<Unit> {
  override fun create(context: Context) {
    val application = context.applicationContext as? Application
      ?: error("Can not cast the given context to an Application")
    @OptIn(InternalDebugOverlayApi::class)
    if (!isMainProcess(application)) {
      // Skip non-main processes so we don't register duplicate listeners.
      return
    }
    val listener = ShakeListener(application)
    ProcessLifecycleOwner.get().lifecycle.addObserver(listener)
  }

  override fun dependencies(): List<Class<out Initializer<*>>> = listOf(ProcessLifecycleInitializer::class.java)
}
