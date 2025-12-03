package com.ms.square.debugoverlay.internal.data.model

/**
 * Network statistics.
 */
internal data class NetworkStats(
  val totalDownloaded: Long, // Total bytes downloaded
  val totalUploaded: Long, // Total bytes uploaded
  val totalRequests: Int? = null, // Total number of requests
  val errorCount: Int? = null, // Requests with 4xx/5xx status
  val avgDuration: Long? = null, // Average duration in ms
) {
  companion object {
    val INITIAL_VALUE: NetworkStats = NetworkStats(0, 0)
    val UNSUPPORTED: NetworkStats = NetworkStats(-1, -1)
  }
}
