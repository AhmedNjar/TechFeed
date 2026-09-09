package com.techfeed.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.techfeed.data.model.FeedItem
import com.techfeed.data.model.FeedTag
import com.techfeed.presentation.*
import com.techfeed.ui.components.*
import com.techfeed.ui.theme.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import techfeed.composeapp.generated.resources.Res
import techfeed.composeapp.generated.resources.rounded_bookmark_24

@Composable
fun HomeScreen(
    viewModel: FeedViewModel,
    onShareRequest: (url: String, title: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state       by viewModel.state.collectAsState()
    val listState      = rememberLazyListState()
    val snackbarHost   = remember { SnackbarHostState() }

    // ── Side effects ──────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is FeedEffect.ShowSnackbar -> snackbarHost.showSnackbar(effect.message)
                is FeedEffect.Share        -> onShareRequest(effect.url, effect.title)
            }
        }
    }

    // ── Pagination trigger ────────────────────────────────────────────────
    val shouldLoadMore by remember {
        derivedStateOf {
            val last  = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && last >= total - 4
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.onIntent(FeedIntent.LoadNextPage)
    }

    Box(modifier = modifier.fillMaxSize()) {

        Scaffold(
            containerColor = TechFeedColors.Surface900,
            snackbarHost   = { SnackbarHost(hostState = snackbarHost) },
            floatingActionButton = {
                if (state.bookmarkCount > 0) {
                    ExtendedFloatingActionButton(
                        onClick        = { viewModel.onIntent(FeedIntent.ShowBookmarks) },
                        containerColor = TechFeedColors.Surface700,
                        contentColor   = TechFeedColors.Accent,
                        icon           = { Icon(painter= painterResource(Res.drawable.rounded_bookmark_24), null) },
                        text           = { Text("${state.bookmarkCount} saved", style = TechFeedType.Caption) },
                    )
                }
            },
        ) { padding ->

            LazyColumn(
                state          = listState,
                contentPadding = PaddingValues(
                    top    = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 80.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {

                // Header
                item(key = "header") {
                    HomeHeader(
                        onRefresh = { viewModel.onIntent(FeedIntent.RefreshFeed) },
                        modifier  = Modifier.animateItem(),
                    )
                }

                // Search bar
                item(key = "search") {
                    SearchBar(
                        query    = state.searchQuery,
                        onQuery  = { viewModel.onIntent(FeedIntent.UpdateSearch(it)) },
                        onClear  = { viewModel.onIntent(FeedIntent.ClearSearch) },
                        modifier = Modifier.animateItem().padding(horizontal = 16.dp),
                    )
                }

                // Filter chips (hidden during search)
                if (!state.isSearchActive) {
                    item(key = "chips") {
                        FilterChipsRow(
                            selectedTag   = state.selectedTag,
                            onTagSelected = { viewModel.onIntent(FeedIntent.SelectTag(it)) },
                            modifier      = Modifier.animateItem().padding(vertical = 4.dp),
                        )
                    }
                }

                // Loading skeleton
                if (state.isLoading) {
                    items(6, key = { "skeleton_$it" }) {
                        FeedCardSkeleton(
                            modifier = Modifier.animateItem().padding(horizontal = 16.dp),
                        )
                    }
                    return@LazyColumn
                }

                // Error banner
                state.error?.let { error ->
                    item(key = "error") {
                        ErrorBanner(
                            message = error,
                            onRetry = { viewModel.onIntent(FeedIntent.LoadFeed) },
                            modifier = Modifier.animateItem().padding(horizontal = 16.dp),
                        )
                    }
                }

                // Feed items
                val displayItems = if (state.isSearchActive) state.searchResults else state.items
                itemsIndexed(displayItems, key = { _, item -> item.id }) { index, item ->
                    FeedCard(
                        item         = item,
                        isBookmarked = item.id in state.bookmarkIds,
                        onClick      = { viewModel.onIntent(FeedIntent.SelectItem(item)) },
                        onBookmark   = { viewModel.onIntent(FeedIntent.ToggleBookmark(item)) },
                        onShare      = { viewModel.onIntent(FeedIntent.ShareItem(item)) },
                        index        = index,
                        modifier     = Modifier.animateItem().padding(horizontal = 16.dp),
                    )
                }

                // Empty state
                if (!state.isLoading && displayItems.isEmpty() && state.error == null) {
                    item(key = "empty") {
                        EmptyState(
                            isSearch = state.isSearchActive,
                            modifier = Modifier.animateItem().fillParentMaxWidth(),
                        )
                    }
                }

                // Pagination footer
                if (state.isPaginating) {
                    item(key = "paging") {
                        Box(
                            modifier         = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                color       = TechFeedColors.Accent,
                                modifier    = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                            )
                        }
                    }
                }
            }
        }

        // ── Read Mode overlay ─────────────────────────────────────────────
        AnimatedVisibility(
            visible  = state.selectedItem != null,
            enter    = slideInVertically { it } + fadeIn(),
            exit     = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            state.selectedItem?.let { item ->
                ReadModeScreen(
                    item         = item,
                    isBookmarked = item.id in state.bookmarkIds,
                    onIntent     = viewModel::onIntent,
                )
            }
        }

        // ── Bookmarks overlay ─────────────────────────────────────────────
        AnimatedVisibility(
            visible  = state.showBookmarks,
            enter    = slideInVertically { it } + fadeIn(),
            exit     = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            BookmarksScreen(
                bookmarks   = state.bookmarks,
                bookmarkIds = state.bookmarkIds,
                onClose     = { viewModel.onIntent(FeedIntent.HideBookmarks) },
                onItemClick = { viewModel.onIntent(FeedIntent.SelectItem(it)) },
                onRemove    = { viewModel.onIntent(FeedIntent.ToggleBookmark(it)) },
            )
        }
    }
}

// ── Sub-components ────────────────────────────────────────────────────────────

@Composable
private fun HomeHeader(onRefresh: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.Bottom,
    ) {
        Column {
            Text("Tech",  style = TechFeedType.Caption.copy(color = TechFeedColors.Accent))
            Text("Feed",  style = TechFeedType.DisplayLarge)
        }
        IconButton(onClick = onRefresh) {
            Icon(Icons.Rounded.Refresh, "Refresh", tint = TechFeedColors.TextMuted)
        }
    }
}

