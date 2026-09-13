package com.techfeed.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.techfeed.data.model.FeedItem
import com.techfeed.presentation.FeedIntent
import com.techfeed.ui.components.AnimatedBookmarkIcon
import com.techfeed.ui.components.ContentRenderer
import com.techfeed.ui.components.MediaCarousel
import com.techfeed.ui.components.TagPill
import com.techfeed.ui.theme.TechFeedColors
import com.techfeed.ui.theme.TechFeedTheme
import com.techfeed.ui.theme.TechFeedType
import com.techfeed.ui.util.CompactWindowSizeInfo
import com.techfeed.ui.util.WindowSizeInfo
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import techfeed.composeapp.generated.resources.Res
import techfeed.composeapp.generated.resources.outline_open_in_browser_24

/**
 * The reading-width cap applied on Medium/Expanded windows — an unconstrained
 * line length on a tablet or desktop-wide pane is genuinely harder to read,
 * so the text column centers itself within this max width instead of
 * stretching edge to edge. The hero image is unaffected (still spans the
 * full pane) since a wide image reads fine either way.
 */
private val ReadingMaxWidth = 720.dp
private val DetailTitleStyle = TechFeedType.DisplayLarge.copy(fontSize = 28.sp, lineHeight = 34.sp)

@Composable
fun ReadModeScreen(
    item: FeedItem,
    isBookmarked: Boolean,
    relatedArticles: List<FeedItem>,
    isRelatedLoading: Boolean,
    bookmarkIds: Set<String>,
    onIntent: (FeedIntent) -> Unit,
    modifier: Modifier = Modifier,
    windowSize: WindowSizeInfo = CompactWindowSizeInfo,
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
            // Keep legacy items readable. Normalized media is rendered separately
            // below, so it is not duplicated as the old single-image hero.
            if (item.mediaList.isEmpty()) item.imageUrl?.let { url ->
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
            } else if (item.mediaList.isEmpty()) Spacer(Modifier.height(104.dp))

            // ── Reading column — centered and width-capped on wide windows ───
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (windowSize.isWide) Modifier.widthIn(max = ReadingMaxWidth) else Modifier)
                        .padding(horizontal = 24.dp),
                ) {
                    // Source + read time
                    // FIX: was `item.source.displayName.uppercase()` — `FeedSource`
                    // and its `displayName` extension were removed from the model
                    // (source is a plain free-text String now), so this didn't
                    // compile. `item.source` is already the display string.
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically,
                    ) {
                        Text(item.source.uppercase(), style = TechFeedType.Mono)
                        if (item.readTimeMinutes > 0)
                            Text("${item.readTimeMinutes} min read", style = TechFeedType.Caption)
                    }

                    Spacer(Modifier.height(12.dp))
                    Text(item.title, style = DetailTitleStyle)
                    Spacer(Modifier.height(8.dp))
                    if (item.publishedAt.isNotBlank())
                        Text(item.publishedAt, style = TechFeedType.Caption)
                    item.author?.let {
                        Spacer(Modifier.height(4.dp))
                        Text("By $it", style = TechFeedType.Caption)
                    }
                    if (item.mediaList.isNotEmpty()) {
                        Spacer(Modifier.height(20.dp))
                        MediaCarousel(media = item.mediaList, modifier = Modifier.fillMaxWidth())
                    }
                    Spacer(Modifier.height(20.dp))
                    HorizontalDivider(color = TechFeedColors.Surface600)
                    Spacer(Modifier.height(20.dp))

                    // ── ⭐ ContentRenderer — zero Markdown, zero WebView, staggered ──
                    if (item.contentBlocks.isNotEmpty()) {
                        ContentRenderer(
                            blocks   = item.contentBlocks,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else if (item.cleanBodyText.isNotBlank()) {
                        Text(item.cleanBodyText, style = TechFeedType.Body)
                    } else if (item.summary.isNotBlank()) {
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
                        onClick  = { uriHandler.openUri(item.articleUrl ?: item.url) },
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

                    item.commentsUrl?.let { commentsUrl ->
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { uriHandler.openUri(commentsUrl) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Open Discussion") }
                    }

                    Spacer(Modifier.height(36.dp))
                }
            }

            // ── Related Articles ─────────────────────────────────────────────
            RelatedArticlesSection(
                articles     = relatedArticles,
                isLoading    = isRelatedLoading,
                onItemClick  = { onIntent(FeedIntent.SelectItem(it)) },
            )

            Spacer(Modifier.height(40.dp))
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

            Row(modifier = Modifier.align(Alignment.CenterEnd), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onIntent(FeedIntent.ShareItem(item)) }) {
                    Icon(Icons.Rounded.Share, contentDescription = "Share",
                        tint = TechFeedColors.TextPrimary)
                }
                AnimatedBookmarkIcon(
                    isBookmarked = isBookmarked,
                    onClick      = { onIntent(FeedIntent.ToggleBookmark(item)) },
                    activeTint   = TechFeedColors.Accent,
                    inactiveTint = TechFeedColors.TextPrimary,
                )
            }
        }
    }
}

