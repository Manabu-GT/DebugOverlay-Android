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
 * Thermal throttling level reported by the platform. Levels [NONE] through [SHUTDOWN] mirror
 * the `PowerManager.THERMAL_STATUS_*` constants. [UNSUPPORTED] is a synthetic value emitted
 * when the device cannot report thermal data — either because it pre-dates the thermal API
 * (Android < 11 / API < 30) or because the thermal HAL is incomplete and
 * `getThermalHeadroom()` returns `NaN` per its Javadoc contract. The UI hides the row in
 * both cases. If a HAL later begins returning real headroom values, the state self-heals
 * to a real level on the next poll.
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
