package com.ms.square.debugoverlay.internal.data.source

import android.os.Build
import android.os.PowerManager
import com.google.common.truth.Truth.assertThat
import com.ms.square.debugoverlay.internal.data.model.ThermalStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Tests for [ThermalDataSource] and its [deriveThermalStatus] helper.
 *
 * Robolectric does not shadow [android.os.PowerManager.getThermalHeadroom]; the call falls
 * through to a real (unimplemented) HAL that returns `0` / `NaN`. Per the Javadoc contract,
 * `NaN` (at our 10s polling rate) means the device does not support thermal headroom, so
 * the data source emits [ThermalStatus.UNSUPPORTED] and the UI hides the row. The state
 * self-heals to a real level if the HAL later starts returning data.
 *
 * See Google's ADPF guidance:
 * https://developer.android.com/games/optimize/adpf/thermal#device-limitations-of-the-thermal-api
 */
@RunWith(RobolectricTestRunner::class)
class ThermalDataSourceTest {

  private val context = RuntimeEnvironment.getApplication()
  private val dataSource = ThermalDataSource(context)

  //region ThermalDataSource — API gate / graceful degradation
  @Test
  fun `thermalState emits UNSUPPORTED on API 30+ when headroom HAL is not present`() = runTest {
    // Robolectric defaults to a modern API level (>= R) with a non-shadowed thermal HAL.
    // getThermalHeadroom returns NaN, which per the Javadoc means "device does not support
    // this functionality" (we poll at 10s, so the rate-limit case doesn't apply). The data
    // source must emit UNSUPPORTED so the UI hides the row.
    val state = dataSource.thermalState().first()

    assertThat(state.status).isEqualTo(ThermalStatus.UNSUPPORTED)
  }

  @Test
  @Config(sdk = [Build.VERSION_CODES.P])
  fun `thermalState emits UNSUPPORTED on devices below API 30`() = runTest {
    val preApi30Source = ThermalDataSource(context)

    val state = preApi30Source.thermalState().first()

    assertThat(state.status).isEqualTo(ThermalStatus.UNSUPPORTED)
  }
  //endregion

  //region deriveThermalStatus — rawStatus takes precedence when above NONE
  @Test
  fun `deriveThermalStatus uses raw status when above NONE regardless of headroom`() {
    // Even a benign headroom should not downgrade an explicit SEVERE reading from the platform.
    assertThat(deriveThermalStatus(PowerManager.THERMAL_STATUS_SEVERE, headroom = 0.1f))
      .isEqualTo(ThermalStatus.SEVERE)
  }

  @Test
  fun `deriveThermalStatus maps each raw status code to its enum`() {
    val cases = mapOf(
      PowerManager.THERMAL_STATUS_NONE to ThermalStatus.NONE,
      PowerManager.THERMAL_STATUS_LIGHT to ThermalStatus.LIGHT,
      PowerManager.THERMAL_STATUS_MODERATE to ThermalStatus.MODERATE,
      PowerManager.THERMAL_STATUS_SEVERE to ThermalStatus.SEVERE,
      PowerManager.THERMAL_STATUS_CRITICAL to ThermalStatus.CRITICAL,
      PowerManager.THERMAL_STATUS_EMERGENCY to ThermalStatus.EMERGENCY,
      PowerManager.THERMAL_STATUS_SHUTDOWN to ThermalStatus.SHUTDOWN
    )
    cases.forEach { (raw, expected) ->
      // Headroom 0.5 is irrelevant when raw > NONE; only matters in the NONE fallback path.
      assertThat(deriveThermalStatus(raw, headroom = 0.5f)).isEqualTo(expected)
    }
  }

  @Test
  fun `deriveThermalStatus falls back to NONE for unknown raw status when headroom is benign`() {
    assertThat(deriveThermalStatus(rawStatus = 99, headroom = 0.5f)).isEqualTo(ThermalStatus.NONE)
  }
  //endregion

  //region deriveThermalStatus — headroom fallback (when raw == NONE)
  @Test
  fun `deriveThermalStatus returns NONE when both signals are benign`() {
    assertThat(deriveThermalStatus(PowerManager.THERMAL_STATUS_NONE, headroom = 0.5f))
      .isEqualTo(ThermalStatus.NONE)
  }

  @Test
  fun `deriveThermalStatus returns LIGHT when headroom exceeds 0_85 threshold`() {
    assertThat(deriveThermalStatus(PowerManager.THERMAL_STATUS_NONE, headroom = 0.86f))
      .isEqualTo(ThermalStatus.LIGHT)
  }

  @Test
  fun `deriveThermalStatus returns MODERATE when headroom exceeds 0_95 threshold`() {
    assertThat(deriveThermalStatus(PowerManager.THERMAL_STATUS_NONE, headroom = 0.96f))
      .isEqualTo(ThermalStatus.MODERATE)
  }

  @Test
  fun `deriveThermalStatus returns SEVERE when headroom equals 1_0 exactly`() {
    // Per the Javadoc for getThermalHeadroom: "1.0 indicates the SEVERE throttling threshold."
    assertThat(deriveThermalStatus(PowerManager.THERMAL_STATUS_NONE, headroom = 1.0f))
      .isEqualTo(ThermalStatus.SEVERE)
  }

  @Test
  fun `deriveThermalStatus returns SEVERE when headroom exceeds 1_0`() {
    assertThat(deriveThermalStatus(PowerManager.THERMAL_STATUS_NONE, headroom = 1.05f))
      .isEqualTo(ThermalStatus.SEVERE)
  }

  @Test
  fun `deriveThermalStatus returns UNSUPPORTED when raw is NONE and headroom is NaN`() {
    // Per the Javadoc, NaN at our 10s polling rate means the device does not support thermal
    // headroom — emit UNSUPPORTED so the UI hides the row rather than misleadingly showing NONE.
    assertThat(deriveThermalStatus(PowerManager.THERMAL_STATUS_NONE, headroom = Float.NaN))
      .isEqualTo(ThermalStatus.UNSUPPORTED)
  }
  //endregion
}