// ── Related Articles Section ───────────────────────────────────────────────────

@Composable
private fun RelatedArticlesSection(
    articles: List<FeedItem>,
    isLoading: Boolean,
    onItemClick: (FeedItem) -> Unit,
) {
    if (!isLoading && articles.isEmpty()) return

    Column(modifier = Modifier.padding(top = 8.dp)) {
        HorizontalDivider(color = TechFeedColors.Surface600, modifier = Modifier.padding(horizontal = 24.dp))
        Spacer(Modifier.height(20.dp))

        Text(
            text     = "Related Articles",
            style    = TechFeedType.Title,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(Modifier.height(12.dp))

        if (isLoading) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding        = PaddingValues(horizontal = 24.dp),
            ) {
                items(3) { RelatedCardSkeleton() }
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding        = PaddingValues(horizontal = 24.dp),
            ) {
                itemsIndexed(articles, key = { _, item -> item.id }) { index, related ->
                    RelatedCard(
                        item    = related,
                        index   = index,
                        onClick = { onItemClick(related) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RelatedCard(item: FeedItem, index: Int, onClick: () -> Unit) {
    // Small staggered fade+slide-in from the right as the row appears —
    // matches the ContentRenderer stagger for a consistent "reveal" feel.
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(minOf(index * 60L, 240L))
        appear.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
    }

    Column(
        modifier = Modifier
            .width(200.dp)
            .graphicsLayer {
                alpha        = appear.value
                translationX = (1f - appear.value) * 24.dp.toPx()
            }
            .clip(RoundedCornerShape(12.dp))
            .background(TechFeedColors.Surface800)
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        item.imageUrl?.let { url ->
            AsyncImage(
                model              = url,
                contentDescription = null,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(TechFeedColors.Surface700),
            )
            Spacer(Modifier.height(8.dp))
        }
        Text(
            text     = item.title,
            style    = TechFeedType.Title.copy(fontSize = 13.sp, lineHeight = 17.sp),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        Text(item.source, style = TechFeedType.Caption)
    }
}

@Composable
private fun RelatedCardSkeleton() {
    Box(
        modifier = Modifier
            .width(200.dp)
            .height(150.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(TechFeedColors.Surface800)
    )
}

// ── Preview ────────────────────────────────────────────────────────────────────

@Preview
@Composable
fun ReadModeScreenPreview() {
    // FIX: was `source = FeedSource.DEV_TO` — that enum was removed from the
    // model (source is a plain String now). Also removed an unrelated stray
    // import (`androidx.compose.ui.input.key.Key.Companion.R`) that had
    // nothing to do with this file.
    val sampleItem = FeedItem(
        id = "1",
        title = "The Future of Kotlin Multiplatform",
        summary = "Explore how KMP is revolutionizing cross-platform development with shared logic and native UIs.",
        url = "https://kotlinlang.org/lp/multiplatform/",
        source = "Dev.to",
        tags = listOf("Kotlin", "KMP", "Mobile"),
        readTimeMinutes = 12,
        imageUrl = "https://example.com/image.jpg"
    )

    TechFeedTheme {
        ReadModeScreen(
            item             = sampleItem,
            isBookmarked     = true,
            relatedArticles  = emptyList(),
            isRelatedLoading = false,
            bookmarkIds      = emptySet(),
            onIntent         = {}
        )
    }
}
