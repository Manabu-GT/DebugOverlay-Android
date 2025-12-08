package com.ms.square.debugoverlay.internal.util

import android.content.Context
import android.hardware.display.DisplayManager
import android.view.Display

internal fun Context.defaultDisplay(): Display? {
  val dm = getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
  return dm?.getDisplay(Display.DEFAULT_DISPLAY)
}
