package com.techfeed.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyColumnItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.techfeed.data.model.FeedItem
import com.techfeed.data.model.FeedTag
import com.techfeed.presentation.*
import com.techfeed.ui.components.*
import com.techfeed.ui.theme.*
import com.techfeed.ui.util.WindowSizeInfo
import com.techfeed.ui.util.windowSizeInfoOf
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import techfeed.composeapp.generated.resources.Res
import techfeed.composeapp.generated.resources.rounded_bookmark_24

/**
 * ── Responsive strategy ─────────────────────────────────────────────────────
 *
 * Rather than branching the whole layout tree on device type, most of the
 * responsiveness here comes from two composable-level techniques that adapt
 * continuously to whatever width is actually available:
 *
 *  1. The feed itself is a [LazyVerticalGrid] with [GridCells.Adaptive] — it
 *     naturally renders as a single column on a narrow phone and multiple
 *     columns on a tablet/desktop-wide window, with no manual breakpoints.
 *
 *  2. [WindowSizeInfo], derived once from [BoxWithConstraints] here at the
 *     root, drives the few structural decisions that a grid alone can't
 *     express: showing the article side-by-side with the list instead of as
 *     a full-screen overlay once there's room for both, and presenting the
 *     Bookmarks/For You/Stats screens as a centered panel instead of an
 *     edge-to-edge sheet on the same wide layouts.
 */
@Composable
fun HomeScreen(
    viewModel: FeedViewModel,
    onShareRequest: (url: String, title: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val windowSize = remember(maxWidth) { windowSizeInfoOf(maxWidth) }
        val state by viewModel.state.collectAsState()
        val gridState    = rememberLazyGridState()
        val snackbarHost = remember { SnackbarHostState() }

        // ── Side effects ──────────────────────────────────────────────────
        LaunchedEffect(Unit) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    is FeedEffect.ShowSnackbar -> snackbarHost.showSnackbar(effect.message)
                    is FeedEffect.Share        -> onShareRequest(effect.url, effect.title)
                }
            }
        }

        // ── Pagination trigger ────────────────────────────────────────────
        val shouldLoadMore by remember {
            derivedStateOf {
                val last  = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                val total = gridState.layoutInfo.totalItemsCount
                total > 0 && last >= total - 4
            }
        }
        LaunchedEffect(shouldLoadMore) {
            if (shouldLoadMore) viewModel.onIntent(FeedIntent.LoadNextPage)
        }

        val showTwoPane = windowSize.isExpanded && state.selectedItem != null

        Row(Modifier.fillMaxSize()) {
            // ── List pane — full width normally; constrained alongside the
            // detail pane once there's room for both side by side.
            Box(
                modifier = if (showTwoPane) Modifier.weight(0.42f).widthIn(max = 440.dp)
                           else             Modifier.weight(1f),
            ) {
                FeedGridScaffold(
                    state         = state,
                    gridState     = gridState,
                    snackbarHost  = snackbarHost,
                    onIntent      = viewModel::onIntent,
                )
            }

            if (showTwoPane) {
                VerticalDivider(color = TechFeedColors.Surface600)
                Box(Modifier.weight(0.58f)) {
                    state.selectedItem?.let { item ->
                        ReadModeScreen(
                            item             = item,
                            isBookmarked     = item.id in state.bookmarkIds,
                            relatedArticles  = state.relatedArticles,
                            isRelatedLoading = state.isRelatedLoading,
                            bookmarkIds      = state.bookmarkIds,
                            windowSize       = windowSize,
                            onIntent         = viewModel::onIntent,
                        )
                    }
                }
            }
        }

        // ── Read Mode — full-screen overlay only when there isn't room to
        // show it as a side pane instead.
        AnimatedVisibility(
            visible  = state.selectedItem != null && !showTwoPane,
            enter    = slideInVertically { it } + fadeIn(),
            exit     = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            state.selectedItem?.let { item ->
                ReadModeScreen(
                    item             = item,
                    isBookmarked     = item.id in state.bookmarkIds,
                    relatedArticles  = state.relatedArticles,
                    isRelatedLoading = state.isRelatedLoading,
                    bookmarkIds      = state.bookmarkIds,
                    windowSize       = windowSize,
                    onIntent         = viewModel::onIntent,
                )
            }
        }

        // ── Bookmarks / For You / Stats — full-screen sheet on Compact/Medium,
        // centered dialog-style panel on Expanded.
        AdaptiveOverlay(
            visible      = state.showBookmarks,
            isWide       = windowSize.isExpanded,
            onScrimClick = { viewModel.onIntent(FeedIntent.HideBookmarks) },
        ) {
            BookmarksScreen(
                bookmarks   = state.bookmarks,
                bookmarkIds = state.bookmarkIds,
                onClose     = { viewModel.onIntent(FeedIntent.HideBookmarks) },
                onItemClick = { viewModel.onIntent(FeedIntent.SelectItem(it)) },
                onRemove    = { viewModel.onIntent(FeedIntent.ToggleBookmark(it)) },
            )
        }

        AdaptiveOverlay(
            visible      = state.showForYou,
            isWide       = windowSize.isExpanded,
            onScrimClick = { viewModel.onIntent(FeedIntent.HideForYou) },
        ) {
            ForYouScreen(
                items       = state.forYouItems,
                basedOnTags = state.forYouBasedOnTags,
                isLoading   = state.isForYouLoading,
                bookmarkIds = state.bookmarkIds,
                onClose     = { viewModel.onIntent(FeedIntent.HideForYou) },
                onItemClick = { viewModel.onIntent(FeedIntent.SelectItem(it)) },
                onBookmark  = { viewModel.onIntent(FeedIntent.ToggleBookmark(it)) },
                onShare     = { viewModel.onIntent(FeedIntent.ShareItem(it)) },
            )
        }

        AdaptiveOverlay(
            visible      = state.showStats,
            isWide       = windowSize.isExpanded,
            onScrimClick = { viewModel.onIntent(FeedIntent.HideStats) },
        ) {
            StatsScreen(
                stats   = state.readingStats,
                onClose = { viewModel.onIntent(FeedIntent.HideStats) },
            )
        }
    }
}

