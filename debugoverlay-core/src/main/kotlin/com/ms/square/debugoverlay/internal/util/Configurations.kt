package com.ms.square.debugoverlay.internal.util

import android.content.res.Configuration

internal fun Configuration.isDarkTheme(): Boolean =
  (uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
