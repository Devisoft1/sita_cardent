package com.example.sitacardent

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
expect fun isNetworkAvailable(): Boolean