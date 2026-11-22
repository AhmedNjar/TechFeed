package com.techhub.ai

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.techhub.config.OllamaProperties
import com.techhub.domain.document.ContentBlock
import com.techhub.domain.document.PostClassification
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.scheduler.Schedulers
import java.time.Duration

@Service
class OllamaService(
    @Qualifier("ollamaWebClient")
    private val webClient: WebClient,
    private val properties: OllamaProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun analyze(title: String, content: String): AiResult {
        return try {
            val raw = callOllama(buildPrompt(title, content))
            parseResponse(raw)
        } catch (ex: Exception) {
            log.error("Ollama analysis failed for '$title': ${ex.message}")
            AiResult(
                summary        = "",
                contentBlocks  = emptyList(),
                classification = PostClassification.ERROR,
                rawResponse    = ex.message ?: "unknown error",
            )
        }
    }

    private fun buildPrompt(title: String, content: String): String {
        val truncated = content.take(4000)
        return """
You are a senior software engineer and tech content curator.
Analyze the following article and respond ONLY with a single valid JSON object.
No markdown fences, no explanation, no extra text — just the JSON.

ARTICLE TITLE: $title

ARTICLE CONTENT:
$truncated

RESPOND WITH EXACTLY THIS JSON STRUCTURE:
{
  "classification": "TECHNICAL" or "SPAM",
  "summary": "one plain-text sentence, max 200 chars, for the card preview",
  "content_blocks": [
    { "type": "heading", "value": "Section Title", "style": "h2", "url": "" },
    { "type": "text",    "value": "paragraph text here", "style": "normal", "url": "" },
    { "type": "code",    "value": "val example = true", "style": "", "url": "" },
    { "type": "image",   "value": "", "style": "", "url": "https://image-url-if-any" },
    { "type": "quote",   "value": "quoted text here", "style": "", "url": "" },
    { "type": "divider", "value": "", "style": "", "url": "" }
  ]
}

CLASSIFICATION RULES:
- TECHNICAL: programming, software engineering, AI/ML, mobile dev, DevOps, hardware, robotics, open source
- SPAM: clickbait, off-topic, marketing without technical content, low quality

CONTENT_BLOCKS RULES (only if TECHNICAL, else return empty array []):
- Start with a heading block: { "type":"heading", "value":"TL;DR", "style":"h2" }
- Follow with 1-2 text blocks summarizing the key idea
- Add a code block if the article mentions code
- Add image blocks only if the article contains real image URLs
- Add a divider block before "Key Takeaways"
- End with a heading "Key Takeaways" and 3-5 short text bullets
- Max 10 blocks total — keep it concise for mobile reading
- ALL four fields (type, value, style, url) must always be present in every block
        """.trimIndent()
    }

    // ── HTTP Call ─────────────────────────────────────────────────────────────

    private fun callOllama(prompt: String): String {
        val response = webClient.post()
            .uri("/api/generate")
            .bodyValue(OllamaRequest(model = properties.model, prompt = prompt, stream = false))
            .retrieve()
            .bodyToMono<OllamaResponse>()
            .timeout(Duration.ofSeconds(properties.timeoutSeconds))
            .publishOn(Schedulers.boundedElastic())
            .block()
            ?: throw IllegalStateException("Empty response from Ollama")

        return response.response
    }

    private fun parseResponse(raw: String): AiResult {
        return try {
            val cleaned = raw
                .trimStart()
                .removePrefix("```json")
                .removePrefix("```")
                .trimEnd()
                .removeSuffix("```")
                .trim()

            val jsonStart = cleaned.indexOf('{')
            val jsonEnd   = cleaned.lastIndexOf('}')
            if (jsonStart == -1 || jsonEnd == -1) throw IllegalArgumentException("No JSON object found in response")
            val jsonStr = cleaned.substring(jsonStart, jsonEnd + 1)

            val node = MAPPER.readTree(jsonStr)

            val classification = when (node.get("classification")?.asText()?.uppercase()) {
                "TECHNICAL" -> PostClassification.TECHNICAL
                "SPAM"      -> PostClassification.SPAM
                else        -> PostClassification.PENDING
            }

            val summary = node.get("summary")?.asText()?.take(200) ?: ""

            val contentBlocks: List<ContentBlock> = try {
                val blocksNode = node.get("content_blocks")
                if (blocksNode != null && blocksNode.isArray)
                    MAPPER.readValue<List<ContentBlock>>(blocksNode.toString())
                else emptyList()
            } catch (e: Exception) {
                log.warn("Failed to parse content_blocks: ${e.message}")
                emptyList()
            }

            AiResult(
                summary        = summary,
                contentBlocks  = contentBlocks,
                classification = classification,
                rawResponse    = raw,
            )
        } catch (ex: Exception) {
            log.warn("Failed to parse Ollama response: ${ex.message}. Raw: ${raw.take(300)}")
            AiResult(
                summary        = raw.take(200),
                contentBlocks  = emptyList(),
                classification = PostClassification.PENDING,
                rawResponse    = raw,
            )
        }
    }

    companion object {
        private val MAPPER: ObjectMapper = ObjectMapper()
            .registerModule(KotlinModule.Builder().build())
    }
}

data class AiResult(
    val summary: String,
    val contentBlocks: List<ContentBlock>,
    val classification: PostClassification,
    val rawResponse: String,
)

data class OllamaRequest(
    val model: String,
    val prompt: String,
    val stream: Boolean = false,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OllamaResponse(
    val model: String    = "",
    val response: String = "",
    val done: Boolean    = false,
)
