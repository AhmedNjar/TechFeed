package com.techhub.ingestion.dedup

import com.techhub.data.repository.PostRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Cross-source duplicate detection.
 *
 * Problem: the same article can be ingested from RSS, Reddit, AND a scraped
 * blog — each with a slightly different URL and title (e.g. with/without a
 * subtitle, different casing, tracking params stripped differently).
 * existsByUrl() alone misses these.
 *
 * Approach: normalize the title (lowercase, strip punctuation, collapse
 * whitespace) and compare word-set Jaccard similarity against recently
 * ingested posts (last 7 days — older posts aren't worth comparing against,
 * and limits the comparison set size).
 */
@Service
class DuplicateDetectionService(
    private val postRepository: PostRepository,
) {
    companion object {
        private const val SIMILARITY_THRESHOLD = 0.75
        private const val LOOKBACK_DAYS = 7L
        private const val COMPARISON_LIMIT = 100
    }

    /**
     * Returns true if a sufficiently similar title already exists among
     * recently ingested posts.
     */
    fun isDuplicate(title: String): Boolean {
        val normalized = normalize(title)
        if (normalized.isBlank()) return false

        val since = Instant.now().minus(LOOKBACK_DAYS, ChronoUnit.DAYS)
        val recent = postRepository.findByCreatedAtAfter(
            since, PageRequest.of(0, COMPARISON_LIMIT)
        )

        val candidateWords = normalized.split(" ").toSet()

        return recent.content.any { existing ->
            val existingWords = normalize(existing.title).split(" ").toSet()
            jaccardSimilarity(candidateWords, existingWords) >= SIMILARITY_THRESHOLD
        }
    }

    private fun normalize(title: String): String =
        title.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")   // strip punctuation
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun jaccardSimilarity(a: Set<String>, b: Set<String>): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val intersection = a.intersect(b).size
        val union = a.union(b).size
        return if (union == 0) 0.0 else intersection.toDouble() / union
    }
}
