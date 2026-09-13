package com.techfeed.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── Content Block — the unit the backend sends instead of Markdown ─────────────

@Serializable
data class ContentBlock(
    val type: ContentBlockType,
    val value: String  = "",    // for text, heading, code, quote
    val url: String    = "",    // for image
    val style: String  = "",    // optional: "h1","h2","h3","bold","normal"
)

@Serializable
enum class ContentBlockType {
    @SerialName("heading")   HEADING,
    @SerialName("text")      TEXT,
    @SerialName("image")     IMAGE,
    @SerialName("code")      CODE,
    @SerialName("quote")     QUOTE,
    @SerialName("divider")   DIVIDER,
}

/** Media is normalized by the server so Compose never has to parse feed HTML. */
@Serializable
sealed interface MediaItem {
    @Serializable
    @SerialName("image")
    data class Image(
        val url: String,
        val caption: String? = null,
    ) : MediaItem

    @Serializable
    @SerialName("video")
    data class Video(
        val streamUrl: String,
        val thumbnailUrl: String? = null,
    ) : MediaItem
}

// ── Feed Item ─────────────────────────────────────────────────────────────────

@Serializable
data class FeedItem(
    val id: String,
    val title: String,

    /** Structured content blocks — rendered by ContentRenderer composable */
    val contentBlocks: List<ContentBlock> = emptyList(),

    /** Short plain-text preview — shown on feed cards */
    val summary: String = "",

    /** Sanitized source text, used when editorial content blocks are unavailable. */
    val cleanBodyText: String = "",

    val url: String,

    /**
     * Plain string, not a closed enum.
     */
    val source: String = "Unknown",

    val tags: List<String> = emptyList(),

    val imageUrl: String? = null,

    /** All source media, independent of optional AI-generated content blocks. */
    val mediaList: List<MediaItem> = emptyList(),

    val articleUrl: String? = null,
    val commentsUrl: String? = null,
    val author: String? = null,

    val readTimeMinutes: Int = 0,

    val publishedAt: String = "",
)

// ── Paginated Response ────────────────────────────────────────────────────────

@Serializable
data class FeedResponse(
    val items: List<FeedItem>,
    val page: Int,
    val totalPages: Int,
    val hasNext: Boolean,
)

// ── Filter Tags ───────────────────────────────────────────────────────────────

enum class FeedTag(val label: String, val apiValue: String) {
    ALL     ("All",      "all"),
    KOTLIN  ("Kotlin",   "kotlin"),
    KMP     ("KMP",      "kmp"),
    AI      ("AI",       "ai"),
    ANDROID ("Android",  "android"),
    IOS     ("iOS",      "ios"),
    ROBOTICS("Robotics", "robotics"),
    BACKEND ("Backend",  "backend"),
}
