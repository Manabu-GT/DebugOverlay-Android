package com.ms.square.debugoverlay.sample.data.repository

/**
 * Singleton provider for FeedRepository.
 * Ensures a single shared instance across the app for proper caching.
 */
object FeedRepositoryProvider {
  val repository: FeedRepository by lazy {
    FeedRepository()
  }
}
