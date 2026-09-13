package com.techhub.ingestion.content

import com.techhub.domain.document.ContentBlock
import com.techhub.domain.document.MediaItem
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import java.net.URI

/**
 * Converts untrusted RSS/Atom descriptions into a safe, presentation-neutral
 * representation. It deliberately does no fetching: a feed must never cause
 * arbitrary URLs to be requested merely because it contains an HTML tag.
 */
class FeedContentParser {

    fun parse(
        rawContent: String,
        entryUrl: String?,
        entryAuthor: String? = null,
    ): ParsedFeedContent {
        val baseUrl = entryUrl.orEmpty()
        val document = Jsoup.parseBodyFragment(rawContent, baseUrl)
        val media = LinkedHashMap<String, MediaItem>()

        document.select("img").forEach { image ->
            val url = imageUrl(image, baseUrl) ?: return@forEach
            media.putIfAbsent("image:$url", MediaItem.Image(url, image.attr("alt").trim().ifBlank { null }))
        }
        document.select("video").forEach { video ->
            val stream = firstUrl(video.attr("src"), baseUrl)
                ?: video.select("source[src]").firstNotNullOfOrNull { firstUrl(it.attr("src"), baseUrl) }
                ?: return@forEach
            val thumbnail = firstUrl(video.attr("poster"), baseUrl)
            media.putIfAbsent("video:$stream", MediaItem.Video(stream, thumbnail))
        }
        document.select("iframe[src]").forEach { frame ->
            val stream = firstUrl(frame.attr("src"), baseUrl) ?: return@forEach
            if (isVideoEmbed(stream)) media.putIfAbsent("video:$stream", MediaItem.Video(stream, null))
        }

        val links = document.select("a[href]").mapNotNull { anchor ->
            firstUrl(anchor.attr("href"), baseUrl)?.let { it to anchor.text().trim() }
        }.toMutableList()
        markdownLinks(rawContent, baseUrl).forEach { (url, caption, isImage) ->
            if (isImage) media.putIfAbsent("image:$url", MediaItem.Image(url, caption.ifBlank { null }))
            links += url to caption
        }
        // Bare URL extraction is for Markdown/plain-text feeds only. Attributes
        // have already been processed above and would otherwise turn a video's
        // poster into a duplicate standalone image.
        bareMediaUrls(rawContent.replace(Regex("<[^>]+>"), " "), baseUrl).forEach { url ->
            if (isImageUrl(url)) media.putIfAbsent("image:$url", MediaItem.Image(url))
            if (isVideoUrl(url)) media.putIfAbsent("video:$url", MediaItem.Video(url))
        }
        links.forEach { (url, caption) ->
            when {
                isImageUrl(url) -> media.putIfAbsent("image:$url", MediaItem.Image(url, caption.ifBlank { null }))
                isVideoUrl(url) || isVideoEmbed(url) -> media.putIfAbsent(
                    "video:$url",
                    MediaItem.Video(url, videoThumbnail(url)),
                )
            }
        }

        val entry = entryUrl?.let { firstUrl(it, baseUrl) }
        val comments = links.firstOrNull { (url, _) -> isDiscussionUrl(url) }?.first
            ?: entry?.takeIf(::isDiscussionUrl)
        val externalArticle = links.firstOrNull { (url, _) ->
            !isDiscussionUrl(url) && !isRedditUrl(url) && !isMediaUrl(url)
        }?.first
        val article = if (entry != null && isDiscussionUrl(entry)) externalArticle else entry
            ?: document.selectFirst("link[rel=canonical][href]")?.attr("href")?.let { firstUrl(it, baseUrl) }
            ?: externalArticle

        document.select("script, style, noscript, picture, img, video, audio, iframe, source").remove()
        val hasHtml = Regex("<[/!]?[a-zA-Z][^>]*>").containsMatchIn(rawContent)
        val contentBlocks = if (hasHtml) htmlBlocks(document) else markdownBlocks(rawContent)
        val resolvedBlocks = contentBlocks.ifEmpty { markdownBlocks(document.body().text()) }
        val cleanBody = resolvedBlocks
            .joinToString("\n\n") { it.value }
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()

        return ParsedFeedContent(
            cleanBodyText = cleanBody,
            contentBlocks = resolvedBlocks,
            mediaList = media.values.toList(),
            articleUrl = article,
            commentsUrl = comments,
            author = entryAuthor?.trim()?.ifBlank { null }
                ?: document.selectFirst("meta[name=author]")?.attr("content")?.trim()?.ifBlank { null }
                ?: document.selectFirst("a[href*='/user/'], .author, [rel=author]")?.text()?.trim()?.ifBlank { null },
        )
    }

