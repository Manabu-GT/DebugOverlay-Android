package com.ms.square.debugoverlay.internal.bugreport

import android.app.Activity

internal sealed interface ActivityProvider {
  val activity: Activity?
}
