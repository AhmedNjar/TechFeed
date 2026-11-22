package com.techhub.api.dto

import com.techhub.domain.document.ContentBlock
import com.techhub.domain.document.MediaItem
import com.techhub.domain.document.Post
import com.techhub.domain.document.PostSource
import com.techhub.ingestion.content.FeedContentParser
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.hibernate.validator.constraints.URL
import org.slf4j.LoggerFactory
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
    return try {
        // Older posts were saved before parser blocks existed. Normalize those at
        // response time so they are readable immediately.
        val legacyContent = if (contentBlocks.isEmpty()) {
            LEGACY_CONTENT_PARSER.parse(content, url, author)
        } else null

        val normalizedSummary = legacyContent?.contentBlocks
            ?.firstOrNull { it.type == "text" || it.type == "quote" }
            ?.value
            ?.take(200)
            ?.takeIf(String::isNotBlank)
            ?: summary

        PostDto(
            id            = id ?: "",
            title         = title,
            contentBlocks = contentBlocks.ifEmpty { legacyContent?.contentBlocks.orEmpty() },
            summary       = normalizedSummary,
            cleanBodyText = legacyContent?.cleanBodyText ?: content,
            url           = url,
            // ✅ Resilience: Handle potential null/blank source from manual DB edits
            source        = sourceName.takeIf { it.isNotBlank() } ?: source?.name ?: "Unknown",
            tags          = tags,
            imageUrl      = imageUrl,
            mediaList     = mediaList,
            articleUrl    = articleUrl,
            commentsUrl   = commentsUrl,
            author        = author,
            readTimeMinutes = readTimeMinutes,
            publishedAt   = (publishedAt ?: createdAt).toString(),
        )
    } catch (e: Exception) {
        // Log the failure to help identify which document is broken
        LoggerFactory.getLogger("PostMapper").error("Failed to map post $id: ${e.message}")
        throw e
    }
}

private val LEGACY_CONTENT_PARSER = FeedContentParser()
