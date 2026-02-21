package io.github.littlesurvival.fetch.post

import io.github.littlesurvival.YamiboRoute
import io.github.littlesurvival.core.FetchResult
import io.github.littlesurvival.dto.value.FormHash
import io.github.littlesurvival.fetch.FetchFactory
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*

class SearchFactory(
    private val fetcher: FetchFactory,
) {
    /**
     * Submit a search query and return the redirect location URL.
     *
     * Performs a POST to `search.php?mod=forum` with the given [formHash] and [query]. The server
     * responds with 302 and a `Location` header pointing to the search results page.
     *
     * @param formHash The formhash token from the current session.
     * @param query The search text.
     * @return [FetchResult.Success] containing a [kotlin.String] with the redirect location, or a
     * [FetchResult.Failure] if the request fails.
     */
    suspend fun getCacheLink(formHash: FormHash, query: String): FetchResult<String> {
        val url = YamiboRoute.Search.SearchPhp.build()
        return try {
            val response =
                fetcher.performNoRedirect(HttpMethod.Post, url) {
                    header("content-type", "application/x-www-form-urlencoded; charset=UTF-8")
                    setBody(
                        FormDataContent(
                            Parameters.build {
                                append("formhash", formHash.value)
                                append("srhfid", "")
                                append("srchtxt", query)
                                append("searchsubmit", "yes")
                            }
                        )
                    )
                }

            if (response.status == HttpStatusCode.Found) {
                val location = response.headers[HttpHeaders.Location]
                if (location != null) {
                    FetchResult.Success(
                        value = YamiboRoute.Search.ByLocation(location).build(),
                        statusCode = response.status.value,
                        url = url
                    )
                } else {
                    FetchResult.Failure.HttpError(
                        statusCode = response.status.value,
                        url = url,
                        bodyPreview = response.bodyAsText()
                    )
                }
            } else {
                FetchResult.Failure.HttpError(
                    statusCode = response.status.value,
                    url = url,
                    bodyPreview = response.bodyAsText()
                )
            }
        } catch (e: HttpRequestTimeoutException) {
            FetchResult.Failure.Timeout(url, e)
        } catch (e: Exception) {
            FetchResult.Failure.NetworkError(url, e)
        }
    }
}
