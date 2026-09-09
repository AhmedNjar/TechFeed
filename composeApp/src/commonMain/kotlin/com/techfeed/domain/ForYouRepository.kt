package com.techfeed.domain

import com.techfeed.data.local.FeedLocalDataSource
import com.techfeed.data.model.FeedItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

/**
 * "For You" — the simplest honest form of personalization: rank tags by how
 * much the user has engaged with them (bookmarks weighted higher than reads,
 * since saving is a stronger signal than a passive open), then pull cached
 * articles matching those top tags.
 *
 * No ML, no server-side model — just weighted tag frequency. Transparent and
 * fast, which matters more for a v1 than sophistication.
 */
class ForYouRepository(
    private val local: FeedLocalDataSource,
    private val bookmarkRepo: BookmarkRepository,
) {
    companion object {
        private const val BOOKMARK_WEIGHT = 2
        private const val READ_WEIGHT     = 1
        private const val MAX_TOP_TAGS    = 3
        private const val PER_TAG_LIMIT   = 15L
    }

    /** Reactive — recomputes whenever bookmarks or read history change */
    val topTags: Flow<List<TagCount>> = combine(
        bookmarkRepo.bookmarks,
        local.allReadTags(),
    ) { bookmarks, readTagLists ->
        val scores = mutableMapOf<String, Int>()

        bookmarks.forEach { item ->
            item.tags.forEach { tag -> scores[tag] = (scores[tag] ?: 0) + BOOKMARK_WEIGHT }
        }
        readTagLists.forEach { tagsCsv ->
            tagsCsv.split(",").filter { it.isNotBlank() }.forEach { tag ->
                scores[tag] = (scores[tag] ?: 0) + READ_WEIGHT
            }
        }

        scores.entries
            .sortedByDescending { it.value }
            .take(MAX_TOP_TAGS)
            .map { TagCount(it.key, it.value) }
    }

    /**
     * One-shot fetch for the "For You" screen. Pulls from the local cache only
     * (fast, offline-friendly) — the regular tag feeds already keep the cache
     * warm via the normal browsing flow.
     */
    suspend fun loadForYouFeed(): ForYouResult {
        val tags = topTags.first()
        if (tags.isEmpty()) return ForYouResult(items = emptyList(), basedOnTags = emptyList())

        val seen = LinkedHashMap<String, FeedItem>()
        tags.forEach { tagCount ->
            local.getCachedByTagKeyword(tagCount.tag, PER_TAG_LIMIT).first().forEach { item ->
                seen.putIfAbsent(item.id, item)
            }
        }

        return ForYouResult(
            items       = seen.values.toList(),
            basedOnTags = tags.map { it.tag },
        )
    }
}

data class ForYouResult(
    val items: List<FeedItem>,
    val basedOnTags: List<String>,
)
