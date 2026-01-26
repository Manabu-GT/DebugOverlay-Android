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
 * ## Stream Lifecycle
 * - **Repeatable**: Each [openInputStream] call returns a new independent stream
 * - **Ownership**: Whoever opens a stream is responsible for closing it
 * - **Thread-safe**: Multiple concurrent calls are safe
 *
 * ## Usage Example
 * See [BugReportExporter][com.ms.square.debugoverlay.internal.bugreport.BugReportExporter]
 * for an example of creating an issue with the archive attached.
 *
 * ```kotlin
 * // Converting to OkHttp RequestBody for multipart upload
 * fun BugReportArchive.toRequestBody(): RequestBody = object : RequestBody() {
 *   override fun contentType() = "application/zip".toMediaType()
 *   override fun contentLength() = sizeBytes
 *   override fun writeTo(sink: BufferedSink) {
 *     openInputStream().use { sink.writeAll(it.source()) }
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
