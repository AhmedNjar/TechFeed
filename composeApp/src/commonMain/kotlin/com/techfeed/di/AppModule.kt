package com.techfeed.di

import com.techfeed.data.local.DriverFactory
import com.techfeed.data.local.FeedLocalDataSource
import com.techfeed.data.local.createDatabase
import com.techfeed.data.remote.FeedApiClient
import com.techfeed.data.remote.buildHttpClient
import com.techfeed.data.remote.createHttpEngine
import com.techfeed.domain.BookmarkRepository
import com.techfeed.domain.FeedRepository
import com.techfeed.domain.ForYouRepository
import com.techfeed.domain.RelatedArticlesRepository
import com.techfeed.domain.StatsRepository
import com.techfeed.presentation.FeedViewModel
import org.koin.dsl.module

fun appModule(driverFactory: DriverFactory) = module {

    // ── Database (SQLDelight) ─────────────────────────────────────────────────
    single { createDatabase(driverFactory) }
    single { FeedLocalDataSource(get()) }

    // ── Networking ────────────────────────────────────────────────────────────
    single { buildHttpClient(createHttpEngine()) }
    single { FeedApiClient(get()) }

    // ── Repositories ──────────────────────────────────────────────────────────
    single { FeedRepository(get(), get()) }
    single { BookmarkRepository(get()) }
    single { ForYouRepository(get(), get()) }
    single { StatsRepository(get()) }
    single { RelatedArticlesRepository(get()) }

    // ── ViewModel ─────────────────────────────────────────────────────────────
    factory { FeedViewModel(get(), get(), get(), get(), get()) }
}
