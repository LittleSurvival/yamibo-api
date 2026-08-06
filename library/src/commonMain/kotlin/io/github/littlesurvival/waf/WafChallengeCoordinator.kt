package io.github.littlesurvival.waf

import io.github.littlesurvival.core.WafProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.milliseconds

internal class WafChallengeCoordinator(
    private val config: WafRecoveryConfig,
    private val cookieStore: ClientCookieStore,
) {
    private data class ActiveFlight(
        val request: WafBrowserRequest,
        val candidateCookie: CompletableDeferred<String> = CompletableDeferred(),
        val result: CompletableDeferred<WafResolution> = CompletableDeferred(),
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()
    private val hostMounted = MutableStateFlow(false)
    private val foreground = MutableStateFlow(false)
    private val _hostState = MutableStateFlow<WafHostState>(WafHostState.Idle)
    private var nextFlightId = 1L
    private var activeFlight: ActiveFlight? = null

    val hostState: StateFlow<WafHostState> = _hostState.asStateFlow()

    fun setHostAvailability(mounted: Boolean, isForeground: Boolean) {
        hostMounted.value = mounted
        foreground.value = isForeground
        if (!mounted || !isForeground) {
            scope.launch { failActive(WafRecoveryDisposition.FOREGROUND_REQUIRED) }
        }
    }

    suspend fun resolve(
        provider: WafProvider,
        statusCode: Int,
        url: String,
        userAgent: String,
        cookieHeader: String,
        verifier: suspend () -> Boolean,
    ): WafResolution {
        if (!config.enabled || !hostMounted.value || !foreground.value) {
            return WafResolution.Unavailable(WafRecoveryDisposition.FOREGROUND_REQUIRED)
        }

        val flight = mutex.withLock {
            activeFlight?.let { return@withLock it }
            val request = WafBrowserRequest(
                id = nextFlightId++,
                provider = provider,
                statusCode = statusCode,
                url = url,
                userAgent = userAgent,
                cookieHeader = cookieHeader,
            )
            ActiveFlight(request).also {
                activeFlight = it
                _hostState.value = WafHostState.Verifying(request)
                scope.launch { runFlight(it, verifier) }
            }
        }
        return flight.result.await()
    }

    fun submitCookie(flightId: Long, rawCookieHeader: String) {
        val value = ClientCookieStore.extractNoxValue(rawCookieHeader) ?: return
        scope.launch {
            mutex.withLock {
                activeFlight
                    ?.takeIf { it.request.id == flightId }
                    ?.candidateCookie
                    ?.complete(value)
            }
        }
    }

    fun cancel(flightId: Long) {
        fail(flightId, WafRecoveryDisposition.CANCELLED)
    }

    fun fail(flightId: Long, disposition: WafRecoveryDisposition) {
        scope.launch {
            mutex.withLock {
                if (activeFlight?.request?.id == flightId) {
                    completeLocked(WafResolution.Unavailable(disposition))
                }
            }
        }
    }

    fun close() {
        scope.launch {
            failActive(WafRecoveryDisposition.CANCELLED)
            scope.cancel()
        }
    }

    private suspend fun runFlight(
        flight: ActiveFlight,
        verifier: suspend () -> Boolean,
    ) {
        val resolution = try {
            withTimeout(config.challengeTimeoutMillis.milliseconds) {
                val candidate = flight.candidateCookie.await()
                val currentState = _hostState.value as? WafHostState.Verifying
                if (currentState?.request?.id == flight.request.id) {
                    _hostState.value = currentState.copy(checkingCookie = true)
                }
                cookieStore.setNoxCookie(candidate, currentEpochMillis())
                if (runCatching { verifier() }.getOrDefault(false)) {
                    WafResolution.Verified
                } else {
                    cookieStore.clearNoxCookie()
                    WafResolution.Unavailable(WafRecoveryDisposition.VERIFICATION_FAILED)
                }
            }
        } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
            cookieStore.clearNoxCookie()
            WafResolution.Unavailable(WafRecoveryDisposition.TIMED_OUT)
        } catch (_: Throwable) {
            cookieStore.clearNoxCookie()
            WafResolution.Unavailable(WafRecoveryDisposition.VERIFICATION_FAILED)
        }

        mutex.withLock {
            if (activeFlight?.request?.id == flight.request.id) {
                completeLocked(resolution)
            } else {
                flight.result.complete(resolution)
            }
        }
    }

    private suspend fun failActive(disposition: WafRecoveryDisposition) {
        mutex.withLock {
            if (activeFlight != null) {
                completeLocked(WafResolution.Unavailable(disposition))
            }
        }
    }

    private fun completeLocked(resolution: WafResolution) {
        activeFlight?.result?.complete(resolution)
        activeFlight = null
        _hostState.value = WafHostState.Idle
    }
}

internal expect fun currentEpochMillis(): Long
