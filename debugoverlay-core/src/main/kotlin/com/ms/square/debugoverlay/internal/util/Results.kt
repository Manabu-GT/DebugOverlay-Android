package com.ms.square.debugoverlay.internal.util

import kotlinx.coroutines.CancellationException

/**
 * Runs [block] catching all exceptions except [CancellationException].
 * This preserves structured concurrency while allowing graceful degradation.
 */
@Suppress("TooGenericExceptionCaught")
internal inline fun <T> runCatchingNonCancellation(block: () -> T): Result<T> = try {
  Result.success(block())
} catch (e: CancellationException) {
  throw e
} catch (e: Exception) {
  Result.failure(e)
}
