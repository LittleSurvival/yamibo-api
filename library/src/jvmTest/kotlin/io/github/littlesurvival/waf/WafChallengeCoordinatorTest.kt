package io.github.littlesurvival.waf

import kotlinx.coroutines.async
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame

class WafChallengeCoordinatorTest {
    @Test
    fun concurrentRequestsShareOneBrowserFlight() = runBlocking {
        val coordinator = coordinator()
        var verifierCalls = 0

        val first = async(start = CoroutineStart.UNDISPATCHED) {
            coordinator.resolveRequest { verifierCalls++; true }
        }
        val second = async(start = CoroutineStart.UNDISPATCHED) {
            coordinator.resolveRequest { verifierCalls++; true }
        }
        coordinator.attachHost()
        val state = coordinator.awaitVerification()

        coordinator.submitCookie(state.request.id, "nox_jst_v1=synthetic")

        assertIs<WafResolution.Verified>(first.await())
        assertIs<WafResolution.Verified>(second.await())
        assertEquals(1, verifierCalls)
        coordinator.close()
    }

    @Test
    fun cancellingOneWaiterDoesNotCancelRemainingWaiters() = runBlocking {
        val coordinator = coordinator()
        coordinator.attachHost()

        val cancelled = async { coordinator.resolveRequest { true } }
        val state = coordinator.awaitVerification()
        val remaining = async { coordinator.resolveRequest { true } }
        cancelled.cancelAndJoin()

        coordinator.submitCookie(state.request.id, "nox_jst_v1=synthetic")
        assertIs<WafResolution.Verified>(remaining.await())
        coordinator.close()
    }

    @Test
    fun resolveWaitsForHostInsteadOfFailingImmediately() = runBlocking {
        val coordinator = coordinator()
        val result = async { coordinator.resolveRequest { true } }

        delay(100L)
        assertFalse(result.isCompleted)

        coordinator.attachHost()
        val state = coordinator.awaitVerification()
        coordinator.submitCookie(state.request.id, "nox_jst_v1=synthetic")

        assertIs<WafResolution.Verified>(result.await())
        coordinator.close()
    }

