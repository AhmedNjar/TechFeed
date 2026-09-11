package com.techfeed.data.local

import platform.Foundation.NSDate

// FIX: was TODO("Not yet implemented") - crashed on every single database
// write (feed caching, bookmarking, recording reads) since this is called
// on each insert.
actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()
