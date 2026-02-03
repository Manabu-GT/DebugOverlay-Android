package com.ms.square.debugoverlay.model

import java.io.InputStream

/**
 * Provides access to a generated bug report archive (ZIP file).
 *
 * This interface exposes bug report data without giving direct file access,
 * preventing exporters from accidentally deleting or modifying the source file.
 * Multiple exporters can safely access the same report concurrently.
 *
 * **Lifecycle**: An archive is valid only within the scope of
 * [BugReportExporter.export][com.ms.square.debugoverlay.BugReportExporter.export] —
 * do not retain references beyond that call.
 *
 * ## Stream Lifecycle
 * - **Repeatable**: Each [openInputStream] call returns a new independent stream
 * - **Ownership**: Whoever opens a stream is responsible for closing it
 * - **Thread-safe**: Multiple concurrent calls are safe
 *
 * ## Usage Example
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
 */
public interface BugReportArchive {

  /**
   * File name for the bug report (e.g., "bugreport_2025-01-23_143052.zip").
   *
   * Use this when uploading to servers that require a filename, or when
   * displaying the report name to users.
   */
  public val fileName: String

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
  public val sizeBytes: Long

  /**
   * Opens a new [InputStream] for reading the bug report data.
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
  public fun openInputStream(): InputStream
}
