package com.techfeed.domain

import com.techfeed.data.local.FeedLocalDataSource
import com.techfeed.data.model.FeedItem
import com.techfeed.data.model.FeedResponse
import com.techfeed.data.remote.FeedApiClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

private const val CACHE_TTL_MS = 3 * 60 * 60 * 1000L  // 3 hours

class FeedRepository(
    private val api: FeedApiClient,
    private val local: FeedLocalDataSource,
) {
    /**
     * Offline-first flow:
     *  1. Emit cache immediately (if exists and not stale)
     *  2. Fetch from network → update cache → emit fresh data
     *  3. On network failure with empty cache → emit error
     */
    fun getFeed(
        tag: String,
        page: Int,
        forceRefresh: Boolean = false,
    ): Flow<Result<FeedResponse>> = flow {

        val cached = local.getFeedByTagAndPage(tag, page).first()

        if (cached.isNotEmpty() && !forceRefresh) {
            emit(Result.success(cached.toFeedResponse(page)))
        }

        val networkResult = api.getFeed(tag = tag, page = page)

        networkResult.onSuccess { response ->
            if (forceRefresh && page == 0) local.clearTag(tag)
            local.insertFeedItems(response.items, tag, page)
            emit(Result.success(response))
        }.onFailure { err ->
            if (cached.isEmpty()) emit(Result.failure(err))
        }
    }

    fun search(query: String): Flow<List<FeedItem>> = local.search(query)

    suspend fun hideItem(id: String) = local.hideItem(id)

    suspend fun pruneOldCache() = local.pruneStale(CACHE_TTL_MS)
}

// ── Bookmark Repository ───────────────────────────────────────────────────────

class BookmarkRepository(private val local: FeedLocalDataSource) {

    val bookmarks: Flow<List<FeedItem>> = local.getAllBookmarks()
    val count: Flow<Long>               = local.bookmarkCount()

    fun isBookmarked(id: String): Flow<Boolean> = local.isBookmarked(id)

    suspend fun remove(id: String) = local.deleteBookmark(id)

    suspend fun toggle(item: FeedItem) {
        val saved = local.isBookmarked(item.id).first()
        if (saved) local.deleteBookmark(item.id)
        else       local.insertBookmark(item)
    }
}

// ── Helper ────────────────────────────────────────────────────────────────────

private fun List<FeedItem>.toFeedResponse(page: Int) = FeedResponse(
    items      = this,
    page       = page,
    totalPages = Int.MAX_VALUE,
    hasNext    = true,
)
