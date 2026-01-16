package com.ms.square.debugoverlay.internal.bugreport

import android.content.Context
import com.ms.square.debugoverlay.internal.Logger
import com.ms.square.debugoverlay.internal.bugreport.FileNames.HTML_REPORT
import com.ms.square.debugoverlay.internal.bugreport.FileNames.METADATA
import com.ms.square.debugoverlay.internal.bugreport.model.BugReportMetadata
import com.ms.square.debugoverlay.internal.bugreport.model.BugReportState
import com.ms.square.debugoverlay.internal.bugreport.model.UserInput
import com.ms.square.debugoverlay.internal.util.checkFolderExists
import com.ms.square.debugoverlay.internal.util.formatFilenameTimestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private const val CACHE_SUBDIR = "debugoverlay_bugreports"
private const val UUID_SUFFIX_LENGTH = 8
private const val MAX_ZIP_FILES = 3
internal const val UNUSED_PNG_QUALITY = 100 // PNG is lossless, quality is ignored

/**
 * Creates ZIP archives containing bug report data.
 *
 * Output filename format: `bug_report_YYYYMMDD_HHmmss_<uuid8>.zip`
 *
 * Contents:
 * - bug_report.html (human-readable report with embedded screenshot)
 * - screenshot.png (if available)
 * - logcat_logs.json (system logcat entries)
 * - {source}_logs.json (custom log source logs, e.g., timber_logs.json, if registered)
 * - network_requests.json
 * - device_info.json
 * - jank_stats.json
 * - app_exits.txt
 * - ui_hierarchy.txt
 * - metadata.json (contains capturedAt, userInput, etc.)
 */
internal class BugReportZipWriter(context: Context) {

  private val json = Json { ignoreUnknownKeys = true }

  private val cacheDir by lazy {
    File(context.cacheDir, CACHE_SUBDIR).also {
      it.checkFolderExists()
    }
  }

  /**
   * Creates a ZIP file from a capture folder containing pre-saved bug report files.
   *
   * @param folder The capture folder containing screenshot.png, bug_report.html, etc.
   * @param userInput Optional user-provided title and description
   * @return The created ZIP file
   * @throws IOException if writing fails
   */
  suspend fun writeFromFolder(folder: File, userInput: UserInput?): File = withContext(Dispatchers.IO) {
    val timestampMs = readTimestampFromMetadata(folder)
    val uniqueSuffix = UUID.randomUUID().toString().take(UUID_SUFFIX_LENGTH)
    val zipFile = File(cacheDir, "bug_report_${formatFilenameTimestamp(timestampMs)}_$uniqueSuffix.zip")

    ZipOutputStream(FileOutputStream(zipFile).buffered()).use { zip ->
      // Copy all files from capture folder, injecting userInput into HTML
      val files = folder.listFiles()
        ?: throw IOException(
          "Failed to list capture folder contents at ${folder.absolutePath}. " +
            "Folder may not exist or be inaccessible. This indicates a bug in capture flow."
        )

      files.filter { it.isFile && it.name != METADATA }
        .forEach { file -> copyOrTransformFile(zip, file, userInput) }

      // Write metadata.json with final userInput to ZIP
      writeMetadataToZip(zip, folder, userInput)
    }

    Logger.d("Bug report ZIP created from folder: ${zipFile.absolutePath} (${zipFile.length()} bytes)")
    cleanupOldZips()
    zipFile
  }

  /**
   * Writes updated metadata.json to ZIP with state set to SUBMITTED.
   */
  private fun writeMetadataToZip(zip: ZipOutputStream, folder: File, userInput: UserInput?) {
    val metadataFile = File(folder, METADATA)
    val existingMetadata = if (metadataFile.exists()) {
      runCatching {
        json.decodeFromString(BugReportMetadata.serializer(), metadataFile.readText())
      }.getOrNull()
    } else {
      null
    }

    val finalMetadata = existingMetadata?.copy(
      state = BugReportState.SUBMITTED,
      userInput = userInput
    ) ?: BugReportMetadata(
      capturedAt = folder.lastModified(),
      state = BugReportState.SUBMITTED,
      userInput = userInput
    )

    try {
      zip.putNextEntry(ZipEntry(METADATA))
      try {
        zip.write(json.encodeToString(BugReportMetadata.serializer(), finalMetadata).toByteArray(Charsets.UTF_8))
      } finally {
        zip.closeEntry()
      }
    } catch (e: IOException) {
      Logger.w("Failed to write metadata.json to ZIP: ${e.message}")
    }
  }

  /**
   * Reads the capturedAt timestamp from the folder's metadata.json.
   * Falls back to lastModified if metadata cannot be read.
   */
  private fun readTimestampFromMetadata(folder: File): Long {
    val metadataFile = File(folder, METADATA)
    if (!metadataFile.exists()) {
      Logger.w("metadata.json not found in ${folder.absolutePath}, using lastModified time")
      return folder.lastModified()
    }

    return runCatching {
      val metadata = json.decodeFromString(BugReportMetadata.serializer(), metadataFile.readText())
      metadata.capturedAt
    }.getOrElse { e ->
      Logger.w("Failed to read metadata.json: ${e.message}, using folder's lastModified time")
      folder.lastModified()
    }
  }

  private fun copyOrTransformFile(zip: ZipOutputStream, file: File, userInput: UserInput?) {
    if (file.name == HTML_REPORT) {
      writeHtmlWithUserInput(zip, file, userInput)
    } else {
      copyFileToZip(zip, file, file.name)
    }
  }

  /**
   * Writes HTML file to ZIP with placeholders replaced.
   * If userInput is provided, uses user values; otherwise injects defaults.
   */
  private fun writeHtmlWithUserInput(zip: ZipOutputStream, htmlFile: File, userInput: UserInput?) {
    try {
      val originalHtml = htmlFile.readText()
      val modifiedHtml = if (userInput != null) {
        HtmlReportBuilder.injectUserInput(originalHtml, userInput)
      } else {
        HtmlReportBuilder.injectDefaults(originalHtml)
      }

      zip.putNextEntry(ZipEntry(HTML_REPORT))
      try {
        zip.write(modifiedHtml.toByteArray(Charsets.UTF_8))
      } finally {
        zip.closeEntry()
      }
    } catch (e: IOException) {
      Logger.w("Failed to write HTML with user input, copying original: ${e.message}")
      copyFileToZip(zip, htmlFile, HTML_REPORT)
    }
  }

  private fun copyFileToZip(zip: ZipOutputStream, file: File, entryName: String) {
    try {
      zip.putNextEntry(ZipEntry(entryName))
      try {
        file.inputStream().buffered().use { input ->
          input.copyTo(zip)
        }
      } finally {
        zip.closeEntry()
      }
    } catch (e: IOException) {
      Logger.w("Failed to copy '$entryName' to ZIP, skipping: ${e.message}")
    }
  }

  /**
   * Deletes oldest ZIP files to maintain [MAX_ZIP_FILES] limit.
   * Called after successful ZIP creation. Keeps the most recent files.
   */
  private fun cleanupOldZips() {
    val zipFiles = cacheDir.listFiles { file -> file.isFile && file.extension == "zip" }
      ?.sortedByDescending { it.lastModified() }
      ?: return

    if (zipFiles.size <= MAX_ZIP_FILES) return

    zipFiles.drop(MAX_ZIP_FILES).forEach { file ->
      if (file.delete()) {
        Logger.d("Deleted old ZIP: ${file.name}")
      } else {
        Logger.w("Failed to delete old ZIP: ${file.name}")
      }
    }
  }
}
