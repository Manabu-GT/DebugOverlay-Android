package com.ms.square.debugoverlay.internal.util

import android.view.Display

/**
 * Default refresh rate fallback (60Hz) used when display info is unavailable.
 *
 * Display can be null in rare cases: destroyed context, system UI crashes,
 * unusual emulator configurations, or Android Automotive/TV setups.
 * 60Hz is a safe fallback as it's been the standard for most Android devices.
 */
internal const val DEFAULT_REFRESH_RATE = 60f

internal val Display?.currentRefreshRate: Float
  get() = this?.refreshRate ?: DEFAULT_REFRESH_RATE

internal val Display?.maxSupportedFps: Float
  get() = this?.supportedModes?.maxOfOrNull { it.refreshRate } ?: DEFAULT_REFRESH_RATE
