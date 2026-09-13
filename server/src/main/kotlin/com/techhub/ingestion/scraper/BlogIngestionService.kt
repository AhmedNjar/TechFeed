package com.techhub.ingestion.scraper

import com.techhub.config.ScraperProperties
import com.techhub.data.repository.PostRepository
import com.techhub.ingestion.dedup.DuplicateDetectionService
import com.techhub.ingestion.content.FeedContentParser
import com.techhub.domain.document.Post
import com.techhub.domain.document.PostClassification
import com.techhub.domain.document.PostSource
import com.techhub.scraping.ScraperApiClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.format.DateTimeFormatter
import kotlin.math.max

/**
 * Ingests articles from blogs that don't have an RSS feed, using the Python
 * Scrapling microservice for discovery + extraction.
 *
 * Same pattern as RssIngestionService/RedditIngestionService:
 *   - dedupe by URL
 *   - save as PENDING
 *   - PostProcessingService picks it up and calls Ollama async
 */
@Service
class BlogIngestionService(
    private val properties: ScraperProperties,
    private val scraperApi: ScraperApiClient,
    private val postRepository: PostRepository,
    private val duplicateDetection: DuplicateDetectionService,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val contentParser = FeedContentParser()

    fun ingestAll(): BlogIngestionReport {
        log.info("Starting blog ingestion for ${properties.blogs.size} configured blogs")

        val results = properties.blogs.map { blog ->
            runCatching { ingestBlog(blog) }
                .getOrElse { ex ->
                    log.error("Failed to ingest blog '${blog.name}': ${ex.message}")
                    BlogResult(blog.name, 0, 0, 1)
                }
        }

        return BlogIngestionReport(
            blogsProcessed = results.size,
            newPosts       = results.sumOf { it.newPosts },
            duplicates     = results.sumOf { it.duplicates },
            errors         = results.sumOf { it.errors },
        ).also { log.info("Blog ingestion complete: $it") }
    }

    private fun ingestBlog(blog: ScraperProperties.BlogConfig): BlogResult {
        log.debug("Discovering articles on ${blog.name} (${blog.listingUrl})")

        val articleUrls = scraperApi.discoverArticleLinks(
            listingUrl   = blog.listingUrl,
            linkSelector = blog.linkSelector,
            limit        = 20,
        )

        if (articleUrls.isEmpty()) {
            log.warn("No article links discovered for ${blog.name} — check link_selector config")
            return BlogResult(blog.name, 0, 0, 0)
        }

        var newPosts = 0
        var duplicates = 0

        articleUrls.forEach { url ->
            if (postRepository.existsByUrl(url)) {
                duplicates++
                return@forEach
            }

            val article = scraperApi.scrapeArticle(url)
            if (article == null || article.content.isNullOrBlank()) {
                log.debug("Skipping $url — scrape failed or content too short")
                return@forEach
            }
            if (duplicateDetection.isDuplicate(article.title ?: "")) {
                duplicates++
                return@forEach
            }

            val parsed = contentParser.parse(article.content, url, article.author)
            val coverMedia = article.image_url?.let { imageUrl ->
                contentParser.parse("<img src=\"$imageUrl\">", url).mediaList
            }.orEmpty()
            val normalized = parsed.copy(mediaList = (coverMedia + parsed.mediaList).distinctBy { media ->
                when (media) {
                    is com.techhub.domain.document.MediaItem.Image -> "image:${media.url}"
                    is com.techhub.domain.document.MediaItem.Video -> "video:${media.streamUrl}"
                }
            })

            val post = Post(
                title           = article.title?.trim() ?: "Untitled",
                content         = normalized.cleanBodyText,
                contentBlocks   = normalized.contentBlocks,
                url             = url,
                source          = PostSource.RSS,   // reuse RSS source type (it's a blog article either way)
                sourceName      = blog.name,        // ✅ e.g. configured blog display name
                tags            = blog.tags,
                imageUrl        = normalized.primaryImageUrl(),
                mediaList       = normalized.mediaList,
                articleUrl      = normalized.articleUrl,
                commentsUrl     = normalized.commentsUrl,
                author          = normalized.author,
                readTimeMinutes = estimateReadTime(normalized.cleanBodyText),
                classification  = PostClassification.PENDING,
                publishedAt     = parsePublishedAt(article.published_at),
                createdAt       = Instant.now(),
            )

            postRepository.save(post)
            newPosts++
            log.debug("Saved blog article: '${post.title}' [${article.fetch_method}]")
        }

        return BlogResult(blog.name, newPosts, duplicates, 0)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun estimateReadTime(text: String): Int {
        val words = text.split(Regex("\\s+")).size
        return max(1, words / 200)
    }

    private fun parsePublishedAt(raw: String?): Instant? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            Instant.parse(raw)
        }.recoverCatching {
            // trafilatura sometimes returns "YYYY-MM-DD" without time
            java.time.LocalDate.parse(raw, DateTimeFormatter.ISO_LOCAL_DATE)
                .atStartOfDay(java.time.ZoneOffset.UTC)
                .toInstant()
        }.getOrNull()
    }
}

private fun com.techhub.ingestion.content.ParsedFeedContent.primaryImageUrl(): String? =
    mediaList.filterIsInstance<com.techhub.domain.document.MediaItem.Image>().firstOrNull()?.url
        ?: mediaList.filterIsInstance<com.techhub.domain.document.MediaItem.Video>().firstOrNull()?.thumbnailUrl

// ── Reports ───────────────────────────────────────────────────────────────────

data class BlogResult(
    val blogName: String,
    val newPosts: Int,
    val duplicates: Int,
    val errors: Int,
)

data class BlogIngestionReport(
    val blogsProcessed: Int,
    val newPosts: Int,
    val duplicates: Int,
    val errors: Int,
)
