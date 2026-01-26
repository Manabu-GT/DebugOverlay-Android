package com.ms.square.debugoverlay.model

import android.util.Log
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LogLevelTest {

  @Test
  fun `fromString parses all standard priority letters`() {
    assertThat(LogLevel.fromString("V")).isEqualTo(LogLevel.VERBOSE)
    assertThat(LogLevel.fromString("D")).isEqualTo(LogLevel.DEBUG)
    assertThat(LogLevel.fromString("I")).isEqualTo(LogLevel.INFO)
    assertThat(LogLevel.fromString("W")).isEqualTo(LogLevel.WARN)
    assertThat(LogLevel.fromString("E")).isEqualTo(LogLevel.ERROR)
    assertThat(LogLevel.fromString("F")).isEqualTo(LogLevel.ERROR) // FATAL maps to ERROR
  }

  @Test
  fun `fromString returns DEBUG for unknown input`() {
    assertThat(LogLevel.fromString("X")).isEqualTo(LogLevel.DEBUG)
    assertThat(LogLevel.fromString("")).isEqualTo(LogLevel.DEBUG)
    assertThat(LogLevel.fromString("UNKNOWN")).isEqualTo(LogLevel.DEBUG)
  }

  @Test
  fun `fromInt converts all standard priority integers`() {
    assertThat(LogLevel.fromInt(Log.VERBOSE)).isEqualTo(LogLevel.VERBOSE)
    assertThat(LogLevel.fromInt(Log.DEBUG)).isEqualTo(LogLevel.DEBUG)
    assertThat(LogLevel.fromInt(Log.INFO)).isEqualTo(LogLevel.INFO)
    assertThat(LogLevel.fromInt(Log.WARN)).isEqualTo(LogLevel.WARN)
    assertThat(LogLevel.fromInt(Log.ERROR)).isEqualTo(LogLevel.ERROR)
    assertThat(LogLevel.fromInt(Log.ASSERT)).isEqualTo(LogLevel.ERROR) // ASSERT maps to ERROR
  }

  @Test
  fun `fromInt returns DEBUG for unknown inputs`() {
    assertThat(LogLevel.fromInt(Log.VERBOSE - 1)).isEqualTo(LogLevel.DEBUG)
    assertThat(LogLevel.fromInt(Log.ASSERT + 1)).isEqualTo(LogLevel.DEBUG)
  }
}
