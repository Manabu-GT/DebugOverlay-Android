package com.ms.square.debugoverlay.extension.trigger.shake

import android.app.Application
import android.content.Context
import android.hardware.SensorManager
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.ms.square.debugoverlay.DebugOverlay

/**
 * Foreground-only shake listener. Registers the [ShakeDetector] when the app moves to the
 * foreground and unregisters on background, so we never wake the accelerometer in the background.
 */
internal class ShakeListener(private val application: Application) :
  DefaultLifecycleObserver,
  ShakeDetector.Listener {

  private val detector = ShakeDetector(this).apply {
    setSensitivity(ShakeDetector.SENSITIVITY_MEDIUM)
  }
  private val sensorManager =
    application.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

  override fun onStart(owner: LifecycleOwner) {
    sensorManager?.let {
      if (!detector.start(it)) {
        Log.i("[DebugOverlay-Shake]", "Accelerometer unavailable — shake-to-open disabled")
      }
    }
  }

  override fun onStop(owner: LifecycleOwner) {
    detector.stop()
  }

  override fun hearShake() {
    DebugOverlay.openPanel(application)
  }
}
