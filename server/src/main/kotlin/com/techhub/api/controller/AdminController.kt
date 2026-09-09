package com.techhub.api.controller

import com.techhub.config.ApiProperties
import com.techhub.ingestion.github.GitHubTrendingService
import com.techhub.ingestion.reddit.RedditIngestionService
import com.techhub.ingestion.rss.RssIngestionService
import com.techhub.ingestion.scraper.BlogIngestionService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Admin endpoints isolated in their own controller.
 *
 * In production, protect this with Spring Security instead of a shared secret:
 *   @PreAuthorize("hasRole('ADMIN')")
 *
 * For now, header-based secret is used for simplicity.
 */
@RestController
@RequestMapping("/api/admin")
class AdminController(
    private val rssService: RssIngestionService,
    private val redditService: RedditIngestionService,
    private val gitHubService: GitHubTrendingService,
    private val blogService: BlogIngestionService,
    private val apiProperties: ApiProperties,
) {
    private fun checkSecret(secret: String): Boolean =
        secret == apiProperties.chromeExtensionSecret

    @PostMapping("/ingest/rss")
    fun triggerRss(@RequestHeader("X-Admin-Secret") secret: String): ResponseEntity<Any> {
        if (!checkSecret(secret)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        return ResponseEntity.ok(rssService.ingestAll())
    }

    @PostMapping("/ingest/reddit")
    fun triggerReddit(@RequestHeader("X-Admin-Secret") secret: String): ResponseEntity<Any> {
        if (!checkSecret(secret)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        return ResponseEntity.ok(redditService.ingestAll())
    }

    @PostMapping("/ingest/github")
    fun triggerGitHub(@RequestHeader("X-Admin-Secret") secret: String): ResponseEntity<Any> {
        if (!checkSecret(secret)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        return ResponseEntity.ok(mapOf("newRepos" to gitHubService.ingestAll()))
    }

    @PostMapping("/ingest/blogs")
    fun triggerBlogs(@RequestHeader("X-Admin-Secret") secret: String): ResponseEntity<Any> {
        if (!checkSecret(secret)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        return ResponseEntity.ok(blogService.ingestAll())
    }
}
