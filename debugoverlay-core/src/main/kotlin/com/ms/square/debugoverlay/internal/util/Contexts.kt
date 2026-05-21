package com.ms.square.debugoverlay.internal.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.hardware.display.DisplayManager
import android.view.Display

internal fun Context.defaultDisplay(): Display? {
  val dm = getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
  return dm?.getDisplay(Display.DEFAULT_DISPLAY)
}

internal tailrec fun Context.findActivityOrNull(): Activity? = when (this) {
  is Activity -> this
  is ContextWrapper -> this.baseContext.findActivityOrNull()
  else -> null
}
