package com.techhub.ingestion.reddit

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.techhub.config.RedditProperties
import com.techhub.data.repository.PostRepository
import com.techhub.domain.document.Post
import com.techhub.domain.document.PostClassification
import com.techhub.domain.document.PostSource
import com.techhub.ingestion.dedup.DuplicateDetectionService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.time.Duration
import java.time.Instant
import kotlin.math.max

/**
 * FIX: this file did not exist anywhere in the project, but
 * IngestionScheduler.kt and AdminController.kt both still imported and
 * constructor-injected RedditIngestionService — unresolved reference in both.
 */
@Service
class RedditIngestionService(
    @Qualifier("webClient")
    private val webClient: WebClient,
    private val properties: RedditProperties,
    private val postRepository: PostRepository,
    private val duplicateDetection: DuplicateDetectionService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun ingestAll(): IngestionReport {
        log.info("Starting Reddit ingestion for ${properties.subreddits.size} subreddits")

        val results = properties.subreddits.map { config ->
            runCatching { ingestSubreddit(config) }
                .getOrElse { ex ->
                    log.error("Failed to ingest r/${config.name}: ${ex.message}")
                    SubredditResult(config.name, 0, 0, 1)
                }
        }

        return IngestionReport(
            subredditsProcessed = results.size,
            newPosts   = results.sumOf { it.newPosts },
            duplicates = results.sumOf { it.duplicates },
            errors     = results.sumOf { it.errors },
        ).also { log.info("Reddit ingestion complete: $it") }
    }

    private fun ingestSubreddit(config: RedditProperties.SubredditConfig): SubredditResult {
        log.debug("Fetching r/${config.name}")

        val listing = webClient.get()
            .uri("https://www.reddit.com/r/${config.name}/hot.json?limit=${properties.limit}")
            .header("User-Agent", properties.userAgent)
            .retrieve()
            .bodyToMono<RedditListing>()
            .timeout(Duration.ofSeconds(30))
            .block()
            ?: return SubredditResult(config.name, 0, 0, 1)

        var newPosts   = 0
        var duplicates = 0

        listing.data.children
            .filter { it.data.isSelf || it.data.url.contains("reddit.com") }
            .forEach { child ->
                val post = child.data

                if (post.selftext.length < 100 && post.score < 50) return@forEach

                val url = "https://www.reddit.com${post.permalink}"

                if (postRepository.existsByUrl(url)) {
                    duplicates++
                    return@forEach
                }
                if (duplicateDetection.isDuplicate(post.title)) {
                    duplicates++
                    return@forEach
                }

                val content = buildMarkdownContent(post)

                val savedPost = Post(
                    title           = post.title.trim(),
                    content         = content,
                    url             = url,
                    source          = PostSource.REDDIT,
                    sourceName      = "Reddit",
                    tags            = (config.tags + extractFlairTags(post.linkFlair)).distinct(),
                    imageUrl        = post.thumbnail?.takeIf { it.startsWith("http") },
                    readTimeMinutes = max(1, post.selftext.split(" ").size / 200),
                    classification  = PostClassification.PENDING,
                    publishedAt     = Instant.ofEpochSecond(post.createdUtc.toLong()),
                    createdAt       = Instant.ofEpochSecond(post.createdUtc.toLong()),
                )

                postRepository.save(savedPost)
                newPosts++
                log.debug("Saved Reddit post: '${post.title}' [PENDING -> Ollama queue]")
            }

        return SubredditResult(config.name, newPosts, duplicates, 0)
    }

    private fun buildMarkdownContent(post: RedditPost): String = buildString {
        appendLine("**Posted in r/${post.subreddit} — Score: ${post.score}**")
        appendLine()
        if (post.selftext.isNotBlank()) {
            appendLine(post.selftext)
        } else {
            appendLine("*Link post: ${post.url}*")
        }
    }

    private fun extractFlairTags(flair: String?): List<String> {
        if (flair.isNullOrBlank()) return emptyList()
        return flair.lowercase()
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length > 2 }
            .take(3)
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class RedditListing(val data: RedditListingData)

@JsonIgnoreProperties(ignoreUnknown = true)
data class RedditListingData(val children: List<RedditChild>)

@JsonIgnoreProperties(ignoreUnknown = true)
data class RedditChild(val data: RedditPost)

@JsonIgnoreProperties(ignoreUnknown = true)
data class RedditPost(
    val title: String = "",
    val selftext: String = "",
    val url: String = "",
    val permalink: String = "",
    val subreddit: String = "",
    val score: Int = 0,
    val thumbnail: String? = null,
    @JsonProperty("is_self") val isSelf: Boolean = false,
    @JsonProperty("link_flair_text") val linkFlair: String? = null,
    @JsonProperty("created_utc") val createdUtc: Double = 0.0,
)

data class SubredditResult(
    val subreddit: String,
    val newPosts: Int,
    val duplicates: Int,
    val errors: Int,
)

data class IngestionReport(
    val subredditsProcessed: Int,
    val newPosts: Int,
    val duplicates: Int,
    val errors: Int,
)