    private fun imageUrl(element: org.jsoup.nodes.Element, baseUrl: String): String? =
        sequenceOf("src", "data-src", "data-original", "data-url")
            .map { element.attr(it) }
            .mapNotNull { firstUrl(it, baseUrl) }
            .firstOrNull()
            ?: element.attr("srcset").substringBefore(',').substringBefore(' ').let { firstUrl(it, baseUrl) }

    private fun markdownLinks(raw: String, baseUrl: String): List<MarkdownLink> {
        val pattern = Regex("""(!)?\[([^]]*)]\(([^\s)]+)(?:\s+"[^"]*")?\)""")
        return pattern.findAll(raw).mapNotNull { match ->
            firstUrl(match.groupValues[3], baseUrl)?.let {
                MarkdownLink(url = it, caption = match.groupValues[2].trim(), isImage = match.groupValues[1] == "!")
            }
        }.toList()
    }

    private fun bareMediaUrls(raw: String, baseUrl: String): List<String> =
        Regex("""https?://[^\s<>'"]+""")
            .findAll(raw)
            .mapNotNull { firstUrl(it.value.trimEnd('.', ',', ')'), baseUrl) }
            .filter(::isMediaUrl)
            .toList()

    private fun htmlBlocks(document: org.jsoup.nodes.Document): List<ContentBlock> {
        val blocks = document.select("pre, blockquote, h1, h2, h3, h4, h5, h6, p, li")
            .filterNot { element -> element.parents().any { parent ->
                parent.tagName() in setOf("pre", "blockquote", "p", "li")
            } }
            .mapNotNull { element ->
                val text = (if (element.tagName() == "pre") {
                    element.selectFirst("code")?.wholeText() ?: element.wholeText()
                } else element.text()).trim()
                toBlock(element.tagName(), text)
            }
        return blocks.ifEmpty {
            document.body().wholeText()
                .lineSequence()
                .map(::cleanInlineMarkdown)
                .filter(::isContentLine)
                .map { ContentBlock(type = "text", value = it) }
                .toList()
        }
    }

    private fun markdownBlocks(raw: String): List<ContentBlock> {
        val blocks = mutableListOf<ContentBlock>()
        val paragraph = mutableListOf<String>()
        var inCode = false
        val code = mutableListOf<String>()

        fun flushParagraph() {
            val value = paragraph.joinToString(" ").trim()
            if (value.isNotBlank() && isContentLine(value)) {
                blocks += ContentBlock(type = "text", value = value)
            }
            paragraph.clear()
        }
        fun flushCode() {
            if (code.isNotEmpty()) blocks += ContentBlock(type = "code", value = code.joinToString("\n").trimEnd())
            code.clear()
        }

        raw.lineSequence().forEach { rawLine ->
            val line = rawLine.trimEnd()
            if (line.trim().startsWith("```")) {
                if (inCode) flushCode() else flushParagraph()
                inCode = !inCode
                return@forEach
            }
            if (inCode) {
                code += line
                return@forEach
            }
            val trimmed = cleanInlineMarkdown(line.trim())
            when {
                trimmed.isBlank() -> flushParagraph()
                trimmed.startsWith("#") -> {
                    flushParagraph()
                    val heading = trimmed.trimStart('#').trim()
                    if (heading.isNotBlank()) blocks += ContentBlock(type = "heading", value = heading, style = "h2")
                }
                trimmed.startsWith(">") -> {
                    flushParagraph()
                    blocks += ContentBlock(type = "quote", value = trimmed.removePrefix(">").trim())
                }
                trimmed.matches(Regex("[-*+]\\s+.+")) -> {
                    flushParagraph()
                    blocks += ContentBlock(type = "text", value = "• " + trimmed.drop(1).trim())
                }
                isContentLine(trimmed) -> paragraph += trimmed
            }
        }
        if (inCode) flushCode() else flushParagraph()
        return blocks
    }

