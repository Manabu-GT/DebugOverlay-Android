package com.ms.square.debugoverlay.internal.bugreport

import android.content.Context
import android.graphics.Bitmap
import com.ms.square.debugoverlay.internal.Logger
import com.ms.square.debugoverlay.internal.util.formatFilenameTimestamp
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
 */
internal class BugReportZipWriter(context: Context) {

  private val cacheDir by lazy {
    File(context.cacheDir, CACHE_SUBDIR).apply { mkdirs() }
  }

  /**
   * Creates a ZIP file containing the bug report data.
   *
   * @param data The collected bug report data
   * @return The created ZIP file
   * @throws java.io.IOException if writing fails
   */
  fun write(data: BugReportData): File {
    val timestamp = formatFilenameTimestamp(data.timestampMs)
    val zipFile = File(cacheDir, "bug_report_$timestamp.zip")

    ZipOutputStream(FileOutputStream(zipFile).buffered()).use { zip ->
      // HTML report (human-readable with embedded screenshot)
      writeFileEntry(zip, "bug_report.html") { file ->
        HtmlReportBuilder.build(data, file)
      }

      // Screenshot (PNG)
      data.screenshot?.let { bitmap ->
        writeScreenshot(zip, bitmap)
      }

      // User-provided metadata (optional)
      data.userMetadata?.let { metadata ->
        writeFileEntry(zip, "user_input.json") { file ->
          BugReportFileWriters.writeUserMetadata(metadata, file)
        }
      }

      // JSON files
      writeFileEntry(zip, "logs.json") { file ->
        BugReportFileWriters.writeLogs(data.logs, file)
      }

      writeFileEntry(zip, "network_requests.json") { file ->
        BugReportFileWriters.writeNetworkRequests(data.networkRequests, file)
      }

      data.deviceInfo?.let {
        writeFileEntry(zip, "device_info.json") { file ->
          BugReportFileWriters.writeDeviceInfo(it, file)
        }
      }

      data.jankStats?.let {
        writeFileEntry(zip, "jank_stats.json") { file ->
          BugReportFileWriters.writeJankStats(it, file)
        }
      }

      // Plain text files
      writeFileEntry(zip, "app_exits.txt") { file ->
        BugReportFileWriters.writeAppExits(data.appExitInfos, file)
      }

      data.uiHierarchy?.let {
        writeFileEntry(zip, "ui_hierarchy.txt") { file ->
          BugReportFileWriters.writeUiHierarchy(it, file)
        }
      }
    }

    Logger.d("Bug report ZIP created: ${zipFile.absolutePath} (${zipFile.length()} bytes)")
    return zipFile
  }

  private fun writeScreenshot(zip: ZipOutputStream, bitmap: Bitmap) {
    zip.putNextEntry(ZipEntry("screenshot.png"))
    bitmap.compress(Bitmap.CompressFormat.PNG, UNUSED_PNG_QUALITY, zip)
    zip.closeEntry()
  }

  private inline fun writeFileEntry(zip: ZipOutputStream, filename: String, writeContent: (File) -> Unit) {
    // Write to temp file first, then add to ZIP
    val tempFile = File.createTempFile("bugreport_", ".tmp", cacheDir)
    try {
      writeContent(tempFile)
      zip.putNextEntry(ZipEntry(filename))
      tempFile.inputStream().buffered().use { input ->
        input.copyTo(zip)
      }
      zip.closeEntry()
    } catch (e: IOException) {
      // Skip failed entries - partial report is better than none
      Logger.w("Failed to write '$filename' to bug report, skipping: ${e.message}")
    } finally {
      if (!tempFile.delete()) {
        Logger.w("Failed to delete temp file: ${tempFile.absolutePath}")
      }
    }
  }
}