    @Test
    fun hostWaitTimeoutFailsWhenNoHostArrives() = runBlocking {
        val coordinator = coordinator(hostWaitTimeoutMillis = 150L)

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
        coordinator.attachHost()
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
        coordinator.attachHost()
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
    fun leavingForegroundKeepsFlightAliveAndPausesChallengeBudget() = runBlocking {
        val coordinator = coordinator(timeoutMillis = 400L)
        val host = coordinator.attachHost()
        val result = async { coordinator.resolveRequest { true } }
        val firstAttempt = coordinator.awaitVerification()

        coordinator.setHostAvailability(host, mounted = true, isForeground = false)
        delay(500L)
        assertFalse(result.isCompleted)

        coordinator.setHostAvailability(host, mounted = true, isForeground = true)
        val resumedAttempt = coordinator.awaitVerificationAfter(firstAttempt.request.id)
        coordinator.submitCookie(resumedAttempt.request.id, "nox_jst_v1=synthetic")

        assertIs<WafResolution.Verified>(result.await())
        coordinator.close()
    }

    @Test
    fun staleHostDetachCannotMakeABackgroundReplacementUsable() = runBlocking {
        val coordinator = coordinator()
        val oldHost = coordinator.registerHost()
        coordinator.setHostAvailability(oldHost, mounted = true, isForeground = true)
        val newHost = coordinator.registerHost()
        coordinator.setHostAvailability(newHost, mounted = true, isForeground = false)

        coordinator.unregisterHost(oldHost)
        coordinator.setHostAvailability(oldHost, mounted = true, isForeground = true)
        val result = async { coordinator.resolveRequest { true } }
        delay(100L)
        assertFalse(result.isCompleted)

        coordinator.setHostAvailability(newHost, mounted = true, isForeground = true)
        val state = coordinator.awaitVerification()
        assertSame(newHost, state.host)
        coordinator.submitCookie(state.request.id, "nox_jst_v1=synthetic")

        assertIs<WafResolution.Verified>(result.await())
        coordinator.close()
    }

    @Test
    fun staleAttemptCallbacksAreIgnoredAfterHostReturns() = runBlocking {
        val coordinator = coordinator(timeoutMillis = 2_000L)
        val host = coordinator.registerHost()
        coordinator.setHostAvailability(host, mounted = true, isForeground = true)
        val result = async { coordinator.resolveRequest { true } }
        val firstAttempt = coordinator.awaitVerification()

        coordinator.setHostAvailability(host, mounted = true, isForeground = false)
        coordinator.awaitHostWait()
        coordinator.setHostAvailability(host, mounted = true, isForeground = true)
        val resumedAttempt = coordinator.awaitVerificationAfter(firstAttempt.request.id)

        coordinator.submitCookie(firstAttempt.request.id, "nox_jst_v1=stale")
        coordinator.fail(firstAttempt.request.id, WafRecoveryDisposition.VERIFICATION_FAILED)
        delay(100L)
        assertFalse(result.isCompleted)

        coordinator.submitCookie(resumedAttempt.request.id, "nox_jst_v1=current")
        assertIs<WafResolution.Verified>(result.await())
        coordinator.close()
    }

    @Test
    fun rapidBackgroundForegroundStillCreatesANewAttempt() = runBlocking {
        val coordinator = coordinator(timeoutMillis = 2_000L)
        val host = coordinator.attachHost()
        val result = async { coordinator.resolveRequest { true } }
        val firstAttempt = coordinator.awaitVerification()

        coordinator.setHostAvailability(host, mounted = true, isForeground = false)
        coordinator.setHostAvailability(host, mounted = true, isForeground = true)

        val resumedAttempt = coordinator.awaitVerificationAfter(firstAttempt.request.id)
        coordinator.submitCookie(firstAttempt.request.id, "nox_jst_v1=stale")
        delay(100L)
        assertFalse(result.isCompleted)

        coordinator.submitCookie(resumedAttempt.request.id, "nox_jst_v1=current")
        assertIs<WafResolution.Verified>(result.await())
        coordinator.close()
    }

    @Test
    fun hostWaitTimeoutAlsoBoundsMidFlightOutages() = runBlocking {
        val coordinator = coordinator(
            timeoutMillis = 2_000L,
            hostWaitTimeoutMillis = 150L,
        )
        val host = coordinator.attachHost()
        val result = async { coordinator.resolveRequest { true } }
        coordinator.awaitVerification()

        coordinator.setHostAvailability(host, mounted = true, isForeground = false)

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
        coordinator.attachHost()
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
    fun verifierCancellationIsReportedAsVerificationFailure() = runBlocking {
        val coordinator = coordinator()
        coordinator.attachHost()
        val result = async {
            coordinator.resolveRequest { throw CancellationException("probe cancelled") }
        }
        val state = coordinator.awaitVerification()

        coordinator.submitCookie(state.request.id, "nox_jst_v1=synthetic")

        assertEquals(
            WafRecoveryDisposition.VERIFICATION_FAILED,
            assertIs<WafResolution.Unavailable>(withTimeout(1_000L) { result.await() }).disposition,
        )
        coordinator.close()
    }

    @Test
    fun closingSessionCancelsActiveFlight() = runBlocking {
        val coordinator = coordinator()
        coordinator.attachHost()
        val result = async { coordinator.resolveRequest { true } }
        coordinator.awaitVerification()

        coordinator.close()

        assertEquals(
            WafRecoveryDisposition.CANCELLED,
            assertIs<WafResolution.Unavailable>(result.await()).disposition,
        )
    }

    @Test
    fun closingSessionCancelsAFlightWaitingForItsFirstHost() = runBlocking {
        val coordinator = coordinator()
        val result = async { coordinator.resolveRequest { true } }
        coordinator.awaitHostWait()

        coordinator.close()

        assertEquals(
            WafRecoveryDisposition.CANCELLED,
            assertIs<WafResolution.Unavailable>(result.await()).disposition,
        )
    }

    @Test
    fun resolveAfterCloseReturnsCancelledWithoutSuspending() = runBlocking {
        val coordinator = coordinator()
        coordinator.close()

        val result = withTimeout(1_000L) { coordinator.resolveRequest { true } }

        assertEquals(
            WafRecoveryDisposition.CANCELLED,
            assertIs<WafResolution.Unavailable>(result).disposition,
        )
    }

    private fun coordinator(
        timeoutMillis: Long = 2_000L,
        hostWaitTimeoutMillis: Long = 2_000L,
    ) = WafChallengeCoordinator(
        config = WafRecoveryConfig(
            challengeTimeoutMillis = timeoutMillis,
            hostWaitTimeoutMillis = hostWaitTimeoutMillis,
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

    private fun WafChallengeCoordinator.attachHost(): WafHostRegistration =
        registerHost().also { registration ->
            setHostAvailability(registration, mounted = true, isForeground = true)
        }

    private suspend fun WafChallengeCoordinator.awaitVerification(): WafHostState.Verifying =
        withTimeout(1_000L) {
            hostState.filterIsInstance<WafHostState.Verifying>().first()
        }

    private suspend fun WafChallengeCoordinator.awaitVerificationAfter(
        requestId: Long,
    ): WafHostState.Verifying = withTimeout(1_000L) {
        hostState.filterIsInstance<WafHostState.Verifying>()
            .first { it.request.id != requestId }
    }

    private suspend fun WafChallengeCoordinator.awaitHostWait() {
        withTimeout(1_000L) {
            hostState.first { it is WafHostState.WaitingForHost }
        }
    }
}
