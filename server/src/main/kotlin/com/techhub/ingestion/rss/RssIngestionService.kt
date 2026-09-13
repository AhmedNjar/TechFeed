package com.techhub.ingestion.rss

import com.rometools.rome.io.SyndFeedInput
import com.techhub.config.RssProperties
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
 * Saves posts as PENDING; PostProcessingService promotes them via Ollama
 * asynchronously (kept fast — no blocking AI call during ingestion itself).
 *
 * NOTE: Uses a "stealthy" User-Agent for feeds that might block automated requests.
 */
@Service
class RssIngestionService(
    @Qualifier("webClient")
    private val webClient: WebClient,
    private val properties: RssProperties,
    private val postRepository: PostRepository,
    private val duplicateDetection: DuplicateDetectionService,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val contentParser = FeedContentParser()

    // The "Stealth" User-Agent
    private val stealthUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    fun ingestAll(): IngestionReport {
        log.info("Starting RSS ingestion for ${properties.feeds.size} feeds")

        val results = properties.feeds.map { feedConfig ->
            runCatching { ingestFeed(feedConfig) }
                .getOrElse { ex ->
                    log.error("Failed to ingest feed ${feedConfig.url}: ${ex.message}")
                    FeedResult(feedConfig.source, 0, 0, 1)
                }
        }

        return IngestionReport(
            feedsProcessed = results.size,
            newPosts       = results.sumOf { it.newPosts },
            duplicates     = results.sumOf { it.duplicates },
            errors         = results.sumOf { it.errors },
        ).also { log.info("RSS ingestion complete: $it") }
    }

    private fun ingestFeed(config: RssProperties.FeedConfig): FeedResult {
        log.debug("Fetching RSS feed: ${config.url}")

        // Fetch XML content as String using WebClient with browser User-Agent
        val xmlContent = webClient.get()
            .uri(config.url)
            .header("User-Agent", stealthUserAgent)
            .retrieve()
            .bodyToMono<String>()
            .timeout(Duration.ofSeconds(30))
            .block() ?: throw IllegalStateException("Empty response from ${config.url}")

        val feed = SyndFeedInput().apply {
            isAllowDoctypes = false
        }.build(StringReader(xmlContent))

        var newPosts = 0
        var duplicates = 0

        feed.entries.forEach { entry ->
            val url = entry.link ?: entry.uri ?: return@forEach

            if (postRepository.existsByUrl(url)) {
                duplicates++
                return@forEach
            }
            if (duplicateDetection.isDuplicate(entry.title ?: "")) {
                duplicates++
                return@forEach
            }

            val htmlContent = entry.contents
                .firstOrNull()?.value
                ?: entry.description?.value
                ?: ""

            val parsed = contentParser.parse(htmlContent, url, entry.author)
            val enclosureMedia = entry.enclosures
                .filter { it.type?.startsWith("image/") == true || it.type?.startsWith("video/") == true }
                .flatMap { enclosure ->
                    val tag = if (enclosure.type?.startsWith("video/") == true) "video" else "img"
                    contentParser.parse("<$tag src=\"${enclosure.url}\">", url).mediaList
                }
            val normalized = parsed.copy(mediaList = (enclosureMedia + parsed.mediaList).distinctBy { media ->
                when (media) {
                    is com.techhub.domain.document.MediaItem.Image -> "image:${media.url}"
                    is com.techhub.domain.document.MediaItem.Video -> "video:${media.streamUrl}"
                }
            })

            val post = Post(
                title           = entry.title?.trim() ?: "Untitled",
                content         = normalized.cleanBodyText.ifBlank { entry.title ?: "" },
                contentBlocks   = normalized.contentBlocks,
                url             = url,
                source          = PostSource.RSS,
                sourceName      = config.source,
                tags            = config.tags,
                imageUrl        = normalized.primaryImageUrl(),
                mediaList       = normalized.mediaList,
                articleUrl      = normalized.articleUrl,
                commentsUrl     = normalized.commentsUrl,
                author          = normalized.author,
                readTimeMinutes = estimateReadTime(normalized.cleanBodyText),
                classification  = PostClassification.PENDING,
                publishedAt     = entry.publishedDate?.toInstant(),
                createdAt       = entry.publishedDate?.toInstant() ?: Instant.now(),
            )

            postRepository.save(post)
            newPosts++
            log.debug("Saved post: '${post.title}' [PENDING -> Ollama queue]")
        }

        return FeedResult(config.source, newPosts, duplicates, 0)
    }

    private fun estimateReadTime(text: String): Int {
        val words = text.split(Regex("\\s+")).size
        return max(1, words / 200)
    }
}

private fun com.techhub.ingestion.content.ParsedFeedContent.primaryImageUrl(): String? =
    mediaList.filterIsInstance<com.techhub.domain.document.MediaItem.Image>().firstOrNull()?.url
        ?: mediaList.filterIsInstance<com.techhub.domain.document.MediaItem.Video>().firstOrNull()?.thumbnailUrl

data class FeedResult(
    val source: String,
    val newPosts: Int,
    val duplicates: Int,
    val errors: Int,
)

data class IngestionReport(
    val feedsProcessed: Int,
    val newPosts: Int,
    val duplicates: Int,
    val errors: Int,
)
