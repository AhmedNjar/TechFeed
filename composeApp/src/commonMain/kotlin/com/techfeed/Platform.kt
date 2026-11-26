package com.techfeed

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform