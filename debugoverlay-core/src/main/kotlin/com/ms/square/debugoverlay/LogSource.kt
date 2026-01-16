package com.ms.square.debugoverlay

import com.ms.square.debugoverlay.model.LogEntry
import kotlinx.coroutines.flow.Flow

/**
 * Interface for custom log source implementations.
 *
 * Implement this interface to provide logs from a custom source (e.g., Timber)
 * as an additional tab alongside the built-in Logcat tab. When a LogSource is
 * set via [DebugOverlay.configure], a second log tab appears in the debug panel
 * with the [sourceName] as the tab title.
 *
 * **Security considerations:** Logs emitted through this interface are displayed
 * directly in the debug overlay UI. Implementations should filter or redact
 * sensitive data (PII, credentials, tokens, etc.) before emitting through the
 * [logs] Flow, especially in builds that may be shared with testers or captured
 * in screen recordings.
 *
 * Example usage with Timber extension (auto-registers via AndroidX Startup):
 * ```kotlin
 * debugImplementation("com.ms-square:debugoverlay-extension-timber:x.x.x")
 * ```
 */
public interface LogSource {
  /**
   * Display name for UI source indicator (e.g., "Timber", "Custom Logger").
   * This is shown in the Log tab to indicate the current log source.
   */
  public val sourceName: String

  /**
   * Flow of log entries from this source.
   * Should emit the current list of entries whenever new logs are added.
   */
  public val logs: Flow<List<LogEntry>>
}
