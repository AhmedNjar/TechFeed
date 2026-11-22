package com.techhub.data.repository

import com.techhub.domain.document.Post
import com.techhub.domain.document.PostClassification
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.Aggregation
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface PostRepository : MongoRepository<Post, String> {

    /** Main mobile API query — only TECHNICAL posts, filtered by tag */
    fun findByTagsContainingIgnoreCaseAndClassification(
        tag: String,
        classification: PostClassification,
        pageable: Pageable,
    ): Page<Post>

    /** All TECHNICAL posts — no tag filter */
    fun findByClassification(
        classification: PostClassification,
        pageable: Pageable,
    ): Page<Post>

    /** Duplicate prevention — check before inserting */
    fun existsByUrl(url: String): Boolean

    fun deleteByUrl(url: String)

    /** Fetch pending posts for Ollama processing (batch) */
    fun findByClassificationOrderByCreatedAtAsc(
        classification: PostClassification,
        pageable: Pageable,
    ): Page<Post>

    /** Stats endpoint */
    fun countByClassification(classification: PostClassification): Long

    /** Cleanup stale PENDING posts older than threshold */
    fun deleteByClassificationAndCreatedAtBefore(
        classification: PostClassification,
        threshold: Instant,
    ): Long

    /** Full-text search across title + summary (requires @TextIndexed) */
    @Query("{ \$text: { \$search: ?0 }, classification: ?1 }")
    fun searchByText(query: String, classification: PostClassification, pageable: Pageable): Page<Post>

    // ✅ NEW: Duplicate detection — recent posts to compare titles against
    fun findByCreatedAtAfter(since: Instant, pageable: Pageable): Page<Post>

    // ✅ NEW: Related articles — posts sharing at least one tag, excluding self
    fun findByTagsInAndIdNotAndClassificationOrderByPublishedAtDesc(
        tags: List<String>,
        excludeId: String,
        classification: PostClassification,
        pageable: Pageable,
    ): Page<Post>

    // ✅ BUG FIX: previous escaping was broken two ways —
    //   1. `\$source` is invalid Kotlin: `\` escapes to one backslash, then `$source`
    //      tries to resolve a template variable named `source`, which doesn't exist
    //      in this scope → compile error (unresolved reference).
    //   2. Even ignoring that, MongoDB's $group stage requires field references as a
    //      *quoted JSON string* — `_id: "$source"`, not a bare `_id: $source` token.
    // Admin dashboard — counts grouped by source
    @Aggregation(pipeline = [
        "{ \$match: { classification: ?0 } }",
        "{ \$group: { _id: \"\$source\", count: { \$sum: 1 } } }",
    ])
    fun countGroupedBySource(classification: PostClassification): List<SourceCount>

    interface SourceCount {
        fun getId(): String
        fun getCount(): Long
    }
}
