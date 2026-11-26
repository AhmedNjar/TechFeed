package com.techfeed

import androidx.compose.ui.window.ComposeUIViewController
import com.techfeed.App
import com.techfeed.data.local.DriverFactory
import com.techfeed.di.appModule
import org.koin.core.context.startKoin
import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

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
