package com.ms.square.debugoverlay.internal.bugreport

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FileNamesTest {

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

  @Test
  fun `customLogSourceFilename lowercases normal source name`() {
    assertThat(FileNames.customLogSourceFilename("Timber")).isEqualTo("timber_logs.json")
  }

  @Test
  fun `customLogSourceFilename strips path separators`() {
    assertThat(FileNames.customLogSourceFilename("../etc")).isEqualTo("etc_logs.json")
    assertThat(FileNames.customLogSourceFilename("foo/bar")).isEqualTo("foobar_logs.json")
    assertThat(FileNames.customLogSourceFilename("foo\\bar")).isEqualTo("foobar_logs.json")
  }

  @Test
  fun `customLogSourceFilename strips leading dots`() {
    assertThat(FileNames.customLogSourceFilename(".foo")).isEqualTo("foo_logs.json")
  }

  @Test
  fun `customLogSourceFilename falls back for dots only`() {
    assertThat(FileNames.customLogSourceFilename("..")).isEqualTo("unknown_logs.json")
    assertThat(FileNames.customLogSourceFilename("...")).isEqualTo("unknown_logs.json")
  }

  @Test
  fun `customLogSourceFilename strips leading dots then keeps rest`() {
    assertThat(FileNames.customLogSourceFilename("...a")).isEqualTo("a_logs.json")
  }

  @Test
  fun `customLogSourceFilename falls back for empty input`() {
    assertThat(FileNames.customLogSourceFilename("")).isEqualTo("unknown_logs.json")
  }

  @Test
  fun `customLogSourceFilename falls back for whitespace only`() {
    assertThat(FileNames.customLogSourceFilename("   ")).isEqualTo("unknown_logs.json")
  }

  @Test
  fun `customLogSourceFilename falls back for special chars only`() {
    assertThat(FileNames.customLogSourceFilename("@#\$%")).isEqualTo("unknown_logs.json")
  }

  @Test
  fun `customLogSourceFilename strips whitespace`() {
    assertThat(FileNames.customLogSourceFilename("My Source")).isEqualTo("mysource_logs.json")
  }

  @Test
  fun `customLogSourceFilename strips unicode characters`() {
    assertThat(FileNames.customLogSourceFilename("Timber📱Logs")).isEqualTo("timberlogs_logs.json")
  }

  @Test
  fun `customLogSourceFilename preserves embedded dots`() {
    assertThat(FileNames.customLogSourceFilename("foo.bar")).isEqualTo("foo.bar_logs.json")
  }

  @Test
  fun `customLogSourceFilename preserves hyphens and underscores`() {
    assertThat(FileNames.customLogSourceFilename("my-custom_logger")).isEqualTo("my-custom_logger_logs.json")
  }

  @Test
  fun `customLogSourceFilename output always passes validateFilename`() {
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
      "My Source Logs"
    )

    edgeCases.forEach { input ->
      val result = FileNames.customLogSourceFilename(input)
      val validationError = validateFilename(result)
      assertThat(validationError)
        .isNull()
    }
  }
}
