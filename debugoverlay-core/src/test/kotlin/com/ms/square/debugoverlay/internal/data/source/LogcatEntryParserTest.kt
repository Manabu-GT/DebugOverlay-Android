package com.ms.square.debugoverlay.internal.data.source

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.ms.square.debugoverlay.model.LogLevel
import org.junit.Test

class LogcatEntryParserTest {

  // Simple thread name resolver for testing - just returns "Thread-{tid}"
  private val simpleThreadNameResolver: (Int) -> String = { tid -> "Thread-$tid" }

  private val parser = LogcatEntryParser(ThreadNameCache(threadNameResolver = simpleThreadNameResolver))

  @Test
  fun `parse valid logcat line extracts all fields correctly`() {
    val line = "1733921286.215 11744 11744 D MyTag: Hello world"
    val result = parser.parse(line)

    assertThat(result).isNotNull()
    with(result!!) {
      assertThat(timestampMs).isEqualTo(1733921286215L)
      assertThat(level).isEqualTo(LogLevel.DEBUG)
      assertThat(tag).isEqualTo("MyTag")
      assertThat(message).isEqualTo("Hello world")
      assertThat(pid).isEqualTo(11744)
      assertThat(tid).isEqualTo(11744)
      assertThat(threadName).isEqualTo("main")
    }
  }

  @Test
  fun `parse assigns different thread name when pid and tid differ`() {
    val line = "1733921286.215 11744 11745 I NetworkService: Connection established"
    val result = parser.parse(line)

    assertThat(result).isNotNull()
    with(result!!) {
      assertThat(pid).isEqualTo(11744)
      assertThat(tid).isEqualTo(11745)
      assertThat(threadName).isEqualTo("Thread-11745")
    }
  }

  @Test
  fun `parse handles whitespace variations`() {
    // Leading and trailing whitespace
    val lineWithPadding = "   1733921286.215 11744 11744 D MyTag: Hello world   "
    val result1 = parser.parse(lineWithPadding)
    assertThat(result1).isNotNull()
    assertThat(result1?.tag).isEqualTo("MyTag")

    // Extra whitespace between fields
    val lineWithExtraSpaces = "1733921286.215   11744   11744   D   MyTag:   Hello world"
    val result2 = parser.parse(lineWithExtraSpaces)
    assertThat(result2).isNotNull()
    assertThat(result2?.message).isEqualTo("Hello world")
  }

  @Test
  fun `parse handles special characters in tag and message`() {
    // Colon in message
    val lineWithColon = "1733921286.215 11744 11744 D MyTag: key: value: nested"
    val result1 = parser.parse(lineWithColon)
    assertThat(result1).isNotNull()
    assertThat(result1?.message).isEqualTo("key: value: nested")

    // Package name as tag
    val lineWithPackage = "1733921286.215 11744 11744 D com.example.MyClass: Test message"
    val result2 = parser.parse(lineWithPackage)
    assertThat(result2).isNotNull()
    assertThat(result2?.tag).isEqualTo("com.example.MyClass")

    // Unicode in message
    val lineWithUnicode = "1733921286.215 11744 11744 D Tag: Hello 世界"
    val result3 = parser.parse(lineWithUnicode)
    assertThat(result3).isNotNull()
    assertThat(result3?.message).isEqualTo("Hello 世界")
  }

  @Test
  fun `parse returns null for invalid lines`() {
    val invalidLines = listOf(
      "" to "empty line",
      "   " to "whitespace only",
      "11744 11744 D MyTag: Hello world" to "missing timestamp",
      "1733921286.215 11744 11744 MyTag: Hello world" to "missing level",
      "1733921286.215 11744 11744 X MyTag: Hello world" to "invalid level character",
      "1733921286.215 11744 11744 D MyTag Hello world" to "missing colon separator",
      "1733921286.215 abc 11744 D MyTag: Hello world" to "non-numeric pid",
      "1733921286.215 11744 abc D MyTag: Hello world" to "non-numeric tid",
      "--------- beginning of main" to "logcat header line",
      "This is not a logcat line at all" to "random text"
    )

    invalidLines.forEach { (line, description) ->
      val result = parser.parse(line)
      assertWithMessage(description).that(result).isNull()
    }
  }
}
