package io.github.littlesurvival.fetch.post

import io.github.littlesurvival.core.FetchResult
import io.github.littlesurvival.dto.model.BlogClassSelection
import io.github.littlesurvival.dto.model.BlogMutationResponse
import io.github.littlesurvival.dto.value.BlogClassId
import io.github.littlesurvival.dto.value.BlogId
import io.github.littlesurvival.dto.value.FormHash
import io.github.littlesurvival.fetch.FetchFactory
import io.github.littlesurvival.waf.ClientCookieStore
import io.github.littlesurvival.waf.WafChallengeCoordinator
import io.github.littlesurvival.waf.WafHostState
import io.github.littlesurvival.waf.WafRecoveryConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class BlogMutationFactoryTest {
    @Test
    fun addBlogMatchesDiscuzMultipartContract() = runBlocking {
        var requestUrl = ""
        var requestBody = ""
        var requestMethod: HttpMethod? = null
        val fixture = fixture { request ->
            requestUrl = request.url.toString()
            requestBody = request.body.toByteArray().decodeToString()
            requestMethod = request.method
            response("created", location = "/home.php?mod=space&do=blog&id=77")
        }

        val result = assertIs<FetchResult.Success<BlogMutationResponse>>(
            fixture.factory.addBlog(
                title = "sync-title",
                message = "sync-body",
                classSelection = BlogClassSelection.Create("Yamibo App Sync"),
                formHash = FORM_HASH,
            ),
        )

        assertEquals(HttpMethod.Post, requestMethod)
        assertTrue(requestUrl.contains("mod=spacecp"))
        assertTrue(requestUrl.contains("ac=blog"))
        assertTrue(requestUrl.contains("blogid="))
        assertMultipartField(requestBody, "subject", "sync-title")
        assertMultipartField(requestBody, "message", "sync-body")
        assertMultipartField(requestBody, "classid", "new:Yamibo App Sync")
        assertMultipartField(requestBody, "friend", "3")
        assertMultipartField(requestBody, "blogsubmit", "true")
        assertMultipartField(requestBody, "formhash", FORM_HASH.value)
        assertEquals("/home.php?mod=space&do=blog&id=77", result.value.location)
        fixture.close()
    }

    @Test
    fun updateBlogUsesExistingClassAndBlogId() = runBlocking {
        var requestUrl = ""
        var requestBody = ""
        val fixture = fixture { request ->
            requestUrl = request.url.toString()
            requestBody = request.body.toByteArray().decodeToString()
            response("updated")
        }

        fixture.factory.updateBlog(
            blogId = BlogId(77),
            title = "sync-title",
            message = "sync-body",
            classSelection = BlogClassSelection.Existing(BlogClassId(4568)),
            formHash = FORM_HASH,
        )

        assertTrue(requestUrl.contains("blogid=77"))
        assertMultipartField(requestBody, "classid", "4568")
        fixture.close()
    }

    @Test
    fun deleteBlogMatchesDiscuzFormContract() = runBlocking {
        var requestUrl = ""
        var requestBody = ""
        var referer: String? = null
        val fixture = fixture { request ->
            requestUrl = request.url.toString()
            requestBody = request.body.toByteArray().decodeToString()
            referer = request.headers[HttpHeaders.Referrer]
            response("deleted")
        }

        fixture.factory.deleteBlog(BlogId(79), FORM_HASH)

        assertTrue(requestUrl.contains("op=delete"))
        assertTrue(requestUrl.contains("blogid=79"))
        assertEquals("https://bbs.yamibo.com/", referer)
        assertTrue(requestBody.contains("deletesubmit=true"))
        assertTrue(requestBody.contains("btnsubmit=true"))
        assertTrue(requestBody.contains("formhash=${FORM_HASH.value}"))
        fixture.close()
    }

    @Test
    fun preciseWafChallengeRecoversAndReplaysBlogWriteOnce() = runBlocking {
        val bodies = mutableListOf<String>()
        var call = 0
        val fixture = fixture { request ->
            if (request.method == HttpMethod.Post) {
                bodies += request.body.toByteArray().decodeToString()
            }
            when (call++) {
                0 -> wafResponse()
                1 -> response("probe-ok")
                else -> response("created")
            }
        }

        val pending = async {
            fixture.factory.addBlog(
                title = "sync-title",
                message = "sync-body",
                classSelection = BlogClassSelection.Existing(BlogClassId(4568)),
                formHash = FORM_HASH,
            )
        }
        fixture.submitSyntheticCookie()

        assertIs<FetchResult.Success<BlogMutationResponse>>(pending.await())
        assertEquals(3, call)
        assertEquals(2, bodies.size)
        bodies.forEach { assertMultipartField(it, "message", "sync-body") }
        fixture.close()
    }

    private fun fixture(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): Fixture {
        val store = ClientCookieStore().also { it.setAuthenticationCookies("auth=synthetic") }
        val config = WafRecoveryConfig(challengeTimeoutMillis = 2_000L)
        val coordinator = WafChallengeCoordinator(config, store).also {
            it.setHostAvailability(mounted = true, isForeground = true)
        }
        val client = HttpClient(MockEngine(handler))
        val fetcher = FetchFactory(
            device = FetchFactory.Device.DESKTOP,
            timeoutMillis = 2_000L,
            cookieStore = store,
            recoveryCoordinator = coordinator,
            recoveryConfig = config,
            client = client,
            noRedirectClient = client,
        )
        return Fixture(BlogMutationFactory(fetcher), coordinator, client)
    }

    private data class Fixture(
        val factory: BlogMutationFactory,
        val coordinator: WafChallengeCoordinator,
        val client: HttpClient,
    ) {
        suspend fun submitSyntheticCookie() {
            val state = withTimeout(1_000L) {
                coordinator.hostState.filterIsInstance<WafHostState.Verifying>().first()
            }
            coordinator.submitCookie(state.request.id, "nox_jst_v1=synthetic")
        }

        fun close() {
            coordinator.close()
            client.close()
        }
    }

    private fun MockRequestHandleScope.wafResponse(): HttpResponseData = respond(
        content = "<script>window.__noxExpire=30</script><script src='/nox_1.js'></script>",
        status = HttpStatusCode.MethodNotAllowed,
        headers = headersOf(HttpHeaders.Server, "BAIDU_WAF"),
    )

    private fun MockRequestHandleScope.response(
        body: String,
        location: String? = null,
    ): HttpResponseData = respond(
        content = body,
        status = HttpStatusCode.OK,
        headers = location?.let { headersOf(HttpHeaders.Location, it) } ?: headersOf(),
    )

    private fun assertMultipartField(body: String, name: String, value: String) {
        assertTrue(
            body.contains("name=\"$name\"") && body.contains("\r\n\r\n$value\r\n"),
            "Multipart body did not contain $name",
        )
    }

    private companion object {
        val FORM_HASH = FormHash("testhash")
    }
}
