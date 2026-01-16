package com.ms.square.debugoverlay.internal.bugreport

import com.ms.square.debugoverlay.BugReportDataContributor
import com.ms.square.debugoverlay.internal.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import kotlin.time.Duration.Companion.seconds

internal const val CUSTOM_FILE_PREFIX = "custom_"
private val DEFAULT_TIMEOUT = 5.seconds
private const val SLOW_THRESHOLD_MS = 3000L

private val VALID_FILENAME_REGEX = Regex("""^[a-zA-Z0-9_.-]+$""")

/**
 * Collects data from all registered [BugReportDataContributor]s.
 *
 * Each contributor runs with a timeout; failures are logged but don't fail
 * the collection.
 *
 * Contributors are processed **sequentially** by design:
 * - Predictable resource usage (one file write at a time)
 * - Simpler debugging when issues occur
 * - Avoids concurrent filesystem writes
 *
 * @param contributors List of registered contributors
 * @param outputFolder Folder to write custom files to
 * @return List of successfully written filenames (with custom_ prefix)
 */
internal suspend fun collectCustomData(
  contributors: List<BugReportDataContributor>,
  outputFolder: File,
): List<String> = withContext(Dispatchers.IO) {
  contributors.mapNotNull { contributor ->
    collectFromContributor(contributor, outputFolder)
  }
}

// Contributors are external plugin code - we don't control what exceptions they throw
@Suppress("TooGenericExceptionCaught")
private suspend fun collectFromContributor(contributor: BugReportDataContributor, outputFolder: File): String? {
  val rawFilename = contributor.filename

  // Validate filename
  val validationError = validateFilename(rawFilename)
  if (validationError != null) {
    Logger.w("Skipping contributor: $validationError (filename='$rawFilename')")
    return null
  }

  // Prefix with "custom_" to avoid collisions with built-in files
  val safeFilename = "$CUSTOM_FILE_PREFIX$rawFilename"
  val outputFile = File(outputFolder, safeFilename)

  val startTime = System.currentTimeMillis()

  return try {
    withTimeout(DEFAULT_TIMEOUT) {
      outputFile.outputStream().buffered().use { stream ->
        contributor.writeTo(stream)
      }
    }

    val elapsed = System.currentTimeMillis() - startTime
    val fileSize = outputFile.length()

    if (elapsed > SLOW_THRESHOLD_MS) {
      Logger.w("Contributor '$rawFilename' took ${elapsed}ms (consider optimization)")
    }

    Logger.d("Custom data collected: $safeFilename ($fileSize bytes, ${elapsed}ms)")
    safeFilename
  } catch (_: TimeoutCancellationException) {
    Logger.w("Contributor timed out after $DEFAULT_TIMEOUT: $rawFilename")
    outputFile.delete()
    null
  } catch (e: CancellationException) {
    // Rethrow to preserve structured concurrency - scope was cancelled externally
    outputFile.delete()
    throw e
  } catch (e: Exception) {
    Logger.w("Contributor failed: $rawFilename", e)
    outputFile.delete()
    null
  }
}

/**
 * Validates a contributor filename.
 * @return Error message if invalid, null if valid
 */
private fun validateFilename(filename: String): String? {
  if (filename.isBlank()) {
    return "Filename cannot be blank"
  }

  if (!VALID_FILENAME_REGEX.matches(filename)) {
    return "Filename contains invalid characters (allowed: a-z, A-Z, 0-9, _, ., -)"
  }

  if (filename.startsWith('.')) {
    return "Filename cannot start with '.'"
  }

  return null
}
