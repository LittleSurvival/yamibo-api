package io.github.littlesurvival.fetch

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cookies.*

internal actual fun createPlatformHttpClient(cookieStorage: CookiesStorage): HttpClient =
        HttpClient(CIO) {
            install(HttpTimeout)
            install(HttpCookies) { storage = cookieStorage }
        }

internal actual fun createPlatformHttpClientNoRedirect(cookieStorage: CookiesStorage): HttpClient =
        HttpClient(CIO) {
            install(HttpTimeout)
            install(HttpCookies) { storage = cookieStorage }
            followRedirects = false
        }
