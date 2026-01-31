package com.example.sitacardent.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

actual fun createHttpClient(): HttpClient = HttpClient(CIO) {
    configureCommon()
    engine {
        // CIO config if needed
    }
}
