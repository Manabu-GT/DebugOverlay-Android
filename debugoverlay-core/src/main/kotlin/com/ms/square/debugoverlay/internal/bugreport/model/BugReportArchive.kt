package com.ms.square.debugoverlay.internal.bugreport.model

import com.ms.square.debugoverlay.internal.bugreport.IntentShareExporter
import java.io.File
import java.io.InputStream

/**
 * Provides access to a generated bug report archive (ZIP file).
 *
 * This interface exposes bug report data without giving direct file access,
 * preventing exporters from accidentally deleting or modifying the source file.
 * Multiple exporters can safely access the same report concurrently.
 *
 * ## Usage in Custom Exporters
 * ```kotlin
 * class JiraExporter(
 *   private val client: OkHttpClient,
 *   private val jiraBaseUrl: String,
 *   private val projectKey: String
 * ) : BugReportExporter {
 *
 *   override suspend fun export(report: BugReportArchive): ExportResult {
 *     // Called on Dispatchers.IO - blocking I/O is safe
 *     val requestBody = MultipartBody.Builder()
 *       .setType(MultipartBody.FORM)
 *       .addFormDataPart(
 *         "file",
 *         report.fileName,
 *         object : RequestBody() {
 *           override fun contentType() = "application/zip".toMediaType()
 *           override fun contentLength() = report.sizeBytes
 *           override fun writeTo(sink: BufferedSink) {
 *             report.openInputStream().use { sink.writeAll(it.source()) }
 *           }
 *         }
 *       )
 *       .build()
 *
 *     val request = Request.Builder()
 *       .url("$jiraBaseUrl/rest/api/2/issue/$projectKey/attachments")
 *       .post(requestBody)
 *       .build()
 *
 *     val success = client.newCall(request).execute().use { it.isSuccessful }
 *     return if (success) ExportResult.Success else ExportResult.Failure()
 *   }
 * }
 * ```
 *
 * @see com.ms.square.debugoverlay.internal.bugreport.BugReportExporter
 */
internal sealed interface BugReportArchive {

  /**
   * File name for the bug report (e.g., "bugreport_2025-01-23_143052.zip").
   *
   * Use this when uploading to servers that require a filename, or when
   * displaying the report name to users.
   */
  val fileName: String

  /**
   * Size of the bug report in bytes.
   *
   * **Important for uploads**: Some servers (S3, Jira, etc.) reject chunked
   * transfer encoding and require `Content-Length` header. Use this value
   * to set the content length in your upload request body for such cases.
   *
   * ```kotlin
   * override fun contentLength() = report.sizeBytes
   * ```
   */
  val sizeBytes: Long

  /**
   * Opens a new [java.io.InputStream] for reading the bug report data.
   *
   * Each call returns an independent stream positioned at the beginning.
   * Multiple exporters can safely call this concurrently. The caller is
   * responsible for closing the returned stream.
   *
   * ```kotlin
   * report.openInputStream().use { inputStream ->
   *   // Read data...
   * }
   * ```
   *
   * @return A new InputStream for reading the bug report ZIP
   * @throws java.io.IOException if the stream cannot be opened
   */
  fun openInputStream(): InputStream
}

/**
 * Internal implementation of [BugReportArchive] that wraps a [File].
 *
 * Exposes [file] for internal use (e.g., [IntentShareExporter] needs it for FileProvider).
 */
internal class BugReportArchiveImpl(val file: File) : BugReportArchive {

  override val fileName: String
    get() = file.name

  override val sizeBytes: Long
    get() = file.length()

  override fun openInputStream(): InputStream = file.inputStream()
}
