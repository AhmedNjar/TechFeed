package com.techhub.scheduler

import com.techhub.ingestion.github.GitHubTrendingService
import com.techhub.ingestion.reddit.RedditIngestionService
import com.techhub.ingestion.rss.RssIngestionService
import com.techhub.ingestion.scraper.BlogIngestionService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class IngestionScheduler(
    private val rssService: RssIngestionService,
    private val redditService: RedditIngestionService,
    private val gitHubService: GitHubTrendingService,
    private val blogService: BlogIngestionService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /** RSS feeds — every 3 hours */
    @Scheduled(cron = "\${ingestion.schedule.rss-cron}")
    fun runRssIngestion() {
        log.info("⚙️  Scheduler → RSS ingestion started")
        val report = rssService.ingestAll()
        log.info("✅ RSS done: ${report.newPosts} new, ${report.duplicates} skipped")
    }

    /** Reddit — every 3 hours (offset by 30 min) */
    @Scheduled(cron = "\${ingestion.schedule.reddit-cron}")
    fun runRedditIngestion() {
        log.info("⚙️  Scheduler → Reddit ingestion started")
        val report = redditService.ingestAll()
        log.info("✅ Reddit done: ${report.newPosts} new, ${report.duplicates} skipped")
    }

    /** GitHub Trending — once a day */
    @Scheduled(cron = "\${ingestion.schedule.github-cron}")
    fun runGitHubIngestion() {
        log.info("⚙️  Scheduler → GitHub Trending ingestion started")
        val count = gitHubService.ingestAll()
        log.info("✅ GitHub done: $count new repos")
    }

    /** Blogs without RSS — once a day, offset from GitHub */
    @Scheduled(cron = "\${ingestion.schedule.blog-cron:0 0 9 * * *}")
    fun runBlogIngestion() {
        log.info("⚙️  Scheduler → Blog scraping started")
        val report = blogService.ingestAll()
        log.info("✅ Blogs done: ${report.newPosts} new, ${report.duplicates} skipped, ${report.errors} errors")
    }
}
