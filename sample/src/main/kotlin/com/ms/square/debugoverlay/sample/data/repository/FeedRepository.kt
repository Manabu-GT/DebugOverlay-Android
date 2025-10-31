package com.ms.square.debugoverlay.sample.data.repository

import com.ms.square.debugoverlay.sample.data.model.FeedItem
import com.ms.square.debugoverlay.sample.data.source.AndroidWeeklyDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Repository for managing RSS feed items.
 * Provides a clean API for accessing feed data with in-memory caching.
 */
class FeedRepository(private val dataSource: AndroidWeeklyDataSource = AndroidWeeklyDataSource()) {
  // Simple in-memory cache for feed items list
  private var cachedFeedItems: List<FeedItem>? = null

  /**
   * Fetches the list of feed items.
   *
   * @param refresh If true, forces a network fetch, otherwise uses cached data if available.
   * @return Flow emitting Result containing list of feed items or error
   */
  fun getFeedItems(refresh: Boolean = false): Flow<Result<List<FeedItem>>> = flow {
    val feedItems = cachedFeedItems
    if (!refresh && feedItems != null) {
      // Use cached data if available
      emit(Result.success(feedItems))
    } else {
      cachedFeedItems = null
      val result = dataSource.fetchFeedItems()
      // Update cache on successful fetch
      result.onSuccess { feedItems ->
        cachedFeedItems = feedItems
      }
      emit(result)
    }
  }

  /**
   * Gets a single feed item by ID.
   * Reuses cached feed items list if available, otherwise fetches from network.
   *
   * @param feedItemId The feed item identifier to retrieve
   * @return Flow emitting Result containing the feed item or error
   */
  fun getFeedItemById(feedItemId: Int): Flow<Result<FeedItem?>> = flow {
    val feedItems = cachedFeedItems
    if (feedItems != null) {
      // Use cached data if available
      emit(Result.success(feedItems.find { it.id == feedItemId }))
    } else {
      // Fetch from network if cache is empty
      val result = dataSource.fetchFeedItems()
      result.onSuccess { fetchedFeedItems ->
        cachedFeedItems = fetchedFeedItems
      }
      emit(
        result.map { fetchedFeedItems ->
          fetchedFeedItems.find { it.id == feedItemId }
        }
      )
    }
  }
}
