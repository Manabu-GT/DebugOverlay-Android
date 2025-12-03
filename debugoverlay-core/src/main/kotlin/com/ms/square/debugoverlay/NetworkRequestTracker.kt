package com.ms.square.debugoverlay

import com.ms.square.debugoverlay.model.NetworkRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Interface for tracking network requests.
 *
 * This abstraction allows the debug panel to work with any HTTP client
 * (OkHttp, Ktor, Retrofit, custom clients) without being tied to a specific implementation.
 *
 * Usage:
 * ```kotlin
 * // Implement for your HTTP client
 * class MyNetworkTracker : NetworkRequestTracker {
 *     private val _requests = MutableStateFlow<List<NetworkRequest>>(emptyList())
 *     override val requests: Flow<List<NetworkRequest>> = _requests.asStateFlow()
 * }
 *
 * // Use in DebugOverlay
 *  DebugOverlay.config =
 *       DebugOverlay.config.copy(networkRequestTracker = debugOverlayNetworkInterceptor)
 * ```
 */
public interface NetworkRequestTracker {

  /**
   * Flow of network request logs, with newest log at the end.
   *
   * Implementations should:
   * - Emit a new list whenever a request completes
   * - Limit stored requests (e.g., last 100)
   */
  public val requests: Flow<List<NetworkRequest>>
}

/**
 * No-op implementation for when tracking is disabled.
 */
public object NoOpNetworkRequestTracker : NetworkRequestTracker {
  override val requests: Flow<List<NetworkRequest>> = flowOf(emptyList())
}
