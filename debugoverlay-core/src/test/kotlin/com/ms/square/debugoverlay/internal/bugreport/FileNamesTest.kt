package com.ms.square.debugoverlay.internal.bugreport

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FileNamesTest {

  // ========== validateFilename tests ==========

  @Test
  fun `validateFilename returns null for valid filename`() {
    assertThat(validateFilename("my_data.json")).isNull()
  }

  @Test
  fun `validateFilename accepts alphanumeric with dots and hyphens`() {
    assertThat(validateFilename("feature-flags_v2.txt")).isNull()
  }

  @Test
  fun `validateFilename returns error for blank filename`() {
    assertThat(validateFilename("")).isEqualTo("Filename cannot be blank")
    assertThat(validateFilename("   ")).isEqualTo("Filename cannot be blank")
  }

  @Test
  fun `validateFilename returns error for path separators`() {
    assertThat(validateFilename("foo/bar.txt"))
      .isEqualTo("Filename contains invalid characters (allowed: a-z, A-Z, 0-9, _, ., -)")
    assertThat(validateFilename("foo\\bar.txt"))
      .isEqualTo("Filename contains invalid characters (allowed: a-z, A-Z, 0-9, _, ., -)")
  }

  @Test
  fun `validateFilename returns error for filename starting with dot`() {
    assertThat(validateFilename(".hidden")).isEqualTo("Filename cannot start with '.'")
    assertThat(validateFilename("..")).isEqualTo("Filename cannot start with '.'")
  }

  @Test
  fun `validateFilename rejects whitespace in filename`() {
    assertThat(validateFilename("my file.json")).isNotNull()
    assertThat(validateFilename("file\tname.json")).isNotNull()
  }

  @Test
  fun `validateFilename rejects unicode characters`() {
    assertThat(validateFilename("file_📱.json")).isNotNull()
    assertThat(validateFilename("файл.json")).isNotNull()
  }

  // ========== trackerLogsFilename tests ==========

  @Test
  fun `trackerLogsFilename lowercases normal source name`() {
    assertThat(FileNames.trackerLogsFilename("Timber")).isEqualTo("timber_logs.json")
  }

  @Test
  fun `trackerLogsFilename strips path separators`() {
    assertThat(FileNames.trackerLogsFilename("../etc")).isEqualTo("etc_logs.json")
    assertThat(FileNames.trackerLogsFilename("foo/bar")).isEqualTo("foobar_logs.json")
    assertThat(FileNames.trackerLogsFilename("foo\\bar")).isEqualTo("foobar_logs.json")
  }

  @Test
  fun `trackerLogsFilename strips leading dots`() {
    assertThat(FileNames.trackerLogsFilename(".foo")).isEqualTo("foo_logs.json")
  }

  @Test
  fun `trackerLogsFilename falls back for dots only`() {
    assertThat(FileNames.trackerLogsFilename("..")).isEqualTo("unknown_logs.json")
    assertThat(FileNames.trackerLogsFilename("...")).isEqualTo("unknown_logs.json")
  }

  @Test
  fun `trackerLogsFilename strips leading dots then keeps rest`() {
    assertThat(FileNames.trackerLogsFilename("...a")).isEqualTo("a_logs.json")
  }

  @Test
  fun `trackerLogsFilename falls back for empty input`() {
    assertThat(FileNames.trackerLogsFilename("")).isEqualTo("unknown_logs.json")
  }

  @Test
  fun `trackerLogsFilename falls back for whitespace only`() {
    assertThat(FileNames.trackerLogsFilename("   ")).isEqualTo("unknown_logs.json")
  }

  @Test
  fun `trackerLogsFilename falls back for special chars only`() {
    assertThat(FileNames.trackerLogsFilename("@#\$%")).isEqualTo("unknown_logs.json")
  }

  @Test
  fun `trackerLogsFilename strips whitespace`() {
    assertThat(FileNames.trackerLogsFilename("My Tracker")).isEqualTo("mytracker_logs.json")
  }

  @Test
  fun `trackerLogsFilename strips unicode characters`() {
    assertThat(FileNames.trackerLogsFilename("Timber📱Logs")).isEqualTo("timberlogs_logs.json")
  }

  @Test
  fun `trackerLogsFilename preserves embedded dots`() {
    assertThat(FileNames.trackerLogsFilename("foo.bar")).isEqualTo("foo.bar_logs.json")
  }

  @Test
  fun `trackerLogsFilename preserves hyphens and underscores`() {
    assertThat(FileNames.trackerLogsFilename("my-custom_logger")).isEqualTo("my-custom_logger_logs.json")
  }

  // ========== Contract test: trackerLogsFilename output must pass validateFilename ==========

  @Test
  fun `trackerLogsFilename output always passes validateFilename`() {
    val edgeCases = listOf(
      "Normal",
      "../../../etc",
      "...dots",
      "@#\$%^&*()",
      "",
      "   ",
      "path/with/slashes",
      "🔥🔥🔥",
      ".hidden",
      "..",
      "foo\\bar",
      "My Tracker Logs"
    )

    edgeCases.forEach { input ->
      val result = FileNames.trackerLogsFilename(input)
      val validationError = validateFilename(result)
      assertThat(validationError)
        .isNull()
    }
  }
}
