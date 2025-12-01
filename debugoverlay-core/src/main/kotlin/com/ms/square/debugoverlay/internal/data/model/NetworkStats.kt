package com.ms.square.debugoverlay.internal.data.model

/**
 * Network statistics.
 */
internal data class NetworkStats(
  val totalDownloaded: Long, // Total bytes downloaded
  val totalUploaded: Long, // Total bytes uploaded
) {
  companion object {
    val INITIAL_VALUE: NetworkStats = NetworkStats(0, 0)
    val UNSUPPORTED: NetworkStats = NetworkStats(-1, -1)
  }
}
