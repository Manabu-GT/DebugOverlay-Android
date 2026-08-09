package com.ms.square.debugoverlay.internal.crash

import com.ms.square.debugoverlay.internal.Logger

/**
 * Captures a crash record on an uncaught exception, then always delegates to
 * [previousHandler] so the app's normal crash behavior (and any other installed crash
 * reporter, e.g. Crashlytics) is unaffected.
 *
 * Installed once by [com.ms.square.debugoverlay.DebugOverlay.install]. [captureCrash] must
 * be non-suspending and should only read in-memory data to complete quickly before the process dies.
 *
 * @param previousHandler The handler that was installed before this one, captured once
 *   at install time. Never re-fetched, so a handler installed by another SDK *after*
 *   DebugOverlay is never clobbered.
 * @param captureCrash Builds and persists the crash record for the given thread/throwable.
 */
internal class CrashHandler(
  private val previousHandler: Thread.UncaughtExceptionHandler?,
  private val captureCrash: (Thread, Throwable) -> Unit,
) : Thread.UncaughtExceptionHandler {

  @Suppress("TooGenericExceptionCaught")
  override fun uncaughtException(thread: Thread, throwable: Throwable) {
    try {
      captureCrash(thread, throwable)
    } catch (t: Throwable) {
      // Deliberately broad: capture failure must never prevent the delegate call below
      // from running, since that's what keeps other crash reporters (e.g. Crashlytics)
      // and the platform's own crash handling working.
      runCatching { Logger.e("CrashHandler failed to capture crash record", t) }
    } finally {
      // previousHandler is effectively always non-null on real devices — the platform
      // installs its own default handler before any app code runs, well before
      // DebugOverlay.install() (see class doc). If it's ever null, the crash record above
      // is already persisted; there's nothing more this library needs to do.
      previousHandler?.uncaughtException(thread, throwable)
    }
  }
}
