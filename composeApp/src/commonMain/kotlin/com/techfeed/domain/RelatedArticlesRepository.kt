package com.techfeed.domain

import com.techfeed.data.model.FeedItem
import com.techfeed.data.remote.FeedApiClient

/**
 * Backs the "Related Articles" section at the bottom of ReadModeScreen.
 * Calls the backend's tag-overlap endpoint (GET /api/posts/{id}/related)
 */
class RelatedArticlesRepository(
    private val api: FeedApiClient,
) {
    suspend fun getRelated(item: FeedItem, limit: Int = 5): List<FeedItem> =
        api.getRelated(item.id, limit).getOrDefault(emptyList())
}
