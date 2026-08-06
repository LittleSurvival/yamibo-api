package io.github.littlesurvival.fetch

import io.github.littlesurvival.core.FetchResult
import io.github.littlesurvival.waf.NoxCookieStore
import io.github.littlesurvival.waf.WafChallengeCoordinator
import io.github.littlesurvival.waf.WafHostState
import io.github.littlesurvival.waf.WafRecoveryConfig
import io.github.littlesurvival.waf.WafRecoveryDisposition
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.setBody
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
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class WafRecoveryTransportTest {
    @Test
    fun safeGetVerifiesCookieAndReplaysOnce() = runBlocking {
        val requests = mutableListOf<String?>()
        var call = 0
        val fixture = fixture { request ->
            requests += request.headers[HttpHeaders.Cookie]
            when (call++) {
                0 -> wafResponse()
                1 -> response("probe-ok")
                else -> response("final-page")
            }
        }

        val result = async { fixture.fetcher.getResult("https://bbs.yamibo.com/thread-1-1-1.html") }
        fixture.submitSyntheticCookie()

        assertEquals("final-page", assertIs<FetchResult.Success<String>>(result.await()).value)
        assertEquals(3, call)
        assertTrue(requests.last().orEmpty().contains("nox_jst_v1=synthetic"))
        fixture.close()
    }

    @Test
    fun ordinary405DoesNotStartRecovery() = runBlocking {
        var call = 0
        val fixture = fixture {
            call++
            respond("permission denied", HttpStatusCode.MethodNotAllowed)
        }

        val result = fixture.fetcher.getResult("https://bbs.yamibo.com/permission")

        assertIs<FetchResult.Failure.HttpError>(result)
        assertEquals(1, call)
        assertIs<WafHostState.Idle>(fixture.coordinator.hostState.value)
        fixture.close()
    }

    @Test
    fun challengedReplayStopsWithoutLooping() = runBlocking {
        var call = 0
        val fixture = fixture {
            when (call++) {
                0 -> wafResponse()
                1 -> response("probe-ok")
                else -> wafResponse()
            }
        }
        val result = async { fixture.fetcher.getResult("https://bbs.yamibo.com/thread") }
        fixture.submitSyntheticCookie()

        val error = assertIs<FetchResult.Failure.HttpError>(result.await())
        assertEquals(WafRecoveryDisposition.VERIFICATION_FAILED, error.wafRecoveryDisposition)
        assertEquals(3, call)
        fixture.close()
    }

    @Test
    fun neverReplayWriteObtainsClearanceButDoesNotRepeatBody() = runBlocking {
        var call = 0
        val fixture = fixture {
            when (call++) {
                0 -> wafResponse()
                else -> response("probe-ok")
            }
        }
        val result = async {
            fixture.fetcher.performBuffered(
                method = HttpMethod.Post,
                url = "https://bbs.yamibo.com/write",
                replayPolicy = ReplayPolicy.NEVER,
            ) { setBody("non-repeatable-by-policy") }
        }
        fixture.submitSyntheticCookie()

        assertEquals(WafRecoveryDisposition.REPLAY_NOT_ALLOWED, result.await().recoveryDisposition)
        assertEquals(2, call)
        fixture.close()
    }

    @Test
    fun approvedRepeatablePostReplaysAndRedirectMetadataSurvives() = runBlocking {
        var call = 0
        val fixture = fixture {
            when (call++) {
                0 -> wafResponse()
                1 -> response("probe-ok")
                else -> respond(
                    content = "",
                    status = HttpStatusCode.Found,
                    headers = headersOf(HttpHeaders.Location, "/search/result"),
                )
            }
        }
        val result = async {
            fixture.fetcher.performBuffered(
                method = HttpMethod.Post,
                url = "https://bbs.yamibo.com/search",
                replayPolicy = ReplayPolicy.AFTER_CONFIRMED_EDGE_REJECTION,
                noRedirect = true,
            ) { setBody("query=repeatable") }
        }
        fixture.submitSyntheticCookie()

        assertEquals(HttpStatusCode.Found, result.await().status)
        assertEquals("/search/result", result.await().location)
        assertEquals(3, call)
        fixture.close()
    }

    private fun fixture(
        handler: suspend io.ktor.client.engine.mock.MockRequestHandleScope.(io.ktor.client.request.HttpRequestData) -> io.ktor.client.request.HttpResponseData,
    ): Fixture {
        val store = NoxCookieStore().also { it.setAuthenticationCookies("auth=synthetic") }
        val config = WafRecoveryConfig(challengeTimeoutMillis = 2_000L)
        val coordinator = WafChallengeCoordinator(config, store).also {
            it.setHostAvailability(mounted = true, isForeground = true)
        }
        val client = HttpClient(MockEngine(handler))
        return Fixture(
            fetcher = FetchFactory(
                device = FetchFactory.Device.MOBILE,
                timeoutMillis = 2_000L,
                cookieStore = store,
                recoveryCoordinator = coordinator,
                recoveryConfig = config,
                client = client,
                noRedirectClient = client,
            ),
            coordinator = coordinator,
            client = client,
        )
    }

    private data class Fixture(
        val fetcher: FetchFactory,
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

    private fun io.ktor.client.engine.mock.MockRequestHandleScope.wafResponse() = respond(
        content = "<script>window.__noxExpire=30</script><script src='/nox_1.js'></script>",
        status = HttpStatusCode.MethodNotAllowed,
        headers = headersOf(HttpHeaders.Server, "BAIDU_WAF"),
    )

    private fun io.ktor.client.engine.mock.MockRequestHandleScope.response(body: String) =
        respond(body, HttpStatusCode.OK)
}
