package com.example.sitacardent.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

actual fun createHttpClient(): HttpClient = HttpClient(Darwin) {
    configureCommon()
    engine {
        configureRequest {
            setAllowsCellularAccess(true)
        }
    }
}
