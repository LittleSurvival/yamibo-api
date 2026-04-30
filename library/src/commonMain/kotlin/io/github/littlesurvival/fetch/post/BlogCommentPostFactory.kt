package io.github.littlesurvival.fetch.post

import io.github.littlesurvival.YamiboRoute
import io.github.littlesurvival.core.FetchResult
import io.github.littlesurvival.dto.value.BlogId
import io.github.littlesurvival.dto.value.FormHash
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

class BlogCommentPostFactory(
    override val fetcher: FetchFactory
) : PostFactory(fetcher) {

    /**
     * Comment on a blog.
     *
     * Performs a POST to `home.php?mod=spacecp&ac=comment` with Yamibo's quick comment form data.
     * The server responds with Discuz XML/CDATA, and the returned message is extracted from
     * `#messagetext p` when possible.
     *
     * @param formHash The formhash token from the current session.
     * @param blogId The blog ID to comment on.
     * @param userId The blog author's user ID, used to build the referer URL.
     * @param message The comment content.
     * @return [FetchResult.Success] containing the response message, or a [FetchResult.Failure] if
     * the request fails.
     */
    suspend fun commentBlog(
        formHash: FormHash,
        blogId: BlogId,
        userId: UserId,
        message: String
    ): FetchResult<String> {
        val url = YamiboRoute.BlogComment.build()
        return try {
            val response =
                fetcher.perform(HttpMethod.Post, url) {
                    contentType(ContentType.Application.FormUrlEncoded.withCharset(Charsets.UTF_8))
                    setBody(
                        FormDataContent(
                            Parameters.build {
                                append("message", message)
                                append("formhash", formHash.value)
                                append("referer", "home.php?mod=space&uid=${userId.value}&do=blog&id=${blogId.value}")
                                append("id", blogId.value.toString())
                                append("idtype", "blogid")
                                append("handlekey", "qcblog_${blogId.value}")
                                append("commentsubmit", "true")
                                append("quickcomment", "true")
                                append("commentsubmit_btn", "true")
                            }
                        )
                    )
                }

            val body = response.bodyAsText()
            val parsedMessage = PostResponseUtils.parseMessageText(body) ?: body

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
}
