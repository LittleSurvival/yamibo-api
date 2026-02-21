package io.github.littlesurvival.fetch

import io.github.littlesurvival.Fetcher
import io.github.littlesurvival.core.FetchResult
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

internal expect fun createPlatformHttpClient(cookieStorage: CookiesStorage): HttpClient

internal expect fun createPlatformHttpClientNoRedirect(cookieStorage: CookiesStorage): HttpClient

class FetchFactory(
        var device: Device,
        var timeoutMillis: Long,
) : Fetcher<String> {
    companion object {
        enum class Device(val userAgent: String) {
            MOBILE(
                    "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36"
            ),
            // DESKTOP("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like
            // Gecko) Chrome/114.0.0.0 Safari/537.36")
        }
    }

    private val cookieStorage = AcceptAllCookiesStorage()
    private val formHash = ""

    private val client = createPlatformHttpClient(cookieStorage)
    private val noRedirectClient = createPlatformHttpClientNoRedirect(cookieStorage)

    /**
     * Parsers a cookie string (e.g. from document.cookie) and adds them to the storage.
     * @param url The URL context for these cookies (required to determine domain/path).
     * @param cookieString The string in format "key=value; key2=value2"
     */
    suspend fun setCookies(url: String, cookieString: String) {
        val effectiveUrl = if (url.startsWith("http")) url else "https://$url"
        val targetUrl = Url(effectiveUrl)
        cookieString.split(";").forEach { pair ->
            val parts = pair.split("=", limit = 2)
            if (parts.size == 2) {
                val name = parts[0].trim()
                val value = parts[1].trim()
                if (name.isNotEmpty()) {
                    cookieStorage.addCookie(
                            targetUrl,
                            Cookie(name = name, value = value, domain = targetUrl.host, path = "/")
                    )
                }
            }
        }
    }

    suspend fun perform(
            method: HttpMethod,
            url: String,
            block: HttpRequestBuilder.() -> Unit = {}
    ): HttpResponse {
        return client.request(url) {
            this.method = method
            headers[HttpHeaders.UserAgent] = device.userAgent
            timeout {
                requestTimeoutMillis = timeoutMillis
                connectTimeoutMillis = timeoutMillis
                socketTimeoutMillis = timeoutMillis
            }
            block()
        }
    }

    /**
     * Perform a request without following redirects.
     *
     * Useful for POST requests where the server responds with 302 and we need to capture the
     * Location header.
     */
    suspend fun performNoRedirect(
            method: HttpMethod,
            url: String,
            block: HttpRequestBuilder.() -> Unit = {}
    ): HttpResponse {
        return noRedirectClient.request(url) {
            this.method = method
            headers[HttpHeaders.UserAgent] = device.userAgent
            timeout {
                requestTimeoutMillis = timeoutMillis
                connectTimeoutMillis = timeoutMillis
                socketTimeoutMillis = timeoutMillis
            }
            block()
        }
    }

    suspend fun get(url: String, block: HttpRequestBuilder.() -> Unit = {}) =
            perform(HttpMethod.Get, url, block)
    suspend fun post(url: String, block: HttpRequestBuilder.() -> Unit = {}) =
            perform(HttpMethod.Post, url, block)
    suspend fun put(url: String, block: HttpRequestBuilder.() -> Unit = {}) =
            perform(HttpMethod.Put, url, block)
    suspend fun delete(url: String, block: HttpRequestBuilder.() -> Unit = {}) =
            perform(HttpMethod.Delete, url, block)
    suspend fun head(url: String, block: HttpRequestBuilder.() -> Unit = {}) =
            perform(HttpMethod.Head, url, block)

    /**
     * Fetcher return the HTML content of the page as FetchResult.
     *
     * @param url The url to fetch.
     * @return The HTML content of the page as FetchResult.
     *
     * FetchResult.Success: The HTML content of the page. FetchResult.Failure: The error occurred
     * during the fetch.
     */
    override suspend fun getResult(url: String): FetchResult<String> {
        return try {
            val response = get(url)
            val text = response.bodyAsText()

            if (response.status.isSuccess()) {
                FetchResult.Success(value = text, statusCode = response.status.value, url = url)
            } else {
                FetchResult.Failure.HttpError(
                    statusCode = response.status.value,
                    url = url,
                    bodyPreview = text
                )
            }
        } catch (e: HttpRequestTimeoutException) {
            FetchResult.Failure.Timeout(url, e)
        } catch (e: Exception) {
            FetchResult.Failure.NetworkError(url, e)
        }
    }
}
