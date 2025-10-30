package com.ms.square.debugoverlay.sample.data.model

/**
 * Represents a feed item from an RSS feed.
 *
 * @property id The item identifier/number
 * @property title The item title
 * @property url The URL to the item page
 * @property publishDate The publication date
 * @property description A brief description or summary of the item
 */
data class FeedItem(val id: Int, val title: String, val url: String, val publishDate: String, val description: String)
