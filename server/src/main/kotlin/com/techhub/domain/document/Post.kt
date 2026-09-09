package com.techhub.domain.document

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.index.TextIndexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "posts")
data class Post(

    @Id
    val id: String? = null,

    @TextIndexed(weight = 3f)
    val title: String,

    /** Full raw content from source — NOT sent to mobile */
    val content: String,

    /**
     * ✅ C-1 FIX: Structured JSON content blocks for the KMP mobile app.
     * Stored as a JSON string in MongoDB, serialized/deserialized via Jackson.
     * Replaces the old plain Markdown summary field.
     *
     * Example value stored:
     * [{"type":"heading","value":"TL;DR","style":"h2"},
     *  {"type":"text","value":"Kotlin 2.1 introduces..."},
     *  {"type":"code","value":"val x: Int = 42"}]
     */
    val contentBlocks: List<ContentBlock> = emptyList(),

    /**
     * Short plain-text preview for the feed card (≤ 200 chars).
     * Different from contentBlocks — this is the card snippet.
     */
    @TextIndexed(weight = 2f)
    val summary: String = "",

    @Indexed(unique = true)
    val url: String,

    val source: PostSource,

    /** ✅ FIX: human-readable origin name sent to mobile, e.g. "Dev.to", "Kotlin Blog",
     *  "Reddit", "GitHub Trending", "Saved". PostSource alone can't represent this —
     *  many different blogs all map to PostSource.RSS. */
    val sourceName: String = "",

    @Indexed
    val tags: List<String> = emptyList(),

    val imageUrl: String? = null,

    val readTimeMinutes: Int = 0,

    val classification: PostClassification = PostClassification.PENDING,

    /** Only stored during development — null in prod to save space */
    val rawAiResponse: String? = null,

    /** When the article was originally published (from the source) */
    @Indexed
    val publishedAt: Instant? = null,

    @Indexed
    val createdAt: Instant = Instant.now(),

    val updatedAt: Instant = Instant.now(),
)

// ── Content Block — mirrors the KMP app's ContentBlock model ─────────────────

data class ContentBlock(
    val type: String,     // "heading" | "text" | "image" | "code" | "quote" | "divider"
    val value: String  = "",
    val url: String    = "",
    val style: String  = "",  // "h1"|"h2"|"h3"|"bold"|"normal"
)

enum class PostSource {
    RSS,
    REDDIT,
    GITHUB_TRENDING,
    CHROME_EXTENSION,
}

enum class PostClassification {
    PENDING,
    TECHNICAL,
    SPAM,
    ERROR,
}
