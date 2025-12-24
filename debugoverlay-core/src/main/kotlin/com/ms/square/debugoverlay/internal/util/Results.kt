package com.ms.square.debugoverlay.internal.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred

/**
 * Runs [block] catching all exceptions except [CancellationException].
 * This preserves structured concurrency while allowing graceful degradation.
 *
 * @throws CancellationException if the coroutine is cancelled (preserves structured concurrency)
 */
@Suppress("TooGenericExceptionCaught")
internal inline fun <T> runCatchingNonCancellation(block: () -> T): Result<T> = try {
  Result.success(block())
} catch (e: CancellationException) {
  throw e
} catch (e: Exception) {
  Result.failure(e)
}

/**
 * Awaits the [Deferred] value, returning a [Result] that encapsulates success or failure.
 *
 * Useful inside [kotlinx.coroutines.supervisorScope] when you want to await multiple
 * async operations and handle individual failures gracefully. Callers can unpack
 * with [Result.getOrNull], [Result.getOrDefault], or [Result.fold].
 *
 * @throws CancellationException if the coroutine is cancelled (preserves structured concurrency)
 */
internal suspend fun <T> Deferred<T>.awaitCatching(): Result<T> = runCatchingNonCancellation { await() }
