package io.github.littlesurvival.fetch.post

import io.github.littlesurvival.YamiboRoute
import io.github.littlesurvival.core.FetchResult
import io.github.littlesurvival.dto.value.FormHash
import io.github.littlesurvival.dto.value.PrivateMessageId
import io.github.littlesurvival.dto.value.UserId
import io.github.littlesurvival.fetch.FetchFactory
import io.github.littlesurvival.fetch.PostFactory
import io.github.littlesurvival.fetch.post.util.PostResponseUtils
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.charsets.Charsets

class PrivateMessageFactory(
    override val fetcher: FetchFactory
) : PostFactory(fetcher) {

    /**
     * Send a private message in an existing conversation.
     *
     * Performs a POST to `home.php?mod=spacecp&ac=pm&op=send&pmid=...` with Yamibo's private
     * message form data. [FormDataContent] handles URL encoding for [message], so callers should
     * pass the raw message string.
     *
     * @param formHash The formhash token from the current session.
     * @param privateMessageId Private-message thread ID from `#pmform`.
     * @param toUser Target user ID.
     * @param message Raw private-message content.
     * @return [FetchResult.Success] containing the response message, or a [FetchResult.Failure] if
     * the request fails.
     */
    suspend fun sendPrivateMessage(
        formHash: FormHash,
        privateMessageId: PrivateMessageId,
        toUser: UserId,
        message: String
    ): FetchResult<String> {
        val url = YamiboRoute.SendPrivateMessage(privateMessageId).build()
        return try {
            val response =
                fetcher.perform(HttpMethod.Post, url) {
                    contentType(ContentType.Application.FormUrlEncoded.withCharset(Charsets.UTF_8))
                    setBody(
                        FormDataContent(
                            Parameters.build {
                                append("formhash", formHash.value)
                                append("touid", toUser.value.toString())
                                append("topmuid", toUser.value.toString())
                                append("message", message)
                            }
                        )
                    )
                }

            val body = response.bodyAsText()
            val parsedMessage = PostResponseUtils.parseMessageText(body) ?: parsePmMessage(body) ?: body

            if (response.status.isSuccess() && PostResponseUtils.isSuccess(body)) {
                FetchResult.Success(
                    value = parsedMessage,
                    statusCode = response.status.value,
                    url = url
                )
            } else {
                FetchResult.Failure.HttpError(
                    statusCode = response.status.value,
                    url = url,
                    bodyPreview = parsedMessage
                )
            }
        } catch (e: HttpRequestTimeoutException) {
            FetchResult.Failure.Timeout(url, e)
        } catch (e: Exception) {
            FetchResult.Failure.NetworkError(url, e)
        }
    }

    companion object {
        private val PM_SEND_MESSAGE_RE = Regex("""succeedhandle_pmsend\([^,]+,\s*'([^']*)'""")

        private fun parsePmMessage(body: String): String? {
            return PM_SEND_MESSAGE_RE.find(body)?.groupValues?.getOrNull(1)?.trim()?.ifEmpty { null }
        }
    }
}
