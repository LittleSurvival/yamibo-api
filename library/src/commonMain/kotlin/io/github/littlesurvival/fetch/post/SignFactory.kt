package io.github.littlesurvival.fetch.post

import io.github.littlesurvival.YamiboRoute
import io.github.littlesurvival.core.FetchResult
import io.github.littlesurvival.fetch.FetchFactory
import io.github.littlesurvival.fetch.PostFactory
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.headers
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod

class SignFactory(
    override val fetcher: FetchFactory
) : PostFactory(fetcher) {

    /**
     * Fetch Yamibo's daily sign-in page.
     *
     * The caller should provide login and Cloudflare cookies through [FetchFactory.setCookies].
     */
    suspend fun fetchSignPage(): FetchResult<String> {
        return performSignGet(YamiboRoute.Sign.build())
    }

    /**
     * Execute the sign-in or repair-sign action URL parsed from [fetchSignPage].
     *
     * Yamibo exposes these actions as GET links on the plugin page.
     */
    suspend fun fetchSignAction(actionUrl: String): FetchResult<String> {
        return performSignGet(YamiboRoute.Domain.toFullLink(actionUrl))
    }

    private suspend fun performSignGet(url: String): FetchResult<String> {
        return try {
            val response = fetcher.perform(HttpMethod.Get, url) {
                headers {
                    set(HttpHeaders.UserAgent, SIGN_USER_AGENT)
                    set("Referer", YamiboRoute.Sign.build())
                    set(HttpHeaders.Accept, "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                    set("Upgrade-Insecure-Requests", "1")
                }
            }
            val body = response.bodyAsText()
            if (response.status.value in 200..299) {
                FetchResult.Success(value = body, statusCode = response.status.value, url = url)
            } else {
                FetchResult.Failure.HttpError(
                    statusCode = response.status.value,
                    url = url,
                    bodyPreview = body
                )
            }
        } catch (e: HttpRequestTimeoutException) {
            FetchResult.Failure.Timeout(url, e)
        } catch (e: Exception) {
            FetchResult.Failure.NetworkError(url, e)
        }
    }

    private companion object {
        const val SIGN_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    }
}
