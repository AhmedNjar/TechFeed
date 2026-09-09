package com.techfeed.data.remote

import com.techfeed.data.model.FeedItem
import com.techfeed.data.model.FeedResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

private const val BASE_URL = "http://localhost:8080/api/"

fun buildHttpClient(engine: HttpClientEngine): HttpClient = HttpClient(engine) {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient         = true
            explicitNulls     = false
        })
    }
    install(Logging) {
        logger = Logger.DEFAULT
        level  = LogLevel.HEADERS
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 15_000
        connectTimeoutMillis = 10_000
    }
    defaultRequest {
        url(BASE_URL)
        contentType(ContentType.Application.Json)
    }
}

// ── API Client ────────────────────────────────────────────────────────────────

class FeedApiClient(private val client: HttpClient) {

    suspend fun getFeed(
        tag: String = "all",
        page: Int   = 0,
        size: Int   = 20,
    ): Result<FeedResponse> = runCatching {
        val dto: PostPageResponse = client.get("posts") {
            parameter("tag",  tag)
            parameter("page", page)
            parameter("size", size)
        }.body()
        dto.toFeedResponse()
    }

    suspend fun getItem(id: String): Result<FeedItem> = runCatching {
        client.get("posts/$id").body()
    }

    /** ✅ NEW: backs RelatedArticlesRepository */
    suspend fun getRelated(id: String, limit: Int = 5): Result<List<FeedItem>> = runCatching {
        client.get("posts/$id/related") {
            parameter("limit", limit)
        }.body()
    }
}

// ── Backend response shape ────────────────────────────────────────────────────

@kotlinx.serialization.Serializable
private data class PostPageResponse(
    val items: List<FeedItem>,
    val page: Int,
    val totalPages: Int,
    val hasNext: Boolean,
    val totalItems: Long = 0,
)

private fun PostPageResponse.toFeedResponse() = FeedResponse(
    items      = items,
    page       = page,
    totalPages = totalPages,
    hasNext    = hasNext,
)

// ── Platform engine — expect/actual ───────────────────────────────────────────
expect fun createHttpEngine(): HttpClientEngine
