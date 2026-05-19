package com.ms.square.debugoverlay.internal.data.source

import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.ms.square.debugoverlay.internal.Logger
import com.ms.square.debugoverlay.internal.data.model.ThermalState
import com.ms.square.debugoverlay.internal.data.model.ThermalStatus
import com.ms.square.debugoverlay.internal.util.runCatchingNonCancellation
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.isActive
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Heuristic thresholds for deriving a synthetic [ThermalStatus] from a [PowerManager.getThermalHeadroom]
 * reading when [PowerManager.getCurrentThermalStatus] reports [PowerManager.THERMAL_STATUS_NONE].
 *
 * Mirrors the example in the Android ADPF documentation:
 * https://developer.android.com/games/optimize/adpf/thermal#device-limitations-of-the-thermal-api
 *
 * Note on the SEVERE bound: the Javadoc for [PowerManager.getThermalHeadroom] states that `1.0`
 * indicates the SEVERE throttling threshold, so we treat `>= 1.0f` as SEVERE (slightly tighter
 * than the doc's `> 1.0` pseudocode example).
 */
private const val HEADROOM_SEVERE = 1.0f
private const val HEADROOM_MODERATE = 0.95f
private const val HEADROOM_LIGHT = 0.85f

/**
 * Headroom poll interval. Google's ADPF documentation recommends not calling
 * [PowerManager.getThermalHeadroom] more than once every 10 seconds; faster polls
 * may return `NaN`.
 * https://developer.android.com/games/optimize/adpf/thermal#java
 */
internal val DEFAULT_THERMAL_POLL_INTERVAL: Duration = 10L.seconds

/**
 * Maps a raw thermal status int plus a headroom reading to a [ThermalStatus].
 *
 * When the platform reports a status above [PowerManager.THERMAL_STATUS_NONE] it is trusted.
 * Otherwise the headroom value is consulted; values above the [HEADROOM_LIGHT] threshold
 * synthesize a corresponding status (per Google's pseudocode in the ADPF docs).
 *
 * Pure function — extracted for testability.
 */
internal fun deriveThermalStatus(rawStatus: Int, headroom: Float): ThermalStatus = when {
  rawStatus > PowerManager.THERMAL_STATUS_NONE -> rawStatus.toThermalStatus()
  // Per the Javadoc for getThermalHeadroom, NaN means either "device does not support this
  // functionality" or "called significantly faster than once per second". We poll at 10s
  // intervals (well within the rate limit), so NaN in our use case means the device's thermal
  // HAL does not expose headroom — treat as unsupported so the UI hides the row. If the HAL
  // ever starts returning real values, this self-heals on the next emission.
  headroom.isNaN() -> ThermalStatus.UNSUPPORTED
  headroom >= HEADROOM_SEVERE -> ThermalStatus.SEVERE
  headroom > HEADROOM_MODERATE -> ThermalStatus.MODERATE
  headroom > HEADROOM_LIGHT -> ThermalStatus.LIGHT
  else -> ThermalStatus.NONE
}

private fun Int.toThermalStatus(): ThermalStatus = when (this) {
  PowerManager.THERMAL_STATUS_NONE -> ThermalStatus.NONE
  PowerManager.THERMAL_STATUS_LIGHT -> ThermalStatus.LIGHT
  PowerManager.THERMAL_STATUS_MODERATE -> ThermalStatus.MODERATE
  PowerManager.THERMAL_STATUS_SEVERE -> ThermalStatus.SEVERE
  PowerManager.THERMAL_STATUS_CRITICAL -> ThermalStatus.CRITICAL
  PowerManager.THERMAL_STATUS_EMERGENCY -> ThermalStatus.EMERGENCY
  PowerManager.THERMAL_STATUS_SHUTDOWN -> ThermalStatus.SHUTDOWN
  else -> ThermalStatus.NONE
}

/**
 * Data source for [ThermalState] derived from [PowerManager] thermal APIs.
 *
 * Requires Android 11 (API 30) or above. On older devices the data source emits a single
 * [ThermalStatus.UNSUPPORTED] state via `flowOf(...)` and completes. Downstream consumers
 * that wrap this in `shareIn(replay = 1)` (e.g. `DebugOverlayPanelDataSource`) will continue
 * to surface that last value to new subscribers.
 *
 * On API 30+ devices, status is derived from a hybrid of:
 *  - [PowerManager.getCurrentThermalStatus] (push-based via [PowerManager.OnThermalStatusChangedListener])
 *  - [PowerManager.getThermalHeadroom] (polled every [pollInterval]) — used as a fallback signal
 *    when the reported status is [PowerManager.THERMAL_STATUS_NONE], following Google's
 *    recommendation for devices whose thermal HAL is not fully implemented.
 *
 * Per the [PowerManager.getThermalHeadroom] Javadoc, `NaN` means either "device does not
 * support this functionality" or "called significantly faster than once per second". Our
 * 10-second polling sits well within the rate limit, so a persistent `NaN` is treated as
 * "unsupported": [deriveThermalStatus] maps the `NaN` case to [ThermalStatus.UNSUPPORTED]
 * and the UI hides the row. If the HAL later starts returning real values, the next
 * emission self-heals and the row reappears.
 */
internal class ThermalDataSource(
  private val context: Context,
  private val pollInterval: Duration = DEFAULT_THERMAL_POLL_INTERVAL,
) {

  private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

  fun thermalState(): Flow<ThermalState> {
    if (!isApiLevelSupported()) {
      return flowOf(ThermalState(ThermalStatus.UNSUPPORTED))
    }
    @Suppress("NewApi") // isApiLevelSupported implies SDK_INT >= R
    return supportedThermalState()
  }

  @RequiresApi(Build.VERSION_CODES.R)
  private fun supportedThermalState(): Flow<ThermalState> =
    combine(statusFlow(), headroomFlow()) { rawStatus, headroom ->
      ThermalState(deriveThermalStatus(rawStatus, headroom))
    }

  @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
  private fun isApiLevelSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

  @RequiresApi(Build.VERSION_CODES.R)
  private fun statusFlow(): Flow<Int> = callbackFlow {
    val listener = PowerManager.OnThermalStatusChangedListener { status -> trySend(status) }
    // Register the listener BEFORE the seed read so a concurrent thermal-status change between
    // the read and the registration is not silently dropped. A duplicate emission (seed + push
    // with the same value) is harmless — downstream uses shareIn(replay=1) / combine semantics.
    val registered = runCatchingNonCancellation {
      powerManager.addThermalStatusListener(ContextCompat.getMainExecutor(context), listener)
    }.isSuccess
    if (!registered) {
      // Listener registration can throw in test environments (notably Robolectric, which does
      // not shadow the (Executor, OnThermalStatusChangedListener) overload) and conceivably on
      // OEM builds with a stub PowerManagerService. Degrade gracefully: emit a single NONE so
      // combine() has a status seed, then keep the channel open. headroomFlow continues to
      // poll, and deriveThermalStatus consults headroom whenever rawStatus == NONE — so the
      // row still reflects throttling via the headroom heuristic.
      Logger.w("addThermalStatusListener failed; falling back to headroom-only thermal signal")
      trySend(PowerManager.THERMAL_STATUS_NONE)
      awaitClose { /* nothing to unregister */ }
      return@callbackFlow
    }
    trySend(powerManager.currentThermalStatus)
    awaitClose { powerManager.removeThermalStatusListener(listener) }
  }

  @RequiresApi(Build.VERSION_CODES.R)
  private fun headroomFlow(): Flow<Float> = flow {
    while (currentCoroutineContext().isActive) {
      emit(readHeadroom())
      delay(pollInterval)
    }
  }

  @Suppress("NewApi") // Guarded by isApiLevelSupported at call sites
  private fun readHeadroom(): Float = runCatchingNonCancellation {
    powerManager.getThermalHeadroom(0)
  }.getOrElse { e ->
    Logger.w("getThermalHeadroom read failed", e)
    Float.NaN
  }
}
