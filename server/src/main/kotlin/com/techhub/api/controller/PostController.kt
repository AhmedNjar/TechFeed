package com.techhub.api.controller

import com.techhub.api.dto.*
import com.techhub.config.ApiProperties
import com.techhub.data.repository.PostRepository
import com.techhub.domain.document.PostClassification
import com.techhub.domain.document.PostSource
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@RestController
@RequestMapping("/api")
class PostController(
    private val postRepository: PostRepository,
    private val apiProperties: ApiProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // ── ✅ M-1 FIX + BUG-4 FIX: Thread-safe rate limiter ───────────────────────
    // windowStart was a non-synchronized var — race condition on concurrent requests.
    // Fixed: use AtomicLong for windowStart so compare-and-reset is atomic.
    private val requestCounts = ConcurrentHashMap<String, AtomicInteger>()
    private val windowStart   = java.util.concurrent.atomic.AtomicLong(System.currentTimeMillis())
    private val WINDOW_MS     = 60_000L
    private val MAX_REQUESTS  = 60

    private fun isRateLimited(request: HttpServletRequest): Boolean {
        val now = System.currentTimeMillis()
        // AtomicLong.compareAndSet ensures only one thread resets the window
        if (now - windowStart.get() > WINDOW_MS) {
            if (windowStart.compareAndSet(windowStart.get(), now)) {
                requestCounts.clear()
            }
        }
        val ip    = request.remoteAddr ?: "unknown"
        val count = requestCounts.getOrPut(ip) { AtomicInteger(0) }.incrementAndGet()
        return count > MAX_REQUESTS
    }

    // ── ✅ m-4 FIX: Search query sanitization ────────────────────────────────
    private fun sanitizeQuery(query: String): String =
        query
            .replace(Regex("[{}\\[\\]\\$\\(\\)]"), "")   // strip MongoDB special chars
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(100)                                      // max 100 chars

    // ── Mobile Feed ───────────────────────────────────────────────────────────

    @GetMapping("/posts")
    fun getPosts(
        @RequestParam(required = false) tag: String?,
        @RequestParam(required = false) search: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "0") size: Int,
        request: HttpServletRequest,
    ): ResponseEntity<PostPageDto> {
        if (isRateLimited(request))
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build()

        val pageSize = if (size > 0) minOf(size, 50) else apiProperties.pageSize
        // ✅ M-5 FIX: sort by publishedAt DESC (original publish time, not ingestion time)
        val pageable = PageRequest.of(
            page, pageSize,
            Sort.by(Sort.Direction.DESC, "publishedAt").and(
                Sort.by(Sort.Direction.DESC, "createdAt")
            )
        )

        val result = when {
            !search.isNullOrBlank() -> {
                val clean = sanitizeQuery(search)
                postRepository.searchByText(clean, PostClassification.TECHNICAL, pageable)
            }
            !tag.isNullOrBlank() && tag != "all" ->
                postRepository.findByTagsContainingIgnoreCaseAndClassification(
                    tag, PostClassification.TECHNICAL, pageable,
                )
            else ->
                postRepository.findByClassification(
                    PostClassification.TECHNICAL, pageable,
                )
        }

        return ResponseEntity.ok(PostPageDto(
            items      = result.content.map { it.toDto() },
            page       = result.number,
            totalPages = result.totalPages,
            hasNext    = result.hasNext(),
            totalItems = result.totalElements,
        ))
    }

    @GetMapping("/posts/{id}")
    fun getPost(
        @PathVariable id: String,
        request: HttpServletRequest,
    ): ResponseEntity<PostDto> {
        if (isRateLimited(request)) return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build()
        val post = postRepository.findById(id).orElse(null)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(post.toDto())
    }

    // ✅ NEW: Related articles — used by ReadModeScreen's "Related" section
    @GetMapping("/posts/{id}/related")
    fun getRelatedPosts(
        @PathVariable id: String,
        @RequestParam(defaultValue = "5") limit: Int,
        request: HttpServletRequest,
    ): ResponseEntity<List<PostDto>> {
        if (isRateLimited(request)) return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build()

        val post = postRepository.findById(id).orElse(null)
            ?: return ResponseEntity.notFound().build()

        if (post.tags.isEmpty()) return ResponseEntity.ok(emptyList())

        val related = postRepository.findByTagsInAndIdNotAndClassificationOrderByPublishedAtDesc(
            post.tags, id, PostClassification.TECHNICAL,
            PageRequest.of(0, minOf(limit, 10)),
        )

        return ResponseEntity.ok(related.content.map { it.toDto() })
    }

    // ── Chrome Extension Save ─────────────────────────────────────────────────

    @PostMapping("/posts/save")
    fun saveFromExtension(
        @RequestHeader("X-Extension-Secret") secret: String,
        @Valid @RequestBody request: SavePostRequest,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<PostDto> {
        // ✅ M-2 FIX: Validate secret properly + log suspicious access
        if (!validateSecret(secret, httpRequest)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
        if (postRepository.existsByUrl(request.url)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build()
        }

        val post = com.techhub.domain.document.Post(
            title          = request.title,
            content        = request.content,
            url            = request.url,
            source         = PostSource.CHROME_EXTENSION,
            sourceName     = "Saved",
            tags           = listOf("saved"),
            imageUrl       = request.imageUrl,
            classification = PostClassification.PENDING,  // Ollama picks it up async
        )
        val saved = postRepository.save(post)
        return ResponseEntity.status(HttpStatus.CREATED).body(saved.toDto())
    }

    // ── Stats ─────────────────────────────────────────────────────────────────

    @GetMapping("/stats")
    fun getStats(): StatsDto = StatsDto(
        totalTechnical = postRepository.countByClassification(PostClassification.TECHNICAL),
        totalSpam      = postRepository.countByClassification(PostClassification.SPAM),
        totalPending   = postRepository.countByClassification(PostClassification.PENDING),
        totalError     = postRepository.countByClassification(PostClassification.ERROR),
        // ✅ NEW: populated via MongoDB aggregation (was a stub before)
        sourceCounts   = postRepository.countGroupedBySource(PostClassification.TECHNICAL)
            .associate { it.getId() to it.getCount() },
    )

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun validateSecret(secret: String, request: HttpServletRequest): Boolean {
        val valid = secret == apiProperties.chromeExtensionSecret &&
                    secret != "dev-secret-change-in-prod"   // block default insecure value
        if (!valid) {
            log.warn("Invalid extension secret attempt from ${request.remoteAddr}")
        }
        return valid
    }
}
