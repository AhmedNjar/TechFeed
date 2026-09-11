package com.techhub.ingestion.github

import com.techhub.config.GitHubProperties
import com.techhub.data.repository.PostRepository
import com.techhub.domain.document.Post
import com.techhub.domain.document.PostClassification
import com.techhub.domain.document.PostSource
import com.techhub.ingestion.dedup.DuplicateDetectionService
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * FIX: this file did not exist anywhere in the project, but
 * IngestionScheduler.kt and AdminController.kt both still imported and
 * constructor-injected GitHubTrendingService — unresolved reference in both.
 *
 * Scrapes https://github.com/trending/{language}?since=daily — GitHub has no
 * official Trending API.
 */
@Service
class GitHubTrendingService(
    private val properties: GitHubProperties,
    private val postRepository: PostRepository,
    private val duplicateDetection: DuplicateDetectionService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun ingestAll(): Int {
        var totalNew = 0
        properties.languages.forEach { language ->
            runCatching { totalNew += ingestLanguage(language) }
                .onFailure { log.error("GitHub trending failed for $language: ${it.message}") }
        }
        log.info("GitHub Trending: $totalNew new repos saved")
        return totalNew
    }

    private fun ingestLanguage(language: String): Int {
        val url = "https://github.com/trending/${language.lowercase()}?since=${properties.since}"
        log.debug("Scraping GitHub Trending: $url")

        val doc = Jsoup.connect(url)
            .userAgent("Mozilla/5.0 (compatible; TechHub/1.0)")
            .timeout(15_000)
            .get()

        var newPosts = 0

        doc.select("article.Box-row").forEach { article ->
            val repoPath = article.select("h2 a").attr("href").trim('/')
            if (repoPath.isBlank()) return@forEach

            val repoUrl     = "https://github.com/$repoPath"
            val title       = repoPath.replace("/", " / ")
            val description = article.select("p").text().trim()
            val stars       = article.select("[id^=repo-stars]").text()
                .replace(",", "").trim().toIntOrNull() ?: 0
            val forks       = article.select("a[href$=/forks]").text()
                .replace(",", "").trim().toIntOrNull() ?: 0

            if (description.isBlank()) return@forEach
            if (postRepository.existsByUrl(repoUrl)) return@forEach
            if (duplicateDetection.isDuplicate(title)) return@forEach

            val content = buildContent(repoPath, description, language, stars, forks)

            val post = Post(
                title           = "🔥 $title",
                content         = content,
                url             = repoUrl,
                source          = PostSource.GITHUB_TRENDING,
                sourceName      = "GitHub Trending",
                tags            = listOf(language.lowercase(), "github", "opensource"),
                readTimeMinutes = 2,
                classification  = PostClassification.PENDING,
                createdAt       = Instant.now(),
            )

            postRepository.save(post)
            newPosts++
        }

        return newPosts
    }

    private fun buildContent(
        repoPath: String,
        description: String,
        language: String,
        stars: Int,
        forks: Int,
    ): String = """
        ## GitHub Trending Repository

        **Repository:** $repoPath
        **Language:** $language
        **Stars today:** $stars ⭐
        **Forks:** $forks

        ### Description
        $description
    """.trimIndent()
}
