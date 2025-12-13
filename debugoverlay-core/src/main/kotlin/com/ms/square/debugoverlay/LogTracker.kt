package com.ms.square.debugoverlay

import com.ms.square.debugoverlay.model.LogEntry
import kotlinx.coroutines.flow.Flow

/**
 * Interface for custom log tracking implementations.
 *
 * Implement this interface to provide logs from a custom source (e.g., Timber)
 * instead of the default system logcat. When a LogTracker is set via
 * [DebugOverlay.configure], the default logcat reading will be stopped.
 *
 * **Security considerations:** Logs emitted through this interface are displayed
 * directly in the debug overlay UI. Implementations should filter or redact
 * sensitive data (PII, credentials, tokens, etc.) before emitting through the
 * [logs] Flow, especially in builds that may be shared with testers or captured
 * in screen recordings.
 *
 * Example usage with Timber extension:
 * ```kotlin
 * Timber.plant(DebugOverlayTimberTree())  // Auto-registers with DebugOverlay
 * ```
 */
public interface LogTracker {
  /**
   * Display name for UI source indicator (e.g., "Timber", "Custom Logger").
   * This is shown in the Log tab to indicate the current log source.
   */
  public val sourceName: String

  /**
   * Flow of log entries from this tracker.
   * Should emit the current list of entries whenever new logs are added.
   */
  public val logs: Flow<List<LogEntry>>
}
