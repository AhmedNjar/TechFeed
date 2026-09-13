package com.techfeed.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.techfeed.data.model.FeedItem
import com.techfeed.ui.components.FeedCard
import com.techfeed.ui.components.FeedCardSkeleton
import com.techfeed.ui.components.TagPill
import com.techfeed.ui.theme.*

@Composable
fun ForYouScreen(
    items: List<FeedItem>,
    basedOnTags: List<String>,
    isLoading: Boolean,
    bookmarkIds: Set<String>,
    onClose: () -> Unit,
    onItemClick: (FeedItem) -> Unit,
    onBookmark: (FeedItem) -> Unit,
    onShare: (FeedItem) -> Unit,
    onDelete: (FeedItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TechFeedColors.Surface900),
    ) {
        // ── Top bar ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector        = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint               = TechFeedColors.TextPrimary,
                )
            }
            Spacer(Modifier.width(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Star, contentDescription = null, tint = TechFeedColors.Accent,
                    modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("For You", style = TechFeedType.DisplayLarge)
            }
        }

        // ── "Based on" tag pills ──────────────────────────────────────────
        if (basedOnTags.isNotEmpty()) {
            Row(
                modifier              = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("Based on:", style = TechFeedType.Caption, modifier = Modifier.padding(top = 5.dp))
                basedOnTags.forEach { tag -> TagPill(label = tag) }
            }
        }

        HorizontalDivider(color = TechFeedColors.Surface600)

        when {
            isLoading -> {
                LazyColumn(
                    contentPadding      = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(6) { FeedCardSkeleton() }
                }
            }

            items.isEmpty() -> EmptyForYouState(modifier = Modifier.weight(1f))

            else -> LazyColumn(
                contentPadding      = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier            = Modifier.fillMaxSize(),
            ) {
                itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                    FeedCard(
                        item         = item,
                        isBookmarked = item.id in bookmarkIds,
                        onClick      = { onItemClick(item) },
                        onBookmark   = { onBookmark(item) },
                        onShare      = { onShare(item) },
                        onDelete     = { onDelete(item) },
                        index        = index,
                        modifier     = Modifier.animateItem().padding(horizontal = 16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyForYouState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector        = Icons.Rounded.Star,
                contentDescription = null,
                tint               = TechFeedColors.TextMuted,
                modifier           = Modifier.size(40.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text      = "Nothing personalized yet",
                style     = TechFeedType.Body,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text      = "Bookmark or read a few articles\nand we'll tailor this feed for you.",
                style     = TechFeedType.Caption,
                textAlign = TextAlign.Center,
            )
        }
    }
}
