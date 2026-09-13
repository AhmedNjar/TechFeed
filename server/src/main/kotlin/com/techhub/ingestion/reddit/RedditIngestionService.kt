package com.techhub.ingestion.reddit

import com.rometools.rome.io.SyndFeedInput
import com.techhub.config.RedditProperties
import com.techhub.data.repository.PostRepository
import com.techhub.domain.document.Post
import com.techhub.domain.document.PostClassification
import com.techhub.domain.document.PostSource
import com.techhub.ingestion.dedup.DuplicateDetectionService
import com.techhub.ingestion.content.FeedContentParser
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.io.StringReader
import java.time.Duration
import java.time.Instant
import kotlin.math.max

/**
 * Ingests Reddit posts using their public RSS feeds.
 * NOTE: Uses a "stealthy" User-Agent to bypass security blocks.
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
    private val contentParser = FeedContentParser()
    
    // The "Stealth" User-Agent verified via manual curl
    private val stealthUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    fun ingestAll(): IngestionReport {
        log.info("Starting Reddit ingestion (RSS mode) for ${properties.subreddits.size} subreddits")

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
        log.debug("Fetching r/${config.name}.rss")

        // Fetch RSS XML content
        val xmlContent = webClient.get()
            .uri("https://www.reddit.com/r/${config.name}/.rss?limit=${properties.limit}")
            .header("User-Agent", stealthUserAgent)
            .retrieve()
            .bodyToMono<String>()
            .timeout(Duration.ofSeconds(30))
            .block() ?: throw IllegalStateException("Empty response from Reddit RSS")

        val feed = SyndFeedInput().apply { isAllowDoctypes = false }.build(StringReader(xmlContent))

        var newPosts   = 0
        var duplicates = 0

        feed.entries.forEach { entry ->
            val url = entry.link ?: return@forEach

            if (postRepository.existsByUrl(url)) {
                duplicates++
                return@forEach
            }
            if (duplicateDetection.isDuplicate(entry.title ?: "")) {
                duplicates++
                return@forEach
            }

            val htmlContent = entry.contents.firstOrNull()?.value ?: entry.description?.value ?: ""
            val normalized = contentParser.parse(htmlContent, url, entry.author)

            val savedPost = Post(
                title           = entry.title?.trim() ?: "Untitled",
                content         = normalized.cleanBodyText.ifBlank { entry.title ?: "" },
                contentBlocks   = normalized.contentBlocks,
                url             = url,
                source          = PostSource.REDDIT,
                sourceName      = "Reddit",
                tags            = config.tags,
                imageUrl        = normalized.primaryImageUrl(),
                mediaList       = normalized.mediaList,
                articleUrl      = normalized.articleUrl,
                commentsUrl     = normalized.commentsUrl,
                author          = normalized.author,
                readTimeMinutes = max(1, normalized.cleanBodyText.split(Regex("\\s+")).size / 200),
                classification  = PostClassification.PENDING,
                publishedAt     = entry.publishedDate?.toInstant(),
                createdAt       = entry.publishedDate?.toInstant() ?: Instant.now(),
            )

            postRepository.save(savedPost)
            newPosts++
            log.debug("Saved Reddit post: '${savedPost.title}' [PENDING -> Ollama queue]")
        }

        return SubredditResult(config.name, newPosts, duplicates, 0)
    }

}

private fun com.techhub.ingestion.content.ParsedFeedContent.primaryImageUrl(): String? =
    mediaList.filterIsInstance<com.techhub.domain.document.MediaItem.Image>().firstOrNull()?.url
        ?: mediaList.filterIsInstance<com.techhub.domain.document.MediaItem.Video>().firstOrNull()?.thumbnailUrl

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
