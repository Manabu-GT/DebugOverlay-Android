package com.ms.square.debugoverlay.internal.bugreport.model

import com.ms.square.debugoverlay.internal.data.model.DeviceInfo
import com.ms.square.debugoverlay.model.AppInfoSummary
import com.ms.square.debugoverlay.model.DeviceInfoSummary

/**
 * Creates an [AppInfoSummary] from the full [AppInfo].
 */
internal fun AppInfo.toSummary() = AppInfoSummary(
  packageName = packageName,
  versionName = versionName,
  versionCode = versionCode,
  isDebuggable = isDebuggable
)

/**
 * Creates a [DeviceInfoSummary] from the full [DeviceInfo].
 */
internal fun DeviceInfo.toSummary() = DeviceInfoSummary(
  manufacturer = hardware.manufacturer,
  model = hardware.model,
  androidVersion = system.androidVersion,
  apiLevel = system.apiLevel,
  locale = system.locale
)
