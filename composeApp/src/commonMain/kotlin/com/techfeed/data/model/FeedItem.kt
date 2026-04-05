package com.techfeed.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── Content Block — the unit the backend sends instead of Markdown ─────────────
// Backend sends: [{"type":"heading","value":"Intro"},{"type":"image","url":"..."}]

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

// ── Feed Item ─────────────────────────────────────────────────────────────────

@Serializable
data class FeedItem(
    val id: String,
    val title: String,

    /** Structured content blocks — rendered by ContentRenderer composable */
    @SerialName("content_blocks")
    val contentBlocks: List<ContentBlock> = emptyList(),

    /** Short plain-text preview — shown on feed cards */
    val summary: String = "",

    val url: String,
    val source: FeedSource,
    val tags: List<String> = emptyList(),

    @SerialName("image_url")
    val imageUrl: String? = null,

    @SerialName("read_time_minutes")
    val readTimeMinutes: Int = 0,

    @SerialName("published_at")
    val publishedAt: String = "",
)

@Serializable
enum class FeedSource {
    @SerialName("reddit")      REDDIT,
    @SerialName("devto")       DEV_TO,
    @SerialName("medium")      MEDIUM,
    @SerialName("hackernews")  HACKER_NEWS,
    @SerialName("github")      GITHUB,
}

val FeedSource.displayName get() = when (this) {
    FeedSource.REDDIT      -> "Reddit"
    FeedSource.DEV_TO      -> "Dev.to"
    FeedSource.MEDIUM      -> "Medium"
    FeedSource.HACKER_NEWS -> "Hacker News"
    FeedSource.GITHUB      -> "GitHub"
}

// ── Paginated Response ────────────────────────────────────────────────────────

@Serializable
data class FeedResponse(
    val items: List<FeedItem>,
    val page: Int,
    @SerialName("total_pages") val totalPages: Int,
    @SerialName("has_next")    val hasNext: Boolean,
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
