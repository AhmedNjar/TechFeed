package com.techhub.ai

import com.techhub.data.repository.PostRepository
import com.techhub.domain.document.PostClassification
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * Background service that processes PENDING posts.
 * Includes a DEV BYPASS to allow testing without Ollama running.
 */
@Service
class PostProcessingService(
    private val postRepository: PostRepository,
    private val ollamaService: OllamaService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    init {
        log.info("🚀 PostProcessingService initialized. Scheduling active.")
    }

    @Scheduled(fixedDelay = 10_000)
    fun processPendingPosts() {
        try {
            val pendingCount = postRepository.countByClassification(PostClassification.PENDING)
            if (pendingCount > 0) {
                log.info("Found $pendingCount PENDING posts to process")
            }

            val pending = postRepository.findByClassificationOrderByCreatedAtAsc(
                PostClassification.PENDING,
                PageRequest.of(0, BATCH_SIZE),
            )

            if (pending.isEmpty) return

            log.info("Processing ${pending.numberOfElements} PENDING posts")

            pending.content.forEach { post ->
                try {
                    var result = try {
                        ollamaService.analyze(post.title, post.content)
                    } catch (e: Exception) {
                        null
                    }

                    // ── DEV BYPASS ────────────────────────────────────────────────
                    if (result == null || result.classification == PostClassification.ERROR || result.classification == PostClassification.PENDING) {
                        log.warn("Ollama failed or returned error for '${post.title}' — applying DEV BYPASS")
                        result = AiResult(
                            summary        = post.content.take(180).replace("\n", " ") + "...",
                            contentBlocks  = emptyList(),
                            classification = PostClassification.TECHNICAL,
                            rawResponse    = "bypass"
                        )
                    }

                    val updated = post.copy(
                        // ✅ FIX: Use full parser blocks for the body to ensure "read the whole post".
                        // AI-generated blocks are often too short/summarized for a full reading mode.
                        contentBlocks  = post.contentBlocks, 
                        summary        = if (result.summary.isNotBlank()) result.summary else post.content.take(180),
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
        } catch (ex: Exception) {
            log.error("CRITICAL: processPendingPosts crashed: ${ex.message}")
        }
    }

    /** Cleans up ERROR posts older than 7 days. */
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
