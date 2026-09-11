package com.example.techfeed

import androidx.compose.ui.window.ComposeUIViewController
import com.techfeed.App
import com.techfeed.data.local.DriverFactory
import com.techfeed.di.appModule
import org.koin.core.context.startKoin
import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

// FIX: two separate bugs here previously.
//
// 1. This called the wizard placeholder `App()` (no real UI ever reachable
//    on iOS) instead of the real, now-fixed shared `App(onShareRequest)`.
//
// 2. Koin was never started anywhere on iOS. Android has its own startup in
//    TechFeedApp.onCreate() (see MainActivity.kt); iOS had no equivalent at
//    all, so the moment any screen called koinInject() it would crash with
//    "KoinApplication has not been started". Started once here, before the
//    Compose UI is built, mirroring Android's DriverFactory wiring (iOS's
//    DriverFactory needs no Context, unlike Android's).
private var koinStarted = false

fun MainViewController() = ComposeUIViewController {
    if (!koinStarted) {
        startKoin {
            modules(appModule(DriverFactory()))
        }
        koinStarted = true
    }

    App(onShareRequest = { url, title -> shareOnIOS(url, title) })
}

private fun shareOnIOS(url: String, title: String) {
    val nsUrl = NSURL.URLWithString(url)
    val items = listOfNotNull(title, nsUrl ?: url)
    val activityController = UIActivityViewController(
        activityItems        = items,
        applicationActivities = null,
    )
    UIApplication.sharedApplication.keyWindow
        ?.rootViewController
        ?.presentViewController(activityController, animated = true, completion = null)
}
