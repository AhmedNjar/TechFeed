package com.techhub.ingestion.content

import com.techhub.domain.document.MediaItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FeedContentParserTest {
    private val parser = FeedContentParser()

    @Test
    fun `normalizes html media and separates article from reddit discussion`() {
        val result = parser.parse(
            rawContent = """
                <p>A post about Compose.</p>
                <a href="https://news.example/article?ref=feed&amp;lang=en"><img src="https://preview.redd.it/card.jpg?width=640&amp;crop=smart" alt="Card" /></a>
                <video poster="https://cdn.example/thumb.jpg"><source src="https://cdn.example/demo.mp4" /></video>
                <a href="https://www.reddit.com/r/kotlin/comments/abc/thread/">Discussion</a>
            """.trimIndent(),
            entryUrl = "https://www.reddit.com/r/kotlin/comments/abc/thread/",
            entryAuthor = "u/alice",
        )

        assertEquals("A post about Compose.", result.cleanBodyText)
        assertEquals("https://news.example/article?ref=feed&lang=en", result.articleUrl)
        assertEquals("https://www.reddit.com/r/kotlin/comments/abc/thread/", result.commentsUrl)
        assertEquals("u/alice", result.author)
        assertEquals(
            listOf(
                MediaItem.Image("https://preview.redd.it/card.jpg?width=640&crop=smart", "Card"),
                MediaItem.Video("https://cdn.example/demo.mp4", "https://cdn.example/thumb.jpg"),
            ),
            result.mediaList,
        )
        assertEquals("text", result.contentBlocks.single().type)
    }

    @Test
    fun `extracts markdown and direct media links`() {
        val result = parser.parse(
            rawContent = """
                ![Diagram](https://i.redd.it/diagram.png?x=1&amp;y=2)
                Watch https://cdn.example/clip.webm and read [the article](https://example.com/post).
            """.trimIndent(),
            entryUrl = "https://feed.example/item",
        )

        assertTrue(result.cleanBodyText.contains("Diagram"))
        // Non-discussion feeds retain their entry URL as the canonical article.
        assertEquals("https://feed.example/item", result.articleUrl)
        assertEquals(
            listOf(
                MediaItem.Image("https://i.redd.it/diagram.png?x=1&y=2", "Diagram"),
                MediaItem.Video("https://cdn.example/clip.webm", null),
            ),
            result.mediaList,
        )
    }

    @Test
    fun `preserves html code blocks and removes reddit attribution footer`() {
        val result = parser.parse(
            rawContent = """
                <p>Use this parser:</p>
                <pre><code>val answer = 42
                println(answer)</code></pre>
                <p>Submitted by <a href="https://reddit.com/u/example">u/example</a></p>
            """.trimIndent(),
            entryUrl = "https://www.reddit.com/r/kotlin/comments/abc/thread/",
        )

        assertEquals(listOf("text", "code"), result.contentBlocks.map { it.type })
        assertEquals("val answer = 42\nprintln(answer)", result.contentBlocks[1].value)
        assertTrue(result.cleanBodyText.contains("Use this parser:"))
        assertTrue(!result.cleanBodyText.contains("Submitted by"))
    }
}
