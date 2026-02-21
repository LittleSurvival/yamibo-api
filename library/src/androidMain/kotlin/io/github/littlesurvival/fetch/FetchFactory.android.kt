package io.github.littlesurvival.fetch

import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cookies.*

internal actual fun createPlatformHttpClient(cookieStorage: CookiesStorage): HttpClient =
    HttpClient(Android) {
        install(HttpTimeout)
        install(HttpCookies) { storage = cookieStorage }
        engine {
            connectTimeout = 100_000
            socketTimeout = 100_000
        }
    }

internal actual fun createPlatformHttpClientNoRedirect(cookieStorage: CookiesStorage): HttpClient =
    HttpClient(Android) {
        install(HttpTimeout)
        install(HttpCookies) { storage = cookieStorage }
        followRedirects = false
        engine {
            connectTimeout = 100_000
            socketTimeout = 100_000
        }
    }
