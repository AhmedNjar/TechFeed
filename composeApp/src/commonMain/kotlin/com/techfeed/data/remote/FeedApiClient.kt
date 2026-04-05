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

private const val BASE_URL = "https://api.yourdomain.com/v1/"

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
        client.get("feed") {
            parameter("tag",  tag)
            parameter("page", page)
            parameter("size", size)
        }.body()
    }

    suspend fun getItem(id: String): Result<FeedItem> = runCatching {
        client.get("feed/$id").body()
    }
}

// ── Platform engine — expect/actual ───────────────────────────────────────────
expect fun createHttpEngine(): HttpClientEngine
