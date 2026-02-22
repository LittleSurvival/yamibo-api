package io.github.littlesurvival.fetch

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*

internal actual fun createPlatformHttpClient(): HttpClient =
        HttpClient(CIO) { install(HttpTimeout) }

internal actual fun createPlatformHttpClientNoRedirect(): HttpClient =
        HttpClient(CIO) {
            install(HttpTimeout)
            followRedirects = false
        }
