package com.techhub.api.dto

import com.techhub.domain.document.ContentBlock
import com.techhub.domain.document.Post
import com.techhub.domain.document.PostSource
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

    val url: String,
    val source: String,
    val tags: List<String>,
    val imageUrl: String?,
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

fun Post.toDto() = PostDto(
    id            = id ?: "",
    title         = title,
    contentBlocks = contentBlocks,
    summary       = summary,
    url           = url,
    // ✅ FIX: use the actual per-post source name (e.g. "Dev.to", "Kotlin Blog")
    // instead of the coarse PostSource enum, which collapses every RSS blog
    // into a single "RSS" label. Falls back to the enum name if sourceName
    // wasn't set (e.g. posts ingested before this field existed).
    source        = sourceName.ifBlank { source.name },
    tags          = tags,
    imageUrl      = imageUrl,
    readTimeMinutes = readTimeMinutes,
    // ✅ F-3: use publishedAt if available, else fall back to createdAt
    publishedAt   = (publishedAt ?: createdAt).toString(),
)
