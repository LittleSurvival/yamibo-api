package io.github.littlesurvival.fetch

import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*

internal actual fun createPlatformHttpClient(): HttpClient =
        HttpClient(Android) {
            install(HttpTimeout)
            engine {
                connectTimeout = 100_000
                socketTimeout = 100_000
            }
        }

internal actual fun createPlatformHttpClientNoRedirect(): HttpClient =
        HttpClient(Android) {
            install(HttpTimeout)
            followRedirects = false
            engine {
                connectTimeout = 100_000
                socketTimeout = 100_000
            }
        }
