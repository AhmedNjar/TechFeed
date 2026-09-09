package com.techfeed.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.techfeed.data.model.FeedItem
import com.techfeed.ui.theme.TechFeedColors
import com.techfeed.ui.theme.TechFeedTheme
import com.techfeed.ui.theme.TechFeedType
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun FeedCard(
    item: FeedItem,
    isBookmarked: Boolean,
    onClick: () -> Unit,
    onBookmark: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
    index: Int = 0,
) {
    val cardShape = RoundedCornerShape(16.dp)

    Card(
        modifier  = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape     = cardShape,
        colors    = CardDefaults.cardColors(containerColor = TechFeedColors.Surface800),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, TechFeedColors.Surface600, cardShape),
        ) {
            // Left accent stripe
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(3.dp)
                    .height(80.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(TechFeedColors.Accent, TechFeedColors.AccentDim)
                        )
                    )
            )

            Column(
                modifier = Modifier.padding(
                    start = 16.dp, end = 12.dp, top = 14.dp, bottom = 8.dp
                )
            ) {
                // Source + read time
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    SourceBadge(sourceName = item.source)
                    if (item.readTimeMinutes > 0) {
                        Text(
                            text  = "${item.readTimeMinutes} min read",
                            style = TechFeedType.Caption,
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Content row
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text     = item.title,
                            style    = TechFeedType.Title,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (item.summary.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text     = item.summary,
                                style    = TechFeedType.Body,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    item.imageUrl?.let { url ->
                        Spacer(Modifier.width(12.dp))
                        AsyncImage(
                            model              = url,
                            contentDescription = "Thumbnail",
                            contentScale       = ContentScale.Crop,
                            modifier           = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(TechFeedColors.Surface700),
                        )
                    }
                }

                // Tags
                if (item.tags.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item.tags.take(3).forEach { tag -> TagPill(label = tag) }
                    }
                }

                // Action row
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    IconButton(onClick = onShare, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector        = Icons.Rounded.Share,
                            contentDescription = "Share",
                            tint               = TechFeedColors.TextMuted,
                            modifier           = Modifier.size(18.dp),
                        )
                    }
                    AnimatedBookmarkIcon(
                        isBookmarked = isBookmarked,
                        onClick      = onBookmark,
                    )
                }
            }
        }
    }
}

@Composable
private fun SourceBadge(sourceName: String) {
    val dotColor = when (sourceName.lowercase()) {
        "reddit"       -> TechFeedColors.RedditOrange
        "dev.to"       -> TechFeedColors.DevToBlue
        "medium"       -> TechFeedColors.MediumGreen
        "hacker news"  -> TechFeedColors.HNOrange
        else           -> TechFeedColors.TextSecondary
    }
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(sourceName, style = TechFeedType.Caption, color = TechFeedColors.TextMuted)
    }
}

@Composable
fun TagPill(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(TechFeedColors.AccentGlow)
            .border(1.dp, TechFeedColors.AccentDim.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
    ) {
        Text(text = "#$label", style = TechFeedType.Mono)
    }
}

@Composable
fun FeedCardSkeleton(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue  = 0.4f,
        targetValue   = 0.8f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shimmerAlpha",
    )
    val shimmer = TechFeedColors.Surface700.copy(alpha = alpha)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = TechFeedColors.Surface800),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(Modifier.width(100.dp).height(10.dp).clip(RoundedCornerShape(4.dp)).background(shimmer))
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(4.dp)).background(shimmer))
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth(0.7f).height(14.dp).clip(RoundedCornerShape(4.dp)).background(shimmer))
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(4.dp)).background(shimmer))
        }
    }
}

@Preview
@Composable
fun FeedCardPreview() {
    val sampleItem = FeedItem(
        id = "1",
        title = "Modern Android Development in 2024",
        summary = "Explore the latest trends in Jetpack Compose and KMP.",
        url = "https://example.com/1",
        source = "Dev.to",
        tags = listOf("Android", "Kotlin"),
        readTimeMinutes = 5
    )
    TechFeedTheme {
        Box(Modifier.padding(16.dp)) {
            FeedCard(
                item = sampleItem,
                isBookmarked = false,
                onClick = {},
                onBookmark = {},
                onShare = {}
            )
        }
    }
}
