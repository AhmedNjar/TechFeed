package com.techhub.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver
import org.springframework.data.mongodb.core.mapping.MongoMappingContext
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import reactor.netty.http.client.HttpClient
import java.time.Duration
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

@Configuration
@EnableAsync
open class AppConfig {

    @Bean
    fun objectMapper(): ObjectMapper = ObjectMapper()
        .registerKotlinModule()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    @Bean
    fun corsConfigurer(): WebMvcConfigurer = object : WebMvcConfigurer {
        override fun addCorsMappings(registry: CorsRegistry) {
            registry.addMapping("/api/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600)
        }

        override fun addViewControllers(registry: org.springframework.web.servlet.config.annotation.ViewControllerRegistry) {
            registry.addRedirectViewController("/admin", "/admin/index.html")
        }
    }

    @EventListener(ApplicationReadyEvent::class)
    fun ensureIndexes(event: ApplicationReadyEvent) {
        val mongoTemplate  = event.applicationContext.getBean(MongoTemplate::class.java)
        val mappingContext = event.applicationContext.getBean(MongoMappingContext::class.java)
        val resolver       = MongoPersistentEntityIndexResolver(mappingContext)

        listOf(com.techhub.domain.document.Post::class.java).forEach { entityClass ->
            try {
                val indexOps = mongoTemplate.indexOps(entityClass)
                resolver.resolveIndexFor(entityClass).forEach { indexOps.ensureIndex(it) }
            } catch (e: Exception) {
                org.slf4j.LoggerFactory.getLogger(AppConfig::class.java)
                    .warn("Could not ensure index for $entityClass: ${e.message}")
            }
        }
    }

    @Bean("processingExecutor")
    fun processingExecutor(): Executor = ThreadPoolTaskExecutor().apply {
        corePoolSize    = 2
        maxPoolSize     = 2
        queueCapacity   = 50
        threadNamePrefix = "ollama-"
        initialize()
    }

    @Bean
    fun ollamaHealthIndicator(
        ollamaProperties: OllamaProperties,
        @Qualifier("ollamaWebClient") ollamaClient: WebClient,
    ): HealthIndicator = HealthIndicator {
        try {
            val response = ollamaClient.get().uri("/api/tags")
                .retrieve()
                .toBodilessEntity()
                .timeout(Duration.ofSeconds(3))
                .block()
            if (response?.statusCode?.is2xxSuccessful == true)
                Health.up().withDetail("url", ollamaProperties.baseUrl).build()
            else
                Health.down().withDetail("url", ollamaProperties.baseUrl).build()
        } catch (e: Exception) {
            Health.down(e).withDetail("url", ollamaProperties.baseUrl).build()
        }
    }

    // ── Scraper Service Health Indicator ───────────────────────────────────────
    @Bean
    fun scraperHealthIndicator(
        scraperProperties: ScraperProperties,
    ): HealthIndicator = HealthIndicator {
        try {
            val client = WebClient.create(scraperProperties.baseUrl)
            val response = client.get().uri("/health")
                .retrieve()
                .toBodilessEntity()
                .timeout(Duration.ofSeconds(3))
                .block()
            if (response?.statusCode?.is2xxSuccessful == true)
                Health.up().withDetail("url", scraperProperties.baseUrl).build()
            else
                Health.down().withDetail("url", scraperProperties.baseUrl).build()
        } catch (e: Exception) {
            Health.down(e).withDetail("url", scraperProperties.baseUrl).build()
        }
    }

    // ── WebClient beans ───────────────────────────────────────────────────────

    @Bean("webClient")
    fun webClient(objectMapper: ObjectMapper): WebClient {
        val httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
            .responseTimeout(Duration.ofSeconds(30))
            .doOnConnected { conn ->
                conn.addHandlerLast(ReadTimeoutHandler(30, TimeUnit.SECONDS))
            }

        val strategies = ExchangeStrategies.builder()
            .codecs { config ->
                config.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)
                config.defaultCodecs().jackson2JsonEncoder(Jackson2JsonEncoder(objectMapper))
                config.defaultCodecs().jackson2JsonDecoder(Jackson2JsonDecoder(objectMapper))
            }.build()

        return WebClient.builder()
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .exchangeStrategies(strategies)
            .build()
    }

    @Bean("ollamaWebClient")
    fun ollamaWebClient(
        objectMapper: ObjectMapper,
        ollamaProperties: OllamaProperties,
    ): WebClient {
        val httpClient = HttpClient.create()
            .responseTimeout(Duration.ofSeconds(ollamaProperties.timeoutSeconds))

        return WebClient.builder()
            .baseUrl(ollamaProperties.baseUrl)
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .defaultHeader("Content-Type", "application/json")
            .build()
    }
}