// ── Feed grid + scaffold (the "list pane") ────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedGridScaffold(
    state: FeedState,
    gridState: LazyGridState,
    snackbarHost: SnackbarHostState,
    onIntent: (FeedIntent) -> Unit,
) {
    val pullState = rememberPullToRefreshState()

    Scaffold(
        containerColor = TechFeedColors.Surface900,
        snackbarHost   = { SnackbarHost(hostState = snackbarHost) },
        floatingActionButton = {
            AnimatedVisibility(
                visible = state.bookmarkCount > 0,
                enter   = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                exit    = scaleOut() + fadeOut(),
            ) {
                ExtendedFloatingActionButton(
                    onClick        = { onIntent(FeedIntent.ShowBookmarks) },
                    containerColor = TechFeedColors.Surface700,
                    contentColor   = TechFeedColors.Accent,
                    icon           = { Icon(painter = painterResource(Res.drawable.rounded_bookmark_24), contentDescription = null) },
                    text           = { Text("${state.bookmarkCount} saved", style = TechFeedType.Caption) },
                )
            }
        },
    ) { padding ->

        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh    = { onIntent(FeedIntent.RefreshFeed) },
            state        = pullState,
            indicator    = {
                PullToRefreshDefaults.Indicator(
                    state          = pullState,
                    isRefreshing   = state.isRefreshing,
                    containerColor = TechFeedColors.Surface800,
                    color          = TechFeedColors.Accent,
                    modifier       = Modifier.align(Alignment.TopCenter),
                )
            },
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyVerticalGrid(
                columns        = GridCells.Adaptive(minSize = 340.dp),
                state          = gridState,
                contentPadding = PaddingValues(
                    start  = 16.dp, end = 16.dp,
                    top    = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 80.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement   = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                // Header
                item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
                    HomeHeader(
                        onRefresh = { onIntent(FeedIntent.RefreshFeed) },
                        onForYou  = { onIntent(FeedIntent.ShowForYou) },
                        onStats   = { onIntent(FeedIntent.ShowStats) },
                    )
                }

                // Search bar
                item(key = "search", span = { GridItemSpan(maxLineSpan) }) {
                    SearchBar(
                        query   = state.searchQuery,
                        onQuery = { onIntent(FeedIntent.UpdateSearch(it)) },
                        onClear = { onIntent(FeedIntent.ClearSearch) },
                    )
                }

                // Filter chips — collapses out of the way while searching
                // instead of just disappearing, so the transition reads as
                // "making room for search results" rather than a jump cut.
                item(key = "chips", span = { GridItemSpan(maxLineSpan) }) {
                    AnimatedVisibility(
                        visible = !state.isSearchActive,
                        enter   = fadeIn() + expandVertically(),
                        exit    = fadeOut() + shrinkVertically(),
                    ) {
                        FilterChipsRow(
                            selectedTag   = state.selectedTag,
                            onTagSelected = { onIntent(FeedIntent.SelectTag(it)) },
                            modifier      = Modifier.padding(vertical = 4.dp),
                        )
                    }
                }

                // Loading skeleton
                if (state.isLoading) {
                    items(6, key = { "skeleton_$it" }) { FeedCardSkeleton() }
                    return@LazyVerticalGrid
                }

                // Error banner
                state.error?.let { error ->
                    item(key = "error", span = { GridItemSpan(maxLineSpan) }) {
                        ErrorBanner(
                            message = error,
                            onRetry = { onIntent(FeedIntent.LoadFeed) },
                        )
                    }
                }

                // Feed items
                val displayItems = if (state.isSearchActive) state.searchResults else state.items
                itemsIndexed(displayItems, key = { _, item -> item.id }) { index, item ->
                    FeedCard(
                        item         = item,
                        isBookmarked = item.id in state.bookmarkIds,
                        onClick      = { onIntent(FeedIntent.SelectItem(item)) },
                        onBookmark   = { onIntent(FeedIntent.ToggleBookmark(item)) },
                        onShare      = { onIntent(FeedIntent.ShareItem(item)) },
                        index        = index,
                    )
                }

                // Empty state
                if (!state.isLoading && displayItems.isEmpty() && state.error == null) {
                    item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                        EmptyState(isSearch = state.isSearchActive)
                    }
                }

                // Pagination footer
                if (state.isPaginating) {
                    item(key = "paging", span = { GridItemSpan(maxLineSpan) }) {
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
    }
}

// ── Adaptive overlay wrapper ───────────────────────────────────────────────────
// Full-screen slide-up sheet on Compact/Medium; a centered, width-capped
// dialog-style panel with a scrim on Expanded, where an edge-to-edge sheet
// would otherwise stretch uncomfortably wide.

@Composable
private fun AdaptiveOverlay(
    visible: Boolean,
    isWide: Boolean,
    onScrimClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible  = visible,
        enter    = if (isWide) fadeIn(tween(200)) else slideInVertically { it } + fadeIn(),
        exit     = if (isWide) fadeOut(tween(150)) else slideOutVertically { it } + fadeOut(),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (isWide) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(
                        indication        = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick           = onScrimClick,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    modifier = Modifier
                        .padding(24.dp)
                        .widthIn(max = 560.dp)
                        .fillMaxHeight(0.88f)
                        // Absorbs taps on the panel's own background so they
                        // don't fall through to the scrim behind it.
                        .clickable(
                            indication        = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick           = {},
                        ),
                    shape          = RoundedCornerShape(20.dp),
                    color          = TechFeedColors.Surface900,
                    tonalElevation = 8.dp,
                ) {
                    content()
                }
            }
        } else {
            content()
        }
    }
}

