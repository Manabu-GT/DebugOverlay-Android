package com.ms.square.debugoverlay

import com.ms.square.debugoverlay.model.NetworkRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Interface for providing network requests to the debug overlay.
 *
 * This abstraction allows the debug panel to work with any HTTP client
 * (OkHttp, Ktor, Retrofit, custom clients) without being tied to a specific implementation.
 *
 * Usage:
 * ```kotlin
 * // Implement for your HTTP client
 * class MyNetworkSource : NetworkRequestSource {
 *     private val _requests = MutableStateFlow<List<NetworkRequest>>(emptyList())
 *     override val requests: Flow<List<NetworkRequest>> = _requests.asStateFlow()
 * }
 *
 * // Register with DebugOverlay
 * DebugOverlay.configure { copy(networkRequestSource = myNetworkSource) }
 * ```
 */
public interface NetworkRequestSource {

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
 * No-op implementation for when network request tracking is disabled.
 */
public object NoOpNetworkRequestSource : NetworkRequestSource {
  override val requests: Flow<List<NetworkRequest>> = flowOf(emptyList())
}
