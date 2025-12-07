package com.ms.square.debugoverlay.internal.util

import android.view.Display

internal val Display.maxSupportedFps: Float
  get() = supportedModes.maxOfOrNull { it.refreshRate } ?: 60f
