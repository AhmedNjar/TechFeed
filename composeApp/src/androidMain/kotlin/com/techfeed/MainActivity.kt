package com.techfeed

import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import com.techfeed.data.local.DriverFactory
import com.techfeed.di.appModule
import com.techfeed.ui.screens.HomeScreen
import com.techfeed.ui.theme.TechFeedTheme
import io.ktor.client.HttpClient
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.compose.koinInject
import org.koin.core.context.startKoin

class TechFeedApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // 1. Start Koin — pass DriverFactory with context
        startKoin {
            androidContext(this@TechFeedApp)
            modules(appModule(DriverFactory(this@TechFeedApp)))
        }

        // 2. Init Coil ONCE — reuse the same HttpClient from Koin
        SingletonImageLoader.setSafe {
            ImageLoader.Builder(this)
                .components {
                    add(KtorNetworkFetcherFactory(httpClient = get<HttpClient>()))
                }
                .crossfade(true)
                .build()
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TechFeedTheme {
                TechFeedRoot()
            }
        }
    }
}

@Composable
private fun TechFeedRoot() {
    val viewModel: com.techfeed.presentation.FeedViewModel = koinInject()
    val context = androidx.compose.ui.platform.LocalContext.current

    HomeScreen(
        viewModel       = viewModel,
        onShareRequest  = { url, title ->
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, url)
            }
            context.startActivity(Intent.createChooser(intent, "Share article"))
        },
    )
}
