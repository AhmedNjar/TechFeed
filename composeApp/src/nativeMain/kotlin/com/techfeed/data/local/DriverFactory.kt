package com.techfeed.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.techfeed.db.TechFeedDb

// FIX: this actual did not exist anywhere in the project. `expect class
// DriverFactory` in commonMain had an Android actual only, which meant the
// iOS targets could not compile at all ("expected class has no actual
// declaration"). Placed in nativeMain so one implementation covers iosX64,
// iosArm64, and iosSimulatorArm64 uniformly.
actual class DriverFactory {
    actual fun createDriver(): SqlDriver =
        NativeSqliteDriver(TechFeedDb.Schema, "techfeed.db")
}