// ── Sub-components ────────────────────────────────────────────────────────────

@Composable
private fun HomeHeader(
    onRefresh: () -> Unit,
    onForYou: () -> Unit,
    onStats: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.Bottom,
    ) {
        Column {
            Text("Tech",  style = TechFeedType.Caption.copy(color = TechFeedColors.Accent))
            Text("Feed",  style = TechFeedType.DisplayLarge)
        }
        Row {
            IconButton(onClick = onForYou) {
                Icon(Icons.Rounded.Star, "For You", tint = TechFeedColors.TextMuted)
            }
            IconButton(onClick = onStats) {
                Icon(Icons.AutoMirrored.Rounded.List, "Stats", tint = TechFeedColors.TextMuted)
            }
            IconButton(onClick = onRefresh) {
                Icon(Icons.Rounded.Refresh, "Refresh", tint = TechFeedColors.TextMuted)
            }
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
    Box(modifier = modifier.fillMaxWidth().padding(vertical = 64.dp), contentAlignment = Alignment.Center) {
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

// ── Preview ──────────────────────────────────────────────────────────────────

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
                    // FIX: was Icons.Filled.Share (a sharing icon on a bookmarks
                    // FAB made no sense — copy/paste leftover). Matches the real
                    // FAB's icon now.
                    icon           = { Icon(painter = painterResource(Res.drawable.rounded_bookmark_24), contentDescription = null) },
                    text           = { Text("${state.bookmarkCount} saved", style = TechFeedType.Caption) },
                )
            },
        ) { padding ->
            LazyColumn(
                contentPadding = PaddingValues(
                    top    = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 80.dp,
                ),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item {
                    HomeHeader(onRefresh = {}, onForYou = {}, onStats = {}, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                }
                item {
                    SearchBar(query = "", onQuery = {}, onClear = {}, modifier = Modifier.padding(horizontal = 16.dp))
                }
                item {
                    FilterChipsRow(selectedTag = FeedTag.ALL, onTagSelected = {}, modifier = Modifier.padding(vertical = 4.dp))
                }
                lazyColumnItems(state.items) { item ->
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
