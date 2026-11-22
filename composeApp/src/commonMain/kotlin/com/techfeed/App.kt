package com.techfeed

import androidx.compose.runtime.Composable
import com.techfeed.presentation.FeedViewModel
import com.techfeed.ui.screens.HomeScreen
import com.techfeed.ui.theme.TechFeedTheme
import org.koin.compose.koinInject

@Composable
fun App(onShareRequest: (url: String, title: String) -> Unit) {
    TechFeedTheme {
        val viewModel: FeedViewModel = koinInject()
        HomeScreen(
            viewModel      = viewModel,
            onShareRequest = onShareRequest,
        )
    }
}
