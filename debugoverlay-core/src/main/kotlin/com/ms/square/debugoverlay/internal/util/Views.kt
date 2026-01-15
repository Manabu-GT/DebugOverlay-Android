package com.ms.square.debugoverlay.internal.util

import android.app.Activity
import android.view.View
import curtains.phoneWindow

internal fun View.findActivity(): Activity? {
  val callback = phoneWindow?.callback
  // check for the callback first as context.findActivity() won't work for certain contexts such as DecorContext.
  // for dialogs, callback could be non-Activity (ex..DialogWrapper), so fallback to the findActivity for those cases.
  return callback as? Activity ?: runCatching { context.findActivity() }.getOrNull()
}
