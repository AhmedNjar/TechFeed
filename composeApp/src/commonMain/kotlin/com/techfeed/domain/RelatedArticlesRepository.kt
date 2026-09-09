package com.techfeed.domain

import com.techfeed.data.model.FeedItem
import com.techfeed.data.remote.FeedApiClient

/**
 * Backs the "Related Articles" section at the bottom of ReadModeScreen.
 * Calls the backend's tag-overlap endpoint (GET /api/posts/{id}/related)
 * rather than trying to compute relatedness from the local SQLDelight cache,
 * since the cache only holds whatever pages the user has scrolled through —
 * the backend has the full corpus.
 */
class RelatedArticlesRepository(
    private val api: FeedApiClient,
) {
    suspend fun getRelated(item: FeedItem, limit: Int = 5): List<FeedItem> =
        api.getRelated(item.id, limit).getOrDefault(emptyList())
}
