package io.github.littlesurvival.fetch

import io.github.littlesurvival.Fetcher
import io.github.littlesurvival.core.FetchResult
import io.github.littlesurvival.fetch.network.WafChallengeDetector
import io.github.littlesurvival.waf.ClientCookieStore
import io.github.littlesurvival.waf.WafChallengeCoordinator
import io.github.littlesurvival.waf.WafRecoveryConfig
import io.github.littlesurvival.waf.WafRecoveryDisposition
import io.github.littlesurvival.waf.WafResolution
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.timeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess

internal expect fun createPlatformHttpClient(): HttpClient

internal expect fun createPlatformHttpClientNoRedirect(): HttpClient

internal enum class ReplayPolicy {
    SAFE_ONCE,
    AFTER_CONFIRMED_EDGE_REJECTION,
    NEVER,
}

internal data class BufferedHttpResponse(
    val status: HttpStatusCode,
    val body: String,
    val finalUrl: String,
    val diagnosticHeaders: Map<String, List<String>>,
    val location: String?,
    val recoveryDisposition: WafRecoveryDisposition? = null,
) {
    fun bodyAsText(): String = body

    fun toHttpError(url: String): FetchResult.Failure.HttpError =
        FetchResult.Failure.HttpError(
            statusCode = status.value,
            url = url,
            bodyPreview = body,
            responseHeaders = diagnosticHeaders,
            wafRecoveryDisposition = recoveryDisposition,
        )
}

class FetchFactory internal constructor(
    var device: Device,
    var timeoutMillis: Long,
    private val cookieStore: ClientCookieStore,
    private val recoveryCoordinator: WafChallengeCoordinator?,
    private val recoveryConfig: WafRecoveryConfig,
    private val client: HttpClient = createPlatformHttpClient(),
    private val noRedirectClient: HttpClient = createPlatformHttpClientNoRedirect(),
) : Fetcher<String> {
    constructor(device: Device, timeoutMillis: Long) : this(
        device = device,
        timeoutMillis = timeoutMillis,
        cookieStore = ClientCookieStore(),
        recoveryCoordinator = null,
        recoveryConfig = WafRecoveryConfig(enabled = false),
    )

    enum class Device(val userAgent: String) {
        MOBILE("Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36"),
        DESKTOP("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36"),
    }

    override fun setCookies(cookie: String) {
        cookieStore.setAuthenticationCookies(cookie)
    }

    override fun clearCookies(clearNox: Boolean) {
        cookieStore.clearAuthenticationCookies()
        if (clearNox) cookieStore.clearNoxCookie()
    }

    internal fun close() {
        client.close()
        noRedirectClient.close()
    }

    suspend fun perform(
        method: HttpMethod,
        url: String,
        noRedirect: Boolean = false,
        userAgent: String = device.userAgent,
        block: HttpRequestBuilder.() -> Unit = {},
    ): HttpResponse {
        val useClient = if (noRedirect) noRedirectClient else client
        return useClient.request(url) {
            this.method = method
            headers[HttpHeaders.UserAgent] = userAgent
            cookieStore.currentHeader()?.let { headers[HttpHeaders.Cookie] = it }
            timeout {
                requestTimeoutMillis = timeoutMillis
                connectTimeoutMillis = timeoutMillis
                socketTimeoutMillis = timeoutMillis
            }
            block()
        }
    }

    /**
     * Buffers one response and applies the request's explicit replay policy. The request builder is
     * invoked again only after precise edge-challenge evidence and verified browser clearance.
     */
    internal suspend fun performBuffered(
        method: HttpMethod,
        url: String,
        replayPolicy: ReplayPolicy,
        noRedirect: Boolean = false,
        userAgent: String = device.userAgent,
        block: HttpRequestBuilder.() -> Unit = {},
    ): BufferedHttpResponse {
        val first = performBufferedOnce(method, url, noRedirect, userAgent, block)
        val provider = WafChallengeDetector.detect(first.toHttpError(url)) ?: return first
        val coordinator = recoveryCoordinator ?: return first

        val resolution = coordinator.resolve(
            provider = provider,
            statusCode = first.status.value,
            url = url,
            userAgent = userAgent,
            cookieHeader = cookieStore.currentHeader().orEmpty(),
        ) {
            performBufferedOnce(
                method = HttpMethod.Get,
                url = recoveryConfig.safeProbeUrl,
                noRedirect = false,
                userAgent = userAgent,
            ).status.isSuccess()
        }

        return when (resolution) {
            WafResolution.Verified -> when (replayPolicy) {
                ReplayPolicy.NEVER -> first.copy(
                    recoveryDisposition = WafRecoveryDisposition.REPLAY_NOT_ALLOWED,
                )

                ReplayPolicy.SAFE_ONCE,
                ReplayPolicy.AFTER_CONFIRMED_EDGE_REJECTION,
                -> {
                    val replay = performBufferedOnce(method, url, noRedirect, userAgent, block)
                    if (WafChallengeDetector.detect(replay.toHttpError(url)) != null) {
                        cookieStore.clearNoxCookie()
                        replay.copy(
                            recoveryDisposition = WafRecoveryDisposition.VERIFICATION_FAILED,
                        )
                    } else {
                        replay
                    }
                }
            }

            is WafResolution.Unavailable -> first.copy(
                recoveryDisposition = resolution.disposition,
            )
        }
    }

    private suspend fun performBufferedOnce(
        method: HttpMethod,
        url: String,
        noRedirect: Boolean,
        userAgent: String,
        block: HttpRequestBuilder.() -> Unit = {},
    ): BufferedHttpResponse {
        val response = perform(method, url, noRedirect, userAgent, block)
        return BufferedHttpResponse(
            status = response.status,
            body = response.bodyAsText(),
            finalUrl = response.call.request.url.toString(),
            diagnosticHeaders = response.wafDiagnosticHeaders(),
            location = response.headers[HttpHeaders.Location],
        )
    }

    override suspend fun getResult(url: String): FetchResult<String> = try {
        val response = performBuffered(
            method = HttpMethod.Get,
            url = url,
            replayPolicy = ReplayPolicy.SAFE_ONCE,
        )
        if (response.status.isSuccess()) {
            FetchResult.Success(response.body, response.status.value, url)
        } else {
            response.toHttpError(url)
        }
    } catch (e: HttpRequestTimeoutException) {
        FetchResult.Failure.Timeout(url, e)
    } catch (e: Exception) {
        FetchResult.Failure.NetworkError(url, e)
    }

    private fun HttpResponse.wafDiagnosticHeaders(): Map<String, List<String>> = buildMap {
        headers.getAll(HttpHeaders.Server)?.let { put(HttpHeaders.Server, it) }
        headers.getAll("BDWAF-Request-ID")?.let { put("BDWAF-Request-ID", it) }
    }
}
