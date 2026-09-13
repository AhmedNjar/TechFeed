package com.techfeed.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.techfeed.data.model.ContentBlock
import com.techfeed.data.model.FeedItem
import com.techfeed.data.model.MediaItem
import com.techfeed.db.TechFeedDb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class FeedLocalDataSource(db: TechFeedDb) {

    private val feedQueries        = db.feedItemQueries
    private val bookmarkQueries    = db.bookmarkQueries
    private val readHistoryQueries = db.readHistoryQueries
    private val hiddenQueries      = db.hiddenItemQueries
    private val json               = Json { ignoreUnknownKeys = true }

    // ── Feed Cache ────────────────────────────────────────────────────────────

    fun getFeedByTagAndPage(tag: String, page: Int): Flow<List<FeedItem>> =
        feedQueries.selectByTagAndPage(tag, page.toLong())
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows -> rows.map { it.toFeedItem(json) } }

    suspend fun insertFeedItems(
        items: List<FeedItem>,
        tag: String,
        page: Int,
    ) = withContext(Dispatchers.IO) {
        feedQueries.transaction {
            items.forEach { item ->
                feedQueries.insertOrReplace(
                    id                = item.id,
                    title             = item.title,
                    content_blocks    = json.encodeToString(item.contentBlocks),
                    summary           = item.summary,
                    clean_body_text   = item.cleanBodyText,
                    url               = item.url,
                    source            = item.source,
                    tags              = item.tags.joinToString(","),
                    image_url         = item.imageUrl,
                    media_list        = json.encodeToString(item.mediaList),
                    article_url       = item.articleUrl,
                    comments_url      = item.commentsUrl,
                    author            = item.author,
                    read_time_minutes = item.readTimeMinutes.toLong(),
                    filter_tag        = tag,
                    page              = page.toLong(),
                    cached_at         = currentTimeMillis(),
                )
            }
        }
    }

    suspend fun clearTag(tag: String) = withContext(Dispatchers.IO) {
        feedQueries.deleteByTag(tag)
    }

    suspend fun pruneStale(olderThanMillis: Long) = withContext(Dispatchers.IO) {
        feedQueries.deleteStale(currentTimeMillis() - olderThanMillis)
    }

    fun search(query: String): Flow<List<FeedItem>> =
        feedQueries.search(query)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows -> rows.map { it.toFeedItem(json) } }

    fun getCachedByTagKeyword(keyword: String, limit: Long): Flow<List<FeedItem>> =
        feedQueries.selectByTagKeyword(keyword, limit)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows -> rows.map { it.toFeedItem(json) } }

    suspend fun hideItem(id: String) = withContext(Dispatchers.IO) {
        hiddenQueries.insert(id)
    }

    // ── Bookmarks ─────────────────────────────────────────────────────────────

    fun getAllBookmarks(): Flow<List<FeedItem>> =
        bookmarkQueries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows -> rows.map { it.toFeedItem(json) } }

    fun isBookmarked(id: String): Flow<Boolean> =
        bookmarkQueries.isBookmarked(id)
            .asFlow()
            .map { query -> query.executeAsOne() }

    suspend fun insertBookmark(item: FeedItem) = withContext(Dispatchers.IO) {
        bookmarkQueries.insert(
            id                = item.id,
            title             = item.title,
                    content_blocks    = json.encodeToString(item.contentBlocks),
                    summary           = item.summary,
                    clean_body_text   = item.cleanBodyText,
                    url               = item.url,
            source            = item.source,
            tags              = item.tags.joinToString(","),
                    image_url         = item.imageUrl,
                    media_list        = json.encodeToString(item.mediaList),
                    article_url       = item.articleUrl,
                    comments_url      = item.commentsUrl,
                    author            = item.author,
                    read_time_minutes = item.readTimeMinutes.toLong(),
            saved_at          = currentTimeMillis(),
        )
    }

    suspend fun deleteBookmark(id: String) = withContext(Dispatchers.IO) {
        bookmarkQueries.deleteById(id)
    }

    fun bookmarkCount(): Flow<Long> =
        bookmarkQueries.count()
            .asFlow()
            .map { it.executeAsOne() }

    // ── Read History ─────────────────────────────────────────────────────────

    suspend fun recordRead(item: FeedItem) = withContext(Dispatchers.IO) {
        readHistoryQueries.recordRead(
            id                = item.id,
            title             = item.title,
                    content_blocks    = json.encodeToString(item.contentBlocks),
                    summary           = item.summary,
                    clean_body_text   = item.cleanBodyText,
                    url               = item.url,
            source            = item.source,
            tags              = item.tags.joinToString(","),
                    image_url         = item.imageUrl,
                    media_list        = json.encodeToString(item.mediaList),
                    article_url       = item.articleUrl,
                    comments_url      = item.commentsUrl,
                    author            = item.author,
                    read_time_minutes = item.readTimeMinutes.toLong(),
            read_at           = currentTimeMillis(),
        )
    }

    fun recentlyRead(limit: Long): Flow<List<FeedItem>> =
        readHistoryQueries.selectRecent(limit)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows -> rows.map { it.toFeedItem(json) } }

    fun readCount(): Flow<Long> =
        readHistoryQueries.countAll()
            .asFlow()
            .map { it.executeAsOne() }

    fun allReadTimestamps(): Flow<List<Long>> =
        readHistoryQueries.allTimestamps()
            .asFlow()
            .mapToList(Dispatchers.IO)

    fun allReadTags(): Flow<List<String>> =
        readHistoryQueries.allTags()
            .asFlow()
            .mapToList(Dispatchers.IO)
}