    private fun toBlock(tag: String, text: String): ContentBlock? {
        val normalized = if (tag == "pre") text.trimIndent().trim() else text
        if (!isContentLine(normalized)) return null
        return when (tag) {
            "pre" -> ContentBlock(type = "code", value = normalized)
            "blockquote" -> ContentBlock(type = "quote", value = normalized)
            "h1", "h2", "h3", "h4", "h5", "h6" -> ContentBlock(type = "heading", value = normalized, style = tag)
            "li" -> ContentBlock(type = "text", value = "• $normalized")
            else -> ContentBlock(type = "text", value = normalized)
        }
    }

    private fun cleanInlineMarkdown(value: String): String = value
        .replace(Regex("!\\[([^]]*)]\\([^)]*\\)"), "$1")
        .replace(Regex("\\[([^]]+)]\\([^)]*\\)"), "$1")
        .replace(Regex("(`{1,3})(.*?)\\1"), "$2")
        .replace(Regex("(\\*\\*|__)(.*?)\\1"), "$2")
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun isContentLine(value: String): Boolean {
        val normalized = value.trim().lowercase()
        return normalized.isNotBlank() &&
            !normalized.startsWith("submitted by") &&
            !normalized.matches(Regex("^\\[(link|comments?)].*"))
    }

    private fun firstUrl(value: String, baseUrl: String): String? {
        val decoded = Parser.unescapeEntities(value, false)
            .replace("\\u0026", "&")
            .trim()
        if (decoded.isBlank() || decoded.startsWith("data:", ignoreCase = true)) return null
        return runCatching {
            val resolved = URI(baseUrl.ifBlank { "https://invalid.local" }).resolve(decoded).normalize().toString()
            resolved.takeIf { it.startsWith("https://") || it.startsWith("http://") }
        }.getOrNull()
    }

    private fun isImageUrl(url: String) = url.substringBefore('?').lowercase()
        .matches(Regex(".*\\.(png|jpe?g|webp|gif|avif)$")) || url.contains("preview.redd.it/") || url.contains("i.redd.it/")

    private fun isVideoUrl(url: String) = url.substringBefore('?').lowercase()
        .matches(Regex(".*\\.(mp4|webm|m3u8|mov)$")) || url.contains("v.redd.it/")

    private fun isVideoEmbed(url: String) = isVideoUrl(url) || listOf("youtube.com", "youtu.be", "vimeo.com", "redditmedia.com")
        .any(url::contains)

    private fun videoThumbnail(url: String): String? {
        val youtubeId = when {
            url.contains("youtu.be/") -> url.substringAfter("youtu.be/").substringBefore('?').substringBefore('/')
            url.contains("youtube.com") -> Regex("[?&]v=([^&]+)").find(url)?.groupValues?.getOrNull(1)
            else -> null
        }
        return youtubeId?.takeIf(String::isNotBlank)?.let { "https://img.youtube.com/vi/$it/hqdefault.jpg" }
    }

    private fun isMediaUrl(url: String) = isImageUrl(url) || isVideoUrl(url) || isVideoEmbed(url)
    private fun isRedditUrl(url: String) = url.contains("reddit.com") || url.contains("redd.it")
    private fun isDiscussionUrl(url: String) = url.contains("reddit.com") && url.contains("/comments/")

    private data class MarkdownLink(val url: String, val caption: String, val isImage: Boolean)
}

data class ParsedFeedContent(
    val cleanBodyText: String,
    val contentBlocks: List<ContentBlock>,
    val mediaList: List<MediaItem>,
    val articleUrl: String?,
    val commentsUrl: String?,
    val author: String?,
)
