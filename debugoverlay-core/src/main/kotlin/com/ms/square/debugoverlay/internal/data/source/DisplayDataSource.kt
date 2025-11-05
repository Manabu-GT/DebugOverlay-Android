package com.ms.square.debugoverlay.internal.data.source

import android.content.Context
import android.hardware.display.DisplayManager
import android.view.Display

internal class DisplayDataSource(context: Context) {

  private val defaultDisplay = context.defaultDisplay()

  val currentRefreshRate: Float
    get() = defaultDisplay.refreshRate

  val maxSupportedRefreshRate = lazy {
    defaultDisplay.supportedModes.maxOf { it.refreshRate }
  }
}

private fun Context.defaultDisplay(): Display {
  val dm = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
  return dm.getDisplay(Display.DEFAULT_DISPLAY)
}
