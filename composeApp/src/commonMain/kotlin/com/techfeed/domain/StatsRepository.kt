package com.techfeed.domain

import com.techfeed.data.local.FeedLocalDataSource
import com.techfeed.data.local.currentTimeMillis
import com.techfeed.data.model.FeedItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class ReadingStats(
    val totalRead: Long          = 0,
    val currentStreakDays: Int   = 0,
    val longestStreakDays: Int   = 0,
    val topTags: List<TagCount>  = emptyList(),
)

data class TagCount(val tag: String, val count: Int)

class StatsRepository(private val local: FeedLocalDataSource) {

    /** Call when the user opens ReadModeScreen for an article */
    suspend fun recordRead(item: FeedItem) = local.recordRead(item)

    fun recentlyRead(limit: Long = 20): Flow<List<FeedItem>> = local.recentlyRead(limit)

    /** Combines count + timestamps + tags into one reactive stats object */
    val stats: Flow<ReadingStats> = combine(
        local.readCount(),
        local.allReadTimestamps(),
        local.allReadTags(),
    ) { total, timestamps, tagLists ->
        ReadingStats(
            totalRead         = total,
            currentStreakDays = computeCurrentStreak(timestamps),
            longestStreakDays = computeLongestStreak(timestamps),
            topTags           = computeTopTags(tagLists),
        )
    }

    // ── Streak logic ──────────────────────────────────────────────────────────
    // A "day" bucket is derived from epoch millis / 86_400_000 (UTC day index).
    // Simple and timezone-naive — good enough for a reading-streak feature.

    private fun toDayBuckets(timestamps: List<Long>): List<Long> =
        timestamps.map { it / 86_400_000L }.distinct().sortedDescending()

    private fun computeCurrentStreak(timestamps: List<Long>): Int {
        if (timestamps.isEmpty()) return 0
        val days = toDayBuckets(timestamps)
        val today = currentTimeMillis() / 86_400_000L

        // Streak must include today or yesterday to still be "current"
        if (days.first() < today - 1) return 0

        var streak = 1
        var expected = days.first() - 1
        for (i in 1 until days.size) {
            if (days[i] == expected) {
                streak++
                expected--
            } else break
        }
        return streak
    }

    private fun computeLongestStreak(timestamps: List<Long>): Int {
        if (timestamps.isEmpty()) return 0
        val days = toDayBuckets(timestamps).sorted()   // ascending for this pass

        var longest = 1
        var current = 1
        for (i in 1 until days.size) {
            current = if (days[i] == days[i - 1] + 1) current + 1 else 1
            if (current > longest) longest = current
        }
        return longest
    }

    private fun computeTopTags(tagLists: List<String>, limit: Int = 5): List<TagCount> =
        tagLists
            .flatMap { it.split(",").filter { tag -> tag.isNotBlank() } }
            .groupingBy { it }
            .eachCount()
            .map { (tag, count) -> TagCount(tag, count) }
            .sortedByDescending { it.count }
            .take(limit)
}
