package com.ms.square.debugoverlay.internal.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class JsonFormattingTest {

  @Test
  fun `formatJsonIfPossible pretty-prints compact JSON object`() {
    val input = """{"name":"test","count":2}"""

    val result = formatJsonIfPossible(input)

    assertThat(result).isEqualTo(
      """
      {
          "name": "test",
          "count": 2
      }
      """.trimIndent()
    )
  }

  @Test
  fun `formatJsonIfPossible returns input unchanged when not valid JSON`() {
    val input = "plain text response, not json at all"

    assertThat(formatJsonIfPossible(input)).isEqualTo(input)
  }

  @Test
  fun `formatJsonIfPossible returns input unchanged for malformed JSON`() {
    val input = """{"name":"test", "unterminated"""

    assertThat(formatJsonIfPossible(input)).isEqualTo(input)
  }

  @Test
  fun `formatJsonIfPossible returns empty string unchanged`() {
    assertThat(formatJsonIfPossible("")).isEqualTo("")
  }
}
