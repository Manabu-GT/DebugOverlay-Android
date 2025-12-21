package com.ms.square.debugoverlay.internal.bugreport

import android.content.Context
import com.ms.square.debugoverlay.internal.Logger
import com.ms.square.debugoverlay.internal.bugreport.FileNames.HTML_REPORT
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private const val CACHE_SUBDIR = "debugoverlay_bugreports"
internal const val UNUSED_PNG_QUALITY = 100 // PNG is lossless, quality is ignored

/**
 * Creates ZIP archives containing bug report data.
 *
 * Output filename format: `bug_report_YYYYMMDD_HHmmss.zip`
 *
 * Contents:
 * - bug_report.html (human-readable report with embedded screenshot)
 * - screenshot.png (if available)
 * - logs.json
 * - network_requests.json
 * - device_info.json
 * - jank_stats.json
 * - app_exits.txt
 * - ui_hierarchy.txt
 * - user_input.json
 */
internal class BugReportZipWriter(context: Context) {

  private val cacheDir by lazy {
    File(context.cacheDir, CACHE_SUBDIR).apply { mkdirs() }
  }

  /**
   * Creates a ZIP file from a capture folder containing pre-saved bug report files.
   *
   * @param folder The capture folder containing screenshot.png, bug_report.html, etc.
   * @param metadata Optional user-provided title and description
   * @return The created ZIP file
   * @throws IOException if writing fails
   */
  fun writeFromFolder(folder: File, metadata: BugReportMetadata?): File {
    val timestamp = folder.name.removePrefix(TEMP_FOLDER_PREFIX)
    val zipFile = File(cacheDir, "bug_report_$timestamp.zip")

    ZipOutputStream(FileOutputStream(zipFile).buffered()).use { zip ->
      // Copy all files from capture folder, injecting metadata into HTML
      val files = folder.listFiles()
        ?: throw IOException("Cannot list files in folder: ${folder.absolutePath} (may not exist or lack permissions)")

      files.filter { it.isFile }
        .forEach { file -> copyOrTransformFile(zip, file, metadata) }

      // Add user metadata as JSON for machine readability
      if (metadata != null) {
        writeFileEntry(zip, "user_input.json") { tempFile ->
          BugReportFileWriters.writeUserMetadata(metadata, tempFile)
        }
      }
    }

    Logger.d("Bug report ZIP created from folder: ${zipFile.absolutePath} (${zipFile.length()} bytes)")
    return zipFile
  }

  private fun copyOrTransformFile(zip: ZipOutputStream, file: File, metadata: BugReportMetadata?) {
    if (file.name == HTML_REPORT) {
      writeHtmlWithMetadata(zip, file, metadata)
    } else {
      copyFileToZip(zip, file, file.name)
    }
  }

  /**
   * Writes HTML file to ZIP with placeholders replaced.
   * If metadata is provided, uses user values; otherwise injects defaults.
   */
  private fun writeHtmlWithMetadata(zip: ZipOutputStream, htmlFile: File, metadata: BugReportMetadata?) {
    try {
      val originalHtml = htmlFile.readText()
      val modifiedHtml = if (metadata != null) {
        HtmlReportBuilder.injectMetadata(originalHtml, metadata)
      } else {
        HtmlReportBuilder.injectDefaults(originalHtml)
      }

      zip.putNextEntry(ZipEntry(HTML_REPORT))
      zip.write(modifiedHtml.toByteArray(Charsets.UTF_8))
      zip.closeEntry()
    } catch (e: IOException) {
      Logger.w("Failed to write HTML with metadata, copying original: ${e.message}")
      copyFileToZip(zip, htmlFile, HTML_REPORT)
    }
  }

  private fun copyFileToZip(zip: ZipOutputStream, file: File, entryName: String) {
    try {
      zip.putNextEntry(ZipEntry(entryName))
      file.inputStream().buffered().use { input ->
        input.copyTo(zip)
      }
      zip.closeEntry()
    } catch (e: IOException) {
      Logger.w("Failed to copy '$entryName' to ZIP, skipping: ${e.message}")
    }
  }

  private inline fun writeFileEntry(zip: ZipOutputStream, fileName: String, writeContent: (File) -> Unit) {
    // Write to temp file first, then add to ZIP
    val tempFile = File.createTempFile("bugreport_", ".tmp", cacheDir)
    try {
      writeContent(tempFile)
      zip.putNextEntry(ZipEntry(fileName))
      tempFile.inputStream().buffered().use { input ->
        input.copyTo(zip)
      }
      zip.closeEntry()
    } catch (e: IOException) {
      // Skip failed entries - partial report is better than none
      Logger.w("Failed to write '$fileName' to bug report, skipping: ${e.message}")
    } finally {
      if (!tempFile.delete()) {
        Logger.w("Failed to delete temp file: ${tempFile.absolutePath}")
      }
    }
  }
}
