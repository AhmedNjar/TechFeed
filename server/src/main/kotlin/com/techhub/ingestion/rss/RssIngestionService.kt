package com.techhub.ingestion.rss

import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.XmlReader
import com.techhub.config.RssProperties
import com.techhub.data.repository.PostRepository
import com.techhub.domain.document.Post
import com.techhub.domain.document.PostClassification
import com.techhub.domain.document.PostSource
import com.techhub.ingestion.dedup.DuplicateDetectionService
import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.net.URL
import java.time.Instant
import kotlin.math.max

/**
 * FIX: this file did not exist anywhere in the project, but
 * IngestionScheduler.kt and AdminController.kt both still imported and
 * constructor-injected RssIngestionService — unresolved reference in both.
 *
 * Saves posts as PENDING; PostProcessingService promotes them via Ollama
 * asynchronously (kept fast — no blocking AI call during ingestion itself).
 */
@Service
class RssIngestionService(
    private val properties: RssProperties,
    private val postRepository: PostRepository,
    private val duplicateDetection: DuplicateDetectionService,
) {
    private val log       = LoggerFactory.getLogger(javaClass)
    private val mdConverter = FlexmarkHtmlConverter.builder().build()

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

        // SyndFeedInput is NOT thread-safe — a fresh instance per fetch.
        val feed = XmlReader(URL(config.url)).use { reader ->
            SyndFeedInput().apply {
                isAllowDoctypes = false // prevent XXE injection
            }.build(reader)
        }

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

            val markdownContent = if (htmlContent.isNotBlank())
                mdConverter.convert(htmlContent)
            else
                entry.title ?: ""

            val post = Post(
                title           = entry.title?.trim() ?: "Untitled",
                content         = markdownContent,
                url             = url,
                source          = PostSource.RSS,
                sourceName      = config.source,
                tags            = config.tags,
                imageUrl        = extractImageUrl(entry),
                readTimeMinutes = estimateReadTime(markdownContent),
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

    private fun extractImageUrl(entry: com.rometools.rome.feed.synd.SyndEntry): String? =
        entry.enclosures
            .firstOrNull { it.type?.startsWith("image") == true }
            ?.url
            ?: extractImgTagSrc(entry.description?.value ?: "")

    private fun extractImgTagSrc(html: String): String? =
        Regex("""<img[^>]+src=["']([^"']+)["']""")
            .find(html)
            ?.groupValues
            ?.getOrNull(1)

    private fun estimateReadTime(text: String): Int {
        val words = text.split(Regex("\\s+")).size
        return max(1, words / 200)
    }
}

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
