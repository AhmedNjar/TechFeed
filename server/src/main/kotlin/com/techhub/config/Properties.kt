package com.techhub.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@ConfigurationProperties(prefix = "ollama")
data class OllamaProperties(
    val baseUrl: String = "http://localhost:11434",
    val model: String   = "llama3.2",
    val timeoutSeconds: Long = 60,
)

@ConfigurationProperties(prefix = "ingestion.rss")
data class RssProperties(
    val feeds: List<FeedConfig> = emptyList(),
) {
    data class FeedConfig(
        val url: String,
        val source: String,
        val tags: List<String> = emptyList(),
    )
}

@ConfigurationProperties(prefix = "ingestion.reddit")
data class RedditProperties(
    val subreddits: List<SubredditConfig> = emptyList(),
    val limit: Int = 25,
    val userAgent: String = "TechHub/1.0",
) {
    data class SubredditConfig(
        val name: String,
        val tags: List<String> = emptyList(),
    )
}

@ConfigurationProperties(prefix = "ingestion.github")
data class GitHubProperties(
    val languages: List<String> = listOf("Kotlin"),
    val since: String = "daily",
)

@ConfigurationProperties(prefix = "api")
data class ApiProperties(
    val chromeExtensionSecret: String = "dev-secret-change-in-prod",
    val pageSize: Int = 20,
)

@ConfigurationProperties(prefix = "ingestion.scraper")
data class ScraperProperties(
    val baseUrl: String = "http://localhost:8000",
    val timeoutSeconds: Long = 45,
    val blogs: List<BlogConfig> = emptyList(),
) {
    data class BlogConfig(
        val name: String,
        val listingUrl: String,
        val linkSelector: String? = null,
        val tags: List<String> = emptyList(),
    )
}

// ── Registration ──────────────────────────────────────────────────────────────

@Configuration
@EnableConfigurationProperties(
    OllamaProperties::class,
    RssProperties::class,
    RedditProperties::class,
    GitHubProperties::class,
    ApiProperties::class,
    ScraperProperties::class,
)
open class PropertiesConfig
