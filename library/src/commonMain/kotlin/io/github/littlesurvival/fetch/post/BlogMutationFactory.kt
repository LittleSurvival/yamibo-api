package io.github.littlesurvival.fetch.post

import io.github.littlesurvival.YamiboRoute
import io.github.littlesurvival.core.FetchResult
import io.github.littlesurvival.dto.model.BlogClassSelection
import io.github.littlesurvival.dto.model.BlogMutationResponse
import io.github.littlesurvival.dto.value.BlogId
import io.github.littlesurvival.dto.value.FormHash
import io.github.littlesurvival.fetch.FetchFactory
import io.github.littlesurvival.fetch.PostFactory
import io.github.littlesurvival.fetch.ReplayPolicy
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.http.isSuccess

class BlogMutationFactory(override val fetcher: FetchFactory) : PostFactory(fetcher) {
    suspend fun addBlog(
        title: String,
        message: String,
        classSelection: BlogClassSelection,
        formHash: FormHash,
    ): FetchResult<BlogMutationResponse> =
        submitBlog(null, title, message, classSelection, formHash)

    suspend fun updateBlog(
        blogId: BlogId,
        title: String,
        message: String,
        classSelection: BlogClassSelection,
        formHash: FormHash,
    ): FetchResult<BlogMutationResponse> =
        submitBlog(blogId, title, message, classSelection, formHash)

    suspend fun deleteBlog(
        blogId: BlogId,
        formHash: FormHash,
    ): FetchResult<BlogMutationResponse> {
        val url = YamiboRoute.BlogManage.Delete(blogId).build()
        return perform(url) {
            header(HttpHeaders.Origin, ORIGIN)
            header(HttpHeaders.Referrer, BASE_REFERER)
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(
                FormDataContent(
                    Parameters.build {
                        append("referer", BASE_REFERER)
                        append("deletesubmit", "true")
                        append("formhash", formHash.value)
                        append("btnsubmit", "true")
                    },
                ),
            )
        }
    }

    private suspend fun submitBlog(
        blogId: BlogId?,
        title: String,
        message: String,
        classSelection: BlogClassSelection,
        formHash: FormHash,
    ): FetchResult<BlogMutationResponse> {
        val url = YamiboRoute.BlogManage.Submit(blogId).build()
        return perform(url) {
            header(HttpHeaders.Origin, ORIGIN)
            header(HttpHeaders.Referrer, BLOG_FORM_REFERER)
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("subject", title)
                        append("savealbumid", "0")
                        append("newalbum", "请输入相册名称")
                        append("view_albumid", "none")
                        append("message", message)
                        append("classid", classSelection.toFormValue())
                        append("tag", "")
                        append("friend", PRIVATE_VISIBILITY)
                        append("password", "")
                        append("selectgroup", "")
                        append("target_names", "")
                        append("blogsubmit", "true")
                        append("formhash", formHash.value)
                    },
                ),
            )
        }
    }

    private suspend fun perform(
        url: String,
        block: io.ktor.client.request.HttpRequestBuilder.() -> Unit,
    ): FetchResult<BlogMutationResponse> = try {
        val response = fetcher.performBuffered(
            method = HttpMethod.Post,
            url = url,
            replayPolicy = ReplayPolicy.AFTER_CONFIRMED_EDGE_REJECTION,
            block = block,
        )
        if (response.status.isSuccess()) {
            FetchResult.Success(
                value = BlogMutationResponse(
                    body = response.body,
                    statusCode = response.status.value,
                    requestUrl = url,
                    finalUrl = response.finalUrl,
                    location = response.location,
                ),
                statusCode = response.status.value,
                url = url,
            )
        } else {
            response.toHttpError(url)
        }
    } catch (error: HttpRequestTimeoutException) {
        FetchResult.Failure.Timeout(url, error)
    } catch (error: Exception) {
        FetchResult.Failure.NetworkError(url, error)
    }

    private fun BlogClassSelection.toFormValue(): String = when (this) {
        is BlogClassSelection.Existing -> classId.value.toString()
        is BlogClassSelection.Create -> "new:$className"
    }

    private companion object {
        const val ORIGIN = "https://bbs.yamibo.com"
        const val BASE_REFERER = "$ORIGIN/"
        const val BLOG_FORM_REFERER = "$ORIGIN/home.php?mod=spacecp&ac=blog"
        const val PRIVATE_VISIBILITY = "3"
    }
}
