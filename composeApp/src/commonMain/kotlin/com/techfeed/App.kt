package com.techfeed

import androidx.compose.runtime.Composable
import com.techfeed.presentation.FeedViewModel
import com.techfeed.ui.screens.HomeScreen
import com.techfeed.ui.theme.TechFeedTheme
import org.koin.compose.koinInject

// FIX: this was still the KMP wizard's placeholder screen (a "Click me!"
// button + Greeting toggle) — none of the app's real UI was reachable from
// it. This is now the real shared entry point: wraps HomeScreen in the app
// theme and injects the ViewModel via Koin. Android has its own equivalent
// wiring in MainActivity.kt (TechFeedRoot) and doesn't call this; iOS's
// MainViewController.kt calls this directly.
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
