package com.ms.square.debugoverlay

import androidx.annotation.AnyThread

/**
 * Optional capability interface for data sources that can clear their
 * accumulated entries. Used by the debug panel's "Clear logs" action so a
 * subsequent bug report contains only entries from the relevant repro window.
 *
 * Any source registered with DebugOverlay (e.g. a [LogSource] or
 * [NetworkRequestSource]) can opt in by additionally implementing this
 * interface:
 *
 * ```kotlin
 * class MyLogSource : LogSource, Clearable {
 *   override fun clear() { /* reset internal buffer */ }
 * }
 * ```
 *
 * Behavior contract: after [clear] returns, subsequent emissions on the
 * source's exposed flow MUST NOT include entries that were present before
 * the call. Implementations may achieve this by resetting an in-memory
 * buffer or by other means.
 *
 * Thread-safety: [clear] MUST be safe to call from any thread.
 */
public interface Clearable {
  @AnyThread
  public fun clear()
}