// ── Mappers ───────────────────────────────────────────────────────────────────

private fun com.techfeed.db.Feed_item.toFeedItem(json: Json) = FeedItem(
    id               = id,
    title            = title,
    contentBlocks    = runCatching {
        json.decodeFromString<List<ContentBlock>>(content_blocks)
    }.getOrDefault(emptyList()),
    summary          = summary,
    cleanBodyText    = clean_body_text,
    url              = url,
    source           = source,
    tags             = tags.split(",").filter { it.isNotBlank() },
    imageUrl         = image_url,
    mediaList        = runCatching { json.decodeFromString<List<MediaItem>>(media_list) }.getOrDefault(emptyList()),
    articleUrl       = article_url,
    commentsUrl      = comments_url,
    author           = author,
    readTimeMinutes  = read_time_minutes.toInt(),
    publishedAt      = "",
)

private fun com.techfeed.db.Bookmark.toFeedItem(json: Json) = FeedItem(
    id               = id,
    title            = title,
    contentBlocks    = runCatching {
        json.decodeFromString<List<ContentBlock>>(content_blocks)
    }.getOrDefault(emptyList()),
    summary          = summary,
    cleanBodyText    = clean_body_text,
    url              = url,
    source           = source,
    tags             = tags.split(",").filter { it.isNotBlank() },
    imageUrl         = image_url,
    mediaList        = runCatching { json.decodeFromString<List<MediaItem>>(media_list) }.getOrDefault(emptyList()),
    articleUrl       = article_url,
    commentsUrl      = comments_url,
    author           = author,
    readTimeMinutes  = read_time_minutes.toInt(),
    publishedAt      = "",
)

private fun com.techfeed.db.Read_history.toFeedItem(json: Json) = FeedItem(
    id               = id,
    title            = title,
    contentBlocks    = runCatching {
        json.decodeFromString<List<ContentBlock>>(content_blocks)
    }.getOrDefault(emptyList()),
    summary          = summary,
    cleanBodyText    = clean_body_text,
    url              = url,
    source           = source,
    tags             = tags.split(",").filter { it.isNotBlank() },
    imageUrl         = image_url,
    mediaList        = runCatching { json.decodeFromString<List<MediaItem>>(media_list) }.getOrDefault(emptyList()),
    articleUrl       = article_url,
    commentsUrl      = comments_url,
    author           = author,
    readTimeMinutes  = read_time_minutes.toInt(),
    publishedAt      = "",
)

// ── Platform time ─────────────────────────────────────────────────────────────
expect fun currentTimeMillis(): Long
