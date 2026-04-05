package com.techfeed.data.local

import app.cash.sqldelight.db.SqlDriver
import com.techfeed.db.TechFeedDb

expect class DriverFactory {
    fun createDriver(): SqlDriver
}

fun createDatabase(driverFactory: DriverFactory): TechFeedDb {
    val driver = driverFactory.createDriver()
    return TechFeedDb(driver)
}
