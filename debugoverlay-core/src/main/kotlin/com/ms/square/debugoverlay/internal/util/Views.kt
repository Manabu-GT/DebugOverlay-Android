package com.ms.square.debugoverlay.internal.util

import android.app.Activity
import android.view.View
import curtains.phoneWindow

internal fun View.findActivityOrNull(): Activity? {
  val callback = phoneWindow?.callback
  // check for the callback first as context unwrapping won't work for certain contexts such as DecorContext.
  // for dialogs, callback could be non-Activity (ex..DialogWrapper), so fallback to unwrapping the context.
  return callback as? Activity ?: context.findActivityOrNull()
}
