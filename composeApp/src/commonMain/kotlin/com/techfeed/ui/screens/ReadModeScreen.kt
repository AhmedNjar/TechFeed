package com.techfeed.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.key.Key.Companion.R
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.techfeed.data.model.FeedItem
import com.techfeed.data.model.FeedSource
import com.techfeed.data.model.displayName
import com.techfeed.presentation.FeedIntent
import com.techfeed.ui.components.ContentRenderer
import com.techfeed.ui.components.TagPill
import com.techfeed.ui.theme.TechFeedColors
import com.techfeed.ui.theme.TechFeedTheme
import com.techfeed.ui.theme.TechFeedType
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import techfeed.composeapp.generated.resources.Res
import techfeed.composeapp.generated.resources.outline_open_in_browser_24
import techfeed.composeapp.generated.resources.round_bookmark_24
import techfeed.composeapp.generated.resources.rounded_bookmark_24

@Composable
fun ReadModeScreen(
    item: FeedItem,
    isBookmarked: Boolean,
    onIntent: (FeedIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uriHandler  = LocalUriHandler.current
    val scrollState = rememberScrollState()

    val topBarAlpha by remember {
        derivedStateOf { (scrollState.value / 200f).coerceIn(0f, 1f) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TechFeedColors.Surface900),
    ) {
        // ── Scrollable content ─────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
        ) {
            // Hero image
            item.imageUrl?.let { url ->
                Box(Modifier.fillMaxWidth().height(260.dp)) {
                    AsyncImage(
                        model              = url,
                        contentDescription = "Hero image",
                        contentScale       = ContentScale.Crop,
                        modifier           = Modifier.fillMaxSize(),
                    )
                    Box(
                        modifier = Modifier.fillMaxSize().background(
                            Brush.verticalGradient(
                                0.4f to TechFeedColors.Surface900.copy(alpha = 0f),
                                1.0f to TechFeedColors.Surface900,
                            )
                        )
                    )
                }
            } ?: Spacer(Modifier.height(80.dp))

            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            ) {
                // Source + read time
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Text(item.source.displayName.uppercase(), style = TechFeedType.Mono)
                    if (item.readTimeMinutes > 0)
                        Text("${item.readTimeMinutes} min read", style = TechFeedType.Caption)
                }

                Spacer(Modifier.height(12.dp))
                Text(item.title, style = TechFeedType.DisplayLarge)
                Spacer(Modifier.height(8.dp))
                if (item.publishedAt.isNotBlank())
                    Text(item.publishedAt, style = TechFeedType.Caption)
                Spacer(Modifier.height(20.dp))
                HorizontalDivider(color = TechFeedColors.Surface600)
                Spacer(Modifier.height(20.dp))

                // ── ⭐ ContentRenderer — zero Markdown, zero WebView ─────────
                if (item.contentBlocks.isNotEmpty()) {
                    ContentRenderer(
                        blocks   = item.contentBlocks,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else if (item.summary.isNotBlank()) {
                    // Fallback: plain summary text
                    Text(item.summary, style = TechFeedType.Body)
                }

                Spacer(Modifier.height(28.dp))

                // Tags
                if (item.tags.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier              = Modifier.fillMaxWidth(),
                    ) {
                        item.tags.forEach { tag -> TagPill(label = tag) }
                    }
                    Spacer(Modifier.height(28.dp))
                }

                // Open full article
                Button(
                    onClick  = { uriHandler.openUri(item.url) },
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = TechFeedColors.Accent,
                        contentColor   = TechFeedColors.Surface900,
                    ),
                ) {
                    Icon(painter = painterResource(Res.drawable.outline_open_in_browser_24), contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Read Full Article", style = TechFeedType.Body.copy(color = TechFeedColors.Surface900))
                }

                Spacer(Modifier.height(40.dp))
            }
        }

        // ── Top action bar ─────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(TechFeedColors.Surface900.copy(alpha = topBarAlpha))
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 4.dp),
        ) {
            IconButton(
                onClick  = { onIntent(FeedIntent.DismissDetail) },
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "Close",
                    tint = TechFeedColors.TextPrimary)
            }

            Row(modifier = Modifier.align(Alignment.CenterEnd)) {
                IconButton(onClick = { onIntent(FeedIntent.ShareItem(item)) }) {
                    Icon(Icons.Rounded.Share, contentDescription = "Share",
                        tint = TechFeedColors.TextPrimary)
                }
                IconButton(onClick = { onIntent(FeedIntent.ToggleBookmark(item)) }) {
                    Icon(
                        painter = if (isBookmarked) painterResource(Res.drawable.round_bookmark_24)
                                      else          painterResource(Res.drawable.rounded_bookmark_24),
                        contentDescription = "Bookmark",
                        tint = if (isBookmarked) TechFeedColors.Accent else TechFeedColors.TextPrimary,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun ReadModeScreenPreview() {
    val sampleItem = FeedItem(
        id = "1",
        title = "The Future of Kotlin Multiplatform",
        summary = "Explore how KMP is revolutionizing cross-platform development with shared logic and native UIs.",
        url = "https://kotlinlang.org/lp/multiplatform/",
        source = FeedSource.DEV_TO,
        tags = listOf("Kotlin", "KMP", "Mobile"),
        readTimeMinutes = 12,
        imageUrl = "https://example.com/image.jpg"
    )

    TechFeedTheme {
        ReadModeScreen(
            item = sampleItem,
            isBookmarked = true,
            onIntent = {}
        )
    }
}
