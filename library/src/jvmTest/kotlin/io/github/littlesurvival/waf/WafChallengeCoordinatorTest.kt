package io.github.littlesurvival.waf

import io.github.littlesurvival.core.WafProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class WafChallengeCoordinatorTest {
    @Test
    fun concurrentRequestsShareOneBrowserFlight() = runBlocking {
        val coordinator = coordinator()
        coordinator.setHostAvailability(mounted = true, isForeground = true)
        var verifierCalls = 0

        val first = async { coordinator.resolveRequest { verifierCalls++; true } }
        val state = coordinator.awaitVerification()
        val second = async(start = CoroutineStart.UNDISPATCHED) {
            coordinator.resolveRequest { verifierCalls++; true }
        }

        coordinator.submitCookie(state.request.id, "nox_jst_v1=synthetic")

        assertIs<WafResolution.Verified>(first.await())
        assertIs<WafResolution.Verified>(second.await())
        assertEquals(1, verifierCalls)
        coordinator.close()
    }

    @Test
    fun cancellingOneWaiterDoesNotCancelRemainingWaiters() = runBlocking {
        val coordinator = coordinator()
        coordinator.setHostAvailability(mounted = true, isForeground = true)

        val cancelled = async { coordinator.resolveRequest { true } }
        val state = coordinator.awaitVerification()
        val remaining = async { coordinator.resolveRequest { true } }
        cancelled.cancelAndJoin()

        coordinator.submitCookie(state.request.id, "nox_jst_v1=synthetic")
        assertIs<WafResolution.Verified>(remaining.await())
        coordinator.close()
    }

    @Test
    fun unavailableHostRequiresForegroundWithoutOpeningFlight() = runBlocking {
        val coordinator = coordinator()
        val result = coordinator.resolveRequest { true }
        assertEquals(
            WafRecoveryDisposition.FOREGROUND_REQUIRED,
            assertIs<WafResolution.Unavailable>(result).disposition,
        )
        assertIs<WafHostState.Idle>(coordinator.hostState.value)
        coordinator.close()
    }

    @Test
    fun timeoutFailsFlightAndRedactsCookieFromStateText() = runBlocking {
        val coordinator = coordinator(timeoutMillis = 150L)
        coordinator.setHostAvailability(mounted = true, isForeground = true)
        val result = async { coordinator.resolveRequest(cookieHeader = "auth=top-secret") { true } }
        val state = coordinator.awaitVerification()
        assertFalse(state.request.toString().contains("top-secret"))
        assertEquals(
            WafRecoveryDisposition.TIMED_OUT,
            assertIs<WafResolution.Unavailable>(result.await()).disposition,
        )
        coordinator.close()
    }

    @Test
    fun timeoutAlsoBoundsCookieVerification() = runBlocking {
        val coordinator = coordinator(timeoutMillis = 150L)
        coordinator.setHostAvailability(mounted = true, isForeground = true)
        val result = async {
            coordinator.resolveRequest {
                delay(500L)
                true
            }
        }
        val state = coordinator.awaitVerification()
        coordinator.submitCookie(state.request.id, "nox_jst_v1=synthetic")

        assertEquals(
            WafRecoveryDisposition.TIMED_OUT,
            assertIs<WafResolution.Unavailable>(result.await()).disposition,
        )
        coordinator.close()
    }

    @Test
    fun leavingForegroundFailsAnActiveFlight() = runBlocking {
        val coordinator = coordinator()
        coordinator.setHostAvailability(mounted = true, isForeground = true)
        val result = async { coordinator.resolveRequest { true } }
        coordinator.awaitVerification()

        coordinator.setHostAvailability(mounted = true, isForeground = false)

        assertEquals(
            WafRecoveryDisposition.FOREGROUND_REQUIRED,
            assertIs<WafResolution.Unavailable>(result.await()).disposition,
        )
        coordinator.close()
    }

    @Test
    fun rejectedCandidateClearsOnlyNoxAndReportsVerificationFailure() = runBlocking {
        val store = ClientCookieStore().also { it.setAuthenticationCookies("auth=kept") }
        val coordinator = WafChallengeCoordinator(
            config = WafRecoveryConfig(challengeTimeoutMillis = 2_000L),
            cookieStore = store,
        )
        coordinator.setHostAvailability(mounted = true, isForeground = true)
        val result = async { coordinator.resolveRequest { false } }
        val state = coordinator.awaitVerification()
        coordinator.submitCookie(state.request.id, "nox_jst_v1=rejected")

        assertEquals(
            WafRecoveryDisposition.VERIFICATION_FAILED,
            assertIs<WafResolution.Unavailable>(result.await()).disposition,
        )
        assertEquals("auth=kept", store.currentHeader())
        coordinator.close()
    }

    @Test
    fun closingSessionCancelsActiveFlight() = runBlocking {
        val coordinator = coordinator()
        coordinator.setHostAvailability(mounted = true, isForeground = true)
        val result = async { coordinator.resolveRequest { true } }
        coordinator.awaitVerification()

        coordinator.close()

        assertEquals(
            WafRecoveryDisposition.CANCELLED,
            assertIs<WafResolution.Unavailable>(result.await()).disposition,
        )
    }

    private fun coordinator(timeoutMillis: Long = 2_000L) = WafChallengeCoordinator(
        config = WafRecoveryConfig(
            challengeTimeoutMillis = timeoutMillis,
        ),
        cookieStore = ClientCookieStore(),
    )

    private suspend fun WafChallengeCoordinator.resolveRequest(
        cookieHeader: String = "auth=synthetic",
        verifier: suspend () -> Boolean,
    ): WafResolution = resolve(
        provider = WafProvider.BAIDU_NOX,
        statusCode = 405,
        url = "https://bbs.yamibo.com/",
        userAgent = "test-agent",
        cookieHeader = cookieHeader,
        verifier = verifier,
    )

    private suspend fun WafChallengeCoordinator.awaitVerification(): WafHostState.Verifying =
        withTimeout(1_000L) {
            hostState.filterIsInstance<WafHostState.Verifying>().first()
        }
}
