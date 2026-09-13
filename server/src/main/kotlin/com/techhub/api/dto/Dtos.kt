package com.techhub.api.dto

import com.techhub.domain.document.ContentBlock
import com.techhub.domain.document.MediaItem
import com.techhub.domain.document.Post
import com.techhub.domain.document.PostSource
import com.techhub.ingestion.content.FeedContentParser
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.hibernate.validator.constraints.URL
import java.time.Instant

// ── Mobile App Response ───────────────────────────────────────────────────────

/**
 * ✅ C-1 FIX: PostDto now includes contentBlocks for the KMP ContentRenderer.
 *
 * Field mapping to KMP app:
 *   contentBlocks → ContentRenderer(blocks = item.contentBlocks)
 *   summary       → FeedCard snippet (plain text preview)
 *   publishedAt   → displayed in ReadMode and card
 */
data class PostDto(
    val id: String,
    val title: String,

    /** Structured content for ReadModeScreen — rendered by ContentRenderer */
    val contentBlocks: List<ContentBlock> = emptyList(),

    /** Plain-text card preview (≤ 200 chars) */
    val summary: String,

    /** Deterministically sanitized source text; not dependent on AI processing. */
    val cleanBodyText: String = "",

    val url: String,
    val source: String,
    val tags: List<String>,
    val imageUrl: String?,
    val mediaList: List<MediaItem> = emptyList(),
    val articleUrl: String? = null,
    val commentsUrl: String? = null,
    val author: String? = null,
    val readTimeMinutes: Int,

    /** ✅ F-3 FIX: publishedAt from the source, not ingestion time */
    val publishedAt: String,
)

data class PostPageDto(
    val items: List<PostDto>,
    val page: Int,
    val totalPages: Int,
    val hasNext: Boolean,
    val totalItems: Long,
)

// ── Chrome Extension Request ──────────────────────────────────────────────────

data class SavePostRequest(
    @field:NotBlank(message = "title must not be blank")
    @field:Size(max = 500, message = "title too long")
    val title: String,

    @field:NotBlank(message = "url must not be blank")
    @field:URL(message = "url must be a valid URL")
    val url: String,

    @field:NotBlank(message = "content must not be blank")
    @field:Size(max = 100_000, message = "content too large")
    val content: String,

    val imageUrl: String? = null,
)

// ── Stats ─────────────────────────────────────────────────────────────────────

data class StatsDto(
    val totalTechnical: Long,
    val totalSpam: Long,
    val totalPending: Long,
    val totalError: Long,
    val sourceCounts: Map<String, Long>,
)

// ── Mapper ────────────────────────────────────────────────────────────────────

fun Post.toDto(): PostDto {
    // Older posts were saved before parser blocks existed. Normalize those at
    // response time so they are readable immediately; new ingestions persist
    // the same blocks and therefore skip this fallback.
    val legacyContent = if (contentBlocks.isEmpty()) {
        LEGACY_CONTENT_PARSER.parse(content, url, author)
    } else null
    val normalizedSummary = legacyContent?.contentBlocks
        ?.firstOrNull { it.type == "text" || it.type == "quote" }
        ?.value
        ?.take(200)
        ?.takeIf(String::isNotBlank)
        ?: summary

    return PostDto(
    id            = id ?: "",
    title         = title,
    contentBlocks = contentBlocks.ifEmpty { legacyContent?.contentBlocks.orEmpty() },
    summary       = normalizedSummary,
    cleanBodyText = legacyContent?.cleanBodyText ?: content,
    url           = url,
    // ✅ FIX: use the actual per-post source name (e.g. "Dev.to", "Kotlin Blog")
    // instead of the coarse PostSource enum, which collapses every RSS blog
    // into a single "RSS" label. Falls back to the enum name if sourceName
    // wasn't set (e.g. posts ingested before this field existed).
    source        = sourceName.ifBlank { source.name },
    tags          = tags,
    imageUrl      = imageUrl,
    mediaList     = mediaList,
    articleUrl    = articleUrl,
    commentsUrl   = commentsUrl,
    author        = author,
    readTimeMinutes = readTimeMinutes,
    // ✅ F-3: use publishedAt if available, else fall back to createdAt
    publishedAt   = (publishedAt ?: createdAt).toString(),
    )
}

private val LEGACY_CONTENT_PARSER = FeedContentParser()
