package io.github.littlesurvival.fetch

import io.ktor.client.*
import io.ktor.client.engine.darwin.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cookies.*

internal actual fun createPlatformHttpClient(cookieStorage: CookiesStorage): HttpClient =
        HttpClient(Darwin) {
            install(HttpTimeout)
            install(HttpCookies) { storage = cookieStorage }
            engine { configureRequest { setAllowsCellularAccess(true) } }
        }

internal actual fun createPlatformHttpClientNoRedirect(cookieStorage: CookiesStorage): HttpClient =
        HttpClient(Darwin) {
            install(HttpTimeout)
            install(HttpCookies) { storage = cookieStorage }
            followRedirects = false
            engine { configureRequest { setAllowsCellularAccess(true) } }
        }
