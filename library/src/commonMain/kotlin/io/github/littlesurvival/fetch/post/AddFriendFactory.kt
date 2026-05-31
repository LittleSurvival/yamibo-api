package io.github.littlesurvival.fetch.post

import io.github.littlesurvival.YamiboRoute
import io.github.littlesurvival.core.FetchResult
import io.github.littlesurvival.dto.value.FormHash
import io.github.littlesurvival.dto.value.UserId
import io.github.littlesurvival.fetch.FetchFactory
import io.github.littlesurvival.fetch.PostFactory
import io.github.littlesurvival.fetch.post.util.PostResponseUtils
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.*
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.utils.io.charsets.Charsets

class AddFriendFactory(
    override val fetcher: FetchFactory
) : PostFactory(fetcher) {

    /**
     * Send an add-friend request.
     *
     * Performs a POST to Yamibo's add-friend endpoint with the current session [formHash]. The
     * [note] string is passed raw; [FormDataContent] handles URL encoding.
     *
     * @param formHash The formhash token from the add-friend popout.
     * @param userId Target user ID.
     * @param note Optional friend request note.
     * @param groupId Friend group ID submitted as `gid`.
     * @return [FetchResult.Success] containing the response message, or a [FetchResult.Failure] if
     * the request fails.
     */
    suspend fun addFriend(
        formHash: FormHash,
        userId: UserId,
        note: String = "",
        groupId: Int = 1
    ): FetchResult<String> {
        val url = YamiboRoute.UserSpace.AddFriend.AddFriendPost(userId).build()
        return try {
            val response =
                fetcher.perform(HttpMethod.Post, url) {
                    contentType(ContentType.Application.FormUrlEncoded.withCharset(Charsets.UTF_8))
                    setBody(
                        FormDataContent(
                            Parameters.build {
                                append("referer", YamiboRoute.Domain.build())
                                append("addsubmit", "true")
                                append("handlekey", "addfriendhk_${userId.value}")
                                append("formhash", formHash.value)
                                append("note", note)
                                append("gid", groupId.toString())
                            }
                        )
                    )
                }

            val body = response.bodyAsText()
            val message = PostResponseUtils.parseMessageText(body) ?: body

            if (response.status.isSuccess() && PostResponseUtils.isSuccess(body)) {
                FetchResult.Success(value = message, statusCode = response.status.value, url = url)
            } else {
                FetchResult.Failure.HttpError(
                    statusCode = response.status.value,
                    url = url,
                    bodyPreview = message
                )
            }
        } catch (e: HttpRequestTimeoutException) {
            FetchResult.Failure.Timeout(url, e)
        } catch (e: Exception) {
            FetchResult.Failure.NetworkError(url, e)
        }
    }
}