@Composable
fun SearchBar(
    query: String,
    onQuery: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value         = query,
        onValueChange = onQuery,
        placeholder   = { Text("Search articles...", style = TechFeedType.Body) },
        leadingIcon   = { Icon(Icons.Rounded.Search, null, tint = TechFeedColors.TextMuted) },
        trailingIcon  = {
            if (query.isNotBlank()) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Rounded.Close, "Clear", tint = TechFeedColors.TextMuted)
                }
            }
        },
        singleLine = true,
        shape      = RoundedCornerShape(12.dp),
        colors     = OutlinedTextFieldDefaults.colors(
            focusedBorderColor      = TechFeedColors.Accent,
            unfocusedBorderColor    = TechFeedColors.Surface600,
            focusedContainerColor   = TechFeedColors.Surface800,
            unfocusedContainerColor = TechFeedColors.Surface800,
            cursorColor             = TechFeedColors.Accent,
            focusedTextColor        = TechFeedColors.TextPrimary,
            unfocusedTextColor      = TechFeedColors.TextPrimary,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun ErrorBanner(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier       = modifier.fillMaxWidth(),
        shape          = RoundedCornerShape(12.dp),
        color          = TechFeedColors.Error.copy(alpha = 0.12f),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier              = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text(message, style = TechFeedType.Body.copy(color = TechFeedColors.Error), modifier = Modifier.weight(1f))
            TextButton(onClick = onRetry) { Text("Retry", color = TechFeedColors.Accent) }
        }
    }
}

@Composable
private fun EmptyState(isSearch: Boolean, modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(vertical = 64.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text      = if (isSearch) "//  no results" else "//  no articles",
                style     = TechFeedType.Mono.copy(color = TechFeedColors.TextMuted),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text      = if (isSearch) "Try different keywords." else "Try a different tag or refresh.",
                style     = TechFeedType.Body,
                textAlign = TextAlign.Center,
            )
        }
    }
}


@Preview
@Composable
fun HomeScreenPreview() {
    val state = remember {
        FeedState(
            items = listOf(
                FeedItem(
                    id = "1",
                    title = "Modern Android Development in 2024",
                    summary = "Explore the latest trends in Jetpack Compose and KMP.",
                    url = "https://example.com/1",
                    source = "Dev.to",
                    tags = listOf("Android", "Kotlin"),
                    readTimeMinutes = 5
                )
            ),
            selectedTag = FeedTag.ALL,
            isLoading = false,
            bookmarkCount = 1
        )
    }

    TechFeedTheme {
        Scaffold(
            containerColor = TechFeedColors.Surface900,
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick        = { },
                    containerColor = TechFeedColors.Surface700,
                    contentColor   = TechFeedColors.Accent,
                    icon           = { Icon(Icons.Filled.Share, contentDescription = null) },                    text           = { Text("${state.bookmarkCount} saved", style = TechFeedType.Caption) },
                )
            },
        ) { padding ->
            LazyColumn(
                contentPadding = PaddingValues(
                    top    = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 80.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item {
                    HomeHeader(onRefresh = {}, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                }
                item {
                    SearchBar(query = "", onQuery = {}, onClear = {}, modifier = Modifier.padding(horizontal = 16.dp))
                }
                item {
                    FilterChipsRow(selectedTag = FeedTag.ALL, onTagSelected = {}, modifier = Modifier.padding(vertical = 4.dp))
                }
                items(state.items) { item ->
                    FeedCard(
                        item = item,
                        isBookmarked = false,
                        onClick = {},
                        onBookmark = {},
                        onShare = {},
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}
