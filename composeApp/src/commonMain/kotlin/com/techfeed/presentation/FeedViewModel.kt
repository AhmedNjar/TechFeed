package com.techfeed.presentation

import com.techfeed.data.model.FeedItem
import com.techfeed.data.model.FeedTag
import com.techfeed.domain.BookmarkRepository
import com.techfeed.domain.FeedRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ── Intent ────────────────────────────────────────────────────────────────────

sealed interface FeedIntent {
    data object LoadFeed                          : FeedIntent
    data object RefreshFeed                       : FeedIntent
    data object LoadNextPage                      : FeedIntent
    data class  SelectTag(val tag: FeedTag)       : FeedIntent
    data class  SelectItem(val item: FeedItem)    : FeedIntent
    data object DismissDetail                     : FeedIntent
    data object DismissError                      : FeedIntent
    data class  UpdateSearch(val query: String)   : FeedIntent
    data object ClearSearch                       : FeedIntent
    data class  ToggleBookmark(val item: FeedItem): FeedIntent
    data object ShowBookmarks                     : FeedIntent
    data object HideBookmarks                     : FeedIntent
    data class  ShareItem(val item: FeedItem)     : FeedIntent
}

// ── State ─────────────────────────────────────────────────────────────────────

data class FeedState(
    val items: List<FeedItem>         = emptyList(),
    val isLoading: Boolean            = false,
    val isRefreshing: Boolean         = false,
    val isPaginating: Boolean         = false,
    val selectedTag: FeedTag          = FeedTag.ALL,
    val selectedItem: FeedItem?       = null,
    val currentPage: Int              = 0,
    val hasNextPage: Boolean          = true,
    val error: String?                = null,
    val searchQuery: String           = "",
    val searchResults: List<FeedItem> = emptyList(),
    val isSearchActive: Boolean       = false,
    val bookmarks: List<FeedItem>     = emptyList(),
    val bookmarkIds: Set<String>      = emptySet(),
    val bookmarkCount: Long           = 0,
    val showBookmarks: Boolean        = false,
)

// ── Effects ───────────────────────────────────────────────────────────────────

sealed interface FeedEffect {
    data class ShowSnackbar(val message: String)            : FeedEffect
    data class Share(val url: String, val title: String)    : FeedEffect
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

@OptIn(FlowPreview::class)
class FeedViewModel(
    private val feedRepo: FeedRepository,
    private val bookmarkRepo: BookmarkRepository,
) {
    private val job   = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.Main)

    private val _state   = MutableStateFlow(FeedState())
    val state: StateFlow<FeedState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<FeedEffect>(extraBufferCapacity = 16)
    val effects: SharedFlow<FeedEffect> = _effects.asSharedFlow()

    private val _searchQuery = MutableStateFlow("")

    init {
        onIntent(FeedIntent.LoadFeed)
        observeBookmarks()
        observeSearch()
    }

    fun onIntent(intent: FeedIntent) = when (intent) {
        FeedIntent.LoadFeed              -> loadFeed(reset = true)
        FeedIntent.RefreshFeed           -> loadFeed(reset = true, isRefresh = true)
        FeedIntent.LoadNextPage          -> loadNextPage()
        is FeedIntent.SelectTag          -> onTagSelected(intent.tag)
        is FeedIntent.SelectItem         -> _state.update { it.copy(selectedItem = intent.item) }
        FeedIntent.DismissDetail         -> _state.update { it.copy(selectedItem = null) }
        FeedIntent.DismissError          -> _state.update { it.copy(error = null) }
        is FeedIntent.UpdateSearch       -> onSearchChanged(intent.query)
        FeedIntent.ClearSearch           -> clearSearch()
        is FeedIntent.ToggleBookmark     -> toggleBookmark(intent.item)
        FeedIntent.ShowBookmarks         -> _state.update { it.copy(showBookmarks = true) }
        FeedIntent.HideBookmarks         -> _state.update { it.copy(showBookmarks = false) }
        is FeedIntent.ShareItem          -> scope.launch {
            _effects.emit(FeedEffect.Share(intent.item.url, intent.item.title))
        }
    }

    // ── Feed ──────────────────────────────────────────────────────────────────

    private fun onTagSelected(tag: FeedTag) {
        if (_state.value.selectedTag == tag) return
        _state.update { it.copy(selectedTag = tag) }
        loadFeed(reset = true)
    }

    private fun loadFeed(reset: Boolean, isRefresh: Boolean = false) {
        val s = _state.value
        if ((s.isLoading || s.isPaginating) && !isRefresh) return
        val page = if (reset) 0 else s.currentPage

        _state.update {
            it.copy(
                isLoading    = !isRefresh,
                isRefreshing = isRefresh,
                error        = null,
                currentPage  = page,
            )
        }

        scope.launch {
            feedRepo.getFeed(
                tag          = _state.value.selectedTag.apiValue,
                page         = page,
                forceRefresh = isRefresh,
            ).collect { result ->
                result
                    .onSuccess { response ->
                        _state.update { s2 ->
                            s2.copy(
                                items        = if (reset) response.items
                                               else s2.items + response.items,
                                isLoading    = false,
                                isRefreshing = false,
                                isPaginating = false,
                                currentPage  = response.page,
                                hasNextPage  = response.hasNext,
                            )
                        }
                    }
                    .onFailure { err ->
                        _state.update {
                            it.copy(
                                isLoading    = false,
                                isRefreshing = false,
                                isPaginating = false,
                                error        = err.message ?: "Unknown error",
                            )
                        }
                        _effects.emit(FeedEffect.ShowSnackbar("Failed to load feed"))
                    }
            }
        }
    }

    private fun loadNextPage() {
        val s = _state.value
        if (!s.hasNextPage || s.isPaginating || s.isLoading) return
        _state.update { it.copy(isPaginating = true, currentPage = it.currentPage + 1) }
        loadFeed(reset = false)
    }

    // ── Search ────────────────────────────────────────────────────────────────

    private fun onSearchChanged(query: String) {
        _state.update { it.copy(searchQuery = query, isSearchActive = query.isNotBlank()) }
        _searchQuery.value = query
    }

    private fun clearSearch() {
        _searchQuery.value = ""
        _state.update { it.copy(searchQuery = "", searchResults = emptyList(), isSearchActive = false) }
    }

    private fun observeSearch() {
        scope.launch {
            _searchQuery
                .debounce(300)
                .filter { it.length >= 2 }
                .distinctUntilChanged()
                .flatMapLatest { feedRepo.search(it) }
                .collect { results -> _state.update { it.copy(searchResults = results) } }
        }
    }

    // ── Bookmarks ─────────────────────────────────────────────────────────────

    private fun observeBookmarks() {
        scope.launch {
            bookmarkRepo.bookmarks.collect { list ->
                _state.update {
                    it.copy(
                        bookmarks     = list,
                        bookmarkIds   = list.map { b -> b.id }.toSet(),
                        bookmarkCount = list.size.toLong(),
                    )
                }
            }
        }
    }

    private fun toggleBookmark(item: FeedItem) {
        scope.launch {
            bookmarkRepo.toggle(item)
            val wasBookmarked = _state.value.bookmarkIds.contains(item.id)
            _effects.emit(FeedEffect.ShowSnackbar(
                if (wasBookmarked) "Removed from bookmarks" else "Saved ✓"
            ))
        }
    }

    fun clear() = job.cancel()
}
