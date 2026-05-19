package com.ms.square.debugoverlay.internal.data.model

/**
 * Current thermal throttling state of the device.
 *
 * Derived from a combination of [android.os.PowerManager.getCurrentThermalStatus] and
 * [android.os.PowerManager.getThermalHeadroom] (API 30+) using the heuristic described in
 * the Android ADPF documentation:
 * https://developer.android.com/games/optimize/adpf/thermal#device-limitations-of-the-thermal-api
 */
internal data class ThermalState(val status: ThermalStatus)

/**
 * Thermal throttling level reported by the platform, or [UNSUPPORTED] when the device
 * does not support the thermal headroom API (API < 30, or API 30+ device with an
 * incomplete thermal HAL implementation).
 *
 * Levels mirror the `PowerManager.THERMAL_STATUS_*` constants.
 */
internal enum class ThermalStatus {
  NONE,
  LIGHT,
  MODERATE,
  SEVERE,
  CRITICAL,
  EMERGENCY,
  SHUTDOWN,
  UNSUPPORTED,
}
