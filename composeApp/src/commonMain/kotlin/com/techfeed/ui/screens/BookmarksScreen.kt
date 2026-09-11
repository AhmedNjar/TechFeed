package com.techfeed.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.techfeed.data.model.FeedItem
import com.techfeed.ui.components.FeedCard
import com.techfeed.ui.theme.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import techfeed.composeapp.generated.resources.Res
import techfeed.composeapp.generated.resources.rounded_bookmark_24

@Composable
fun BookmarksScreen(
    bookmarks: List<FeedItem>,
    bookmarkIds: Set<String>,
    onClose: () -> Unit,
    onItemClick: (FeedItem) -> Unit,
    onRemove: (FeedItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TechFeedColors.Surface900),
    ) {
        // Top bar
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
            Column {
                Text("Saved",              style = TechFeedType.DisplayLarge)
                Text("${bookmarks.size} articles", style = TechFeedType.Caption)
            }
        }

        HorizontalDivider(color = TechFeedColors.Surface600)

        if (bookmarks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter            = painterResource(Res.drawable.rounded_bookmark_24),
                        contentDescription = null,
                        tint               = TechFeedColors.TextMuted,
                        modifier           = Modifier.size(48.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("No saved articles yet", style = TechFeedType.Body, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Text("Tap the bookmark icon on any article.", style = TechFeedType.Caption, textAlign = TextAlign.Center)
                }
            }
            return
        }

        LazyColumn(
            contentPadding      = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier            = Modifier.fillMaxSize(),
        ) {
            itemsIndexed(bookmarks, key = { _, item -> item.id }) { index, item ->
                FeedCard(
                    item         = item,
                    isBookmarked = item.id in bookmarkIds,
                    onClick      = { onItemClick(item) },
                    onBookmark   = { onRemove(item) },
                    onShare      = { },
                    index        = index,
                    modifier     = Modifier.animateItem().padding(horizontal = 16.dp),
                )
            }
        }
    }
}

@Preview
@Composable
fun BookmarksScreenPreview() {
    val sampleItems = listOf(
        FeedItem(
            id = "1",
            title = "Getting Started with KMP",
            summary = "Learn the basics of Kotlin Multiplatform development.",
            url = "https://example.com/1",
            source = "Dev.to",
            tags = listOf("Kotlin", "KMP"),
            readTimeMinutes = 5
        ),
        FeedItem(
            id = "2",
            title = "Compose Multiplatform Best Practices",
            summary = "Tips and tricks for building shared UIs.",
            url = "https://example.com/2",
            source = "Medium",
            tags = listOf("Compose", "UI"),
            readTimeMinutes = 8
        )
    )

    TechFeedTheme {
        BookmarksScreen(
            bookmarks = sampleItems,
            bookmarkIds = setOf("1", "2"),
            onClose = {},
            onItemClick = {},
            onRemove = {}
        )
    }
}

@Preview
@Composable
fun BookmarksScreenEmptyPreview() {
    TechFeedTheme {
        BookmarksScreen(
            bookmarks = emptyList(),
            bookmarkIds = emptySet(),
            onClose = {},
            onItemClick = {},
            onRemove = {}
        )
    }
}
