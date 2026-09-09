package com.techhub.scraping

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.techhub.config.ScraperProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.time.Duration

/**
 * Talks to the Python Scrapling microservice over REST.
 * The microservice handles all the actual scraping (Fetcher / StealthyFetcher
 * fallback + trafilatura extraction) — this client just calls it and maps responses.
 */
@Service
class ScraperApiClient(
    private val properties: ScraperProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val client: WebClient = WebClient.builder()
        .baseUrl(properties.baseUrl)
        .build()

    fun scrapeArticle(url: String): ScrapedArticle? {
        return try {
            client.post()
                .uri("/scrape/article")
                .bodyValue(ArticleScrapeRequest(url = url))
                .retrieve()
                .bodyToMono<ScrapedArticle>()
                .timeout(Duration.ofSeconds(properties.timeoutSeconds))
                .block()
                ?.takeIf { it.success }
        } catch (e: Exception) {
            log.warn("Scraper service failed for $url: ${e.message}")
            null
        }
    }

    fun discoverArticleLinks(
        listingUrl: String,
        linkSelector: String?,
        limit: Int = 20,
    ): List<String> {
        return try {
            val response = client.post()
                .uri("/scrape/discover")
                .bodyValue(DiscoverRequest(listingUrl, linkSelector, limit))
                .retrieve()
                .bodyToMono<DiscoverResponse>()
                .timeout(Duration.ofSeconds(properties.timeoutSeconds))
                .block()

            if (response?.success == true) response.articleUrls else emptyList()
        } catch (e: Exception) {
            log.warn("Link discovery failed for $listingUrl: ${e.message}")
            emptyList()
        }
    }
}

// ── DTOs matching the Python service's Pydantic models ────────────────────────

data class ArticleScrapeRequest(
    val url: String,
    val force_browser: Boolean = false,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ScrapedArticle(
    val success: Boolean,
    val url: String,
    val title: String? = null,
    val content: String? = null,
    val image_url: String? = null,
    val author: String? = null,
    val published_at: String? = null,
    val fetch_method: String? = null,
    val error: String? = null,
)

data class DiscoverRequest(
    val listing_url: String,
    val link_selector: String?,
    val limit: Int,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DiscoverResponse(
    val success: Boolean,
    val listing_url: String,
    val article_urls: List<String> = emptyList(),
    val error: String? = null,
) {
    val articleUrls get() = article_urls
}
