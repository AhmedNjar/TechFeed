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

    @Scheduled(fixedDelay = 10_000)
    fun processPendingPosts() {
        try {
            val pending = postRepository.findByClassificationOrderByCreatedAtAsc(
                PostClassification.PENDING,
                PageRequest.of(0, BATCH_SIZE),
            )

            if (pending.isEmpty) return

            pending.content.forEach { post ->
                try {
                    var result = try {
                        ollamaService.analyze(post.title, post.content)
                    } catch (e: Exception) {
                        null
                    }

                    if (result == null || result.classification == PostClassification.ERROR || result.classification == PostClassification.PENDING) {
                        log.warn("Ollama failed or returned error for '${post.title}' — applying bypass")
                        result = AiResult(
                            summary        = post.content.take(180).replace("\n", " ") + "...",
                            contentBlocks  = emptyList(),
                            classification = PostClassification.TECHNICAL,
                            rawResponse    = "bypass"
                        )
                    }

                    val updated = post.copy(
                        contentBlocks  = post.contentBlocks, 
                        summary        = if (result.summary.isNotBlank()) result.summary else post.content.take(180),
                        classification = result.classification,
                        rawAiResponse  = null,
                        updatedAt      = Instant.now(),
                    )

                    postRepository.save(updated)
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
            log.error("Error processing pending posts: ${ex.message}")
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
