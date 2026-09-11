package com.techhub.ai

import com.techhub.data.repository.PostRepository
import com.techhub.domain.document.PostClassification
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * FIX: this file did not exist anywhere in the project. Every ingestion path
 * (RSS, Reddit, GitHub, blog scraper) saves posts with classification =
 * PENDING and expects THIS service to pick them up in the background and
 * call Ollama. Without it, every post stays PENDING forever, and since
 * every mobile-facing endpoint (GET /api/posts, search, related) filters
 * strictly on classification = TECHNICAL, the feed would be permanently
 * empty regardless of how successful ingestion was.
 *
 * Runs every 2 minutes, processes a small batch at a time (kind to a local
 * Ollama instance rather than blocking ingestion itself).
 */
@Service
class PostProcessingService(
    private val postRepository: PostRepository,
    private val ollamaService: OllamaService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = 120_000)
    fun processPendingPosts() {
        val pending = postRepository.findByClassificationOrderByCreatedAtAsc(
            PostClassification.PENDING,
            PageRequest.of(0, BATCH_SIZE),
        )

        if (pending.isEmpty) return

        log.info("Processing ${pending.numberOfElements} PENDING posts via Ollama")

        pending.content.forEach { post ->
            try {
                val result = ollamaService.analyze(post.title, post.content)

                val updated = post.copy(
                    contentBlocks  = result.contentBlocks,
                    summary        = result.summary,
                    classification = result.classification,
                    rawAiResponse  = null,
                    updatedAt      = Instant.now(),
                )

                postRepository.save(updated)
                log.debug("Processed '${post.title}' -> ${result.classification}")

            } catch (ex: Exception) {
                log.error("Failed to process post '${post.title}': ${ex.message}")
                postRepository.save(
                    post.copy(
                        classification = PostClassification.ERROR,
                        updatedAt      = Instant.now(),
                    )
                )
            }
        }
    }

    /** Cleans up ERROR posts older than 7 days — we give up retrying after a week. */
    @Scheduled(cron = "0 0 3 * * *")
    fun cleanupErrorPosts() {
        val threshold = Instant.now().minusSeconds(7 * 24 * 3600)
        val deleted = postRepository.deleteByClassificationAndCreatedAtBefore(
            PostClassification.ERROR, threshold
        )
        if (deleted > 0) log.info("Cleaned up $deleted stale ERROR posts")
    }

    companion object {
        private const val BATCH_SIZE = 5
    }
}
