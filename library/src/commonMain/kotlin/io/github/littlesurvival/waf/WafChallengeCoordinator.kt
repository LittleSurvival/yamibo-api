package io.github.littlesurvival.waf

import kotlin.concurrent.Volatile
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.onTimeout
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

/** Opaque identity for one mounted challenge-host composition. */
internal class WafHostRegistration

internal class WafChallengeCoordinator(
    private val config: WafRecoveryConfig,
    private val cookieStore: ClientCookieStore,
) {
    private data class HostRecord(
        val registration: WafHostRegistration,
        val registered: Boolean,
        val mounted: Boolean,
        val isForeground: Boolean,
        val generation: Long,
        val changedAtNanos: Long,
        val lastUnavailableAtNanos: Long?,
    ) {
        val usable: Boolean get() = registered && mounted && isForeground
    }

    private data class ActiveAttempt(
        val host: WafHostRegistration,
        val hostGeneration: Long,
        val startedAtNanos: Long,
        val request: WafBrowserRequest,
    )

    private data class ActiveFlight(
        val provider: WafProvider,
        val statusCode: Int,
        val url: String,
        val userAgent: String,
        val cookieHeader: String,
        val createdAtNanos: Long,
        val result: CompletableDeferred<WafResolution> = CompletableDeferred(),
        var attempt: ActiveAttempt? = null,
        var runner: Job? = null,
    )

    private sealed interface CoordinatorEvent {
        data class HostChanged(val record: HostRecord) : CoordinatorEvent
        data class CookieSubmitted(
            val attemptId: Long,
            val value: String,
            val occurredAtNanos: Long,
        ) : CoordinatorEvent

        data class AttemptFailed(
            val attemptId: Long,
            val disposition: WafRecoveryDisposition,
            val occurredAtNanos: Long,
        ) : CoordinatorEvent
    }

    private sealed interface AttemptOutcome {
        data class Candidate(val value: String, val occurredAtNanos: Long) : AttemptOutcome
        data class HostLost(val occurredAtNanos: Long) : AttemptOutcome
        data class Failed(val disposition: WafRecoveryDisposition) : AttemptOutcome
        data object TimedOut : AttemptOutcome
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()
    private val clockOrigin = TimeSource.Monotonic.markNow()
    private val hosts = MutableStateFlow<List<HostRecord>>(emptyList())
    private val events = Channel<CoordinatorEvent>(Channel.UNLIMITED)
    private val _hostState = MutableStateFlow<WafHostState>(WafHostState.Idle)

    @Volatile
    private var closed = false

    private var nextAttemptId = 1L
    private var activeFlight: ActiveFlight? = null

    val hostState: StateFlow<WafHostState> = _hostState.asStateFlow()

    fun registerHost(): WafHostRegistration = WafHostRegistration().also { registration ->
        val now = monotonicNanos()
        hosts.update { records ->
            records.filter(HostRecord::registered) + HostRecord(
                registration = registration,
                registered = true,
                mounted = false,
                isForeground = false,
                generation = 0L,
                changedAtNanos = now,
                lastUnavailableAtNanos = now,
            )
        }
    }

    fun setHostAvailability(
        registration: WafHostRegistration,
        mounted: Boolean,
        isForeground: Boolean,
    ) {
        val now = monotonicNanos()
        var changed: HostRecord? = null
        hosts.update { records ->
            val index = records.indexOfFirst { it.registration === registration }
            val current = records.getOrNull(index)
            if (
                current == null ||
                !current.registered ||
                (current.mounted == mounted && current.isForeground == isForeground)
            ) {
                records
            } else {
                val replacement = current.copy(
                    mounted = mounted,
                    isForeground = isForeground,
                    generation = current.generation + 1L,
                    changedAtNanos = now,
                    lastUnavailableAtNanos = if (mounted && isForeground) {
                        current.lastUnavailableAtNanos
                    } else {
                        now
                    },
                )
                changed = replacement
                records.toMutableList().apply { set(index, replacement) }
            }
        }
        changed?.let { events.trySend(CoordinatorEvent.HostChanged(it)) }
    }

    fun unregisterHost(registration: WafHostRegistration) {
        val now = monotonicNanos()
        var changed: HostRecord? = null
        hosts.update { records ->
            val index = records.indexOfFirst { it.registration === registration }
            val current = records.getOrNull(index)
            if (current == null || !current.registered) {
                records
            } else {
                val replacement = current.copy(
                    registered = false,
                    mounted = false,
                    isForeground = false,
                    generation = current.generation + 1L,
                    changedAtNanos = now,
                    lastUnavailableAtNanos = now,
                )
                changed = replacement
                records.toMutableList().apply { set(index, replacement) }
            }
        }
        changed?.let { events.trySend(CoordinatorEvent.HostChanged(it)) }
    }

    suspend fun resolve(
        provider: WafProvider,
        statusCode: Int,
        url: String,
        userAgent: String,
        cookieHeader: String,
        verifier: suspend () -> Boolean,
    ): WafResolution {
        if (!config.enabled) {
            return WafResolution.Unavailable(WafRecoveryDisposition.FOREGROUND_REQUIRED)
        }
        if (closed) return WafResolution.Unavailable(WafRecoveryDisposition.CANCELLED)

        val flight = mutex.withLock {
            if (closed) return@withLock null
            activeFlight ?: ActiveFlight(
                provider = provider,
                statusCode = statusCode,
                url = url,
                userAgent = userAgent,
                cookieHeader = cookieHeader,
                createdAtNanos = monotonicNanos(),
            ).also { created ->
                activeFlight = created
                _hostState.value = WafHostState.WaitingForHost
                created.runner = scope.launch(start = CoroutineStart.LAZY) {
                    runFlight(created, verifier)
                }.also(Job::start)
            }
        } ?: return WafResolution.Unavailable(WafRecoveryDisposition.CANCELLED)
        return flight.result.await()
    }

    fun submitCookie(attemptId: Long, rawCookieHeader: String) {
        val value = ClientCookieStore.extractNoxValue(rawCookieHeader) ?: return
        events.trySend(
            CoordinatorEvent.CookieSubmitted(
                attemptId = attemptId,
                value = value,
                occurredAtNanos = monotonicNanos(),
            ),
        )
    }

    fun cancel(attemptId: Long) {
        fail(attemptId, WafRecoveryDisposition.CANCELLED)
    }

    fun fail(attemptId: Long, disposition: WafRecoveryDisposition) {
        events.trySend(
            CoordinatorEvent.AttemptFailed(
                attemptId = attemptId,
                disposition = disposition,
                occurredAtNanos = monotonicNanos(),
            ),
        )
    }

    fun close() {
        if (closed) return
        closed = true
        scope.launch {
            mutex.withLock {
                activeFlight?.let { flight ->
                    finishLocked(
                        flight,
                        WafResolution.Unavailable(WafRecoveryDisposition.CANCELLED),
                        cancelRunner = true,
                    )
                }
            }
            scope.cancel()
        }
    }

    private suspend fun runFlight(
        flight: ActiveFlight,
        verifier: suspend () -> Boolean,
    ) {
        val resolution = try {
            executeFlight(flight, verifier)
        } catch (_: CancellationException) {
            return
        } catch (_: Throwable) {
            cookieStore.clearNoxCookie()
            WafResolution.Unavailable(WafRecoveryDisposition.VERIFICATION_FAILED)
        }

        mutex.withLock {
            if (activeFlight === flight) {
                finishLocked(flight, resolution, cancelRunner = false)
            }
        }
    }

    private suspend fun executeFlight(
        flight: ActiveFlight,
        verifier: suspend () -> Boolean,
    ): WafResolution {
        var remainingChallengeNanos = millisToNanos(config.challengeTimeoutMillis)
        var hostWaitStartedAtNanos = flight.createdAtNanos
        drainPendingEvents()

        while (true) {
            val host = awaitUsableHost(hostWaitStartedAtNanos) ?: return WafResolution.Unavailable(
                WafRecoveryDisposition.FOREGROUND_REQUIRED,
            )
            val attemptStartedAtNanos = monotonicNanos()
            val attempt = ActiveAttempt(
                host = host.registration,
                hostGeneration = host.generation,
                startedAtNanos = attemptStartedAtNanos,
                request = WafBrowserRequest(
                    id = nextAttemptId++,
                    provider = flight.provider,
                    statusCode = flight.statusCode,
                    url = flight.url,
                    userAgent = flight.userAgent,
                    cookieHeader = flight.cookieHeader,
                ),
            )
            mutex.withLock {
                if (activeFlight !== flight) {
                    throw CancellationException("WAF recovery flight is no longer active")
                }
                flight.attempt = attempt
                _hostState.value = WafHostState.Verifying(
                    host = host.registration,
                    request = attempt.request,
                )
            }

            val outcome = awaitAttemptOutcome(attempt, remainingChallengeNanos)
            val attemptEndedAtNanos = when (outcome) {
                is AttemptOutcome.Candidate -> outcome.occurredAtNanos
                is AttemptOutcome.HostLost -> outcome.occurredAtNanos
                is AttemptOutcome.Failed,
                AttemptOutcome.TimedOut -> monotonicNanos()
            }
            remainingChallengeNanos = (remainingChallengeNanos -
                (attemptEndedAtNanos - attempt.startedAtNanos).coerceAtLeast(0L))
                .coerceAtLeast(0L)

            when (outcome) {
                is AttemptOutcome.HostLost -> {
                    mutex.withLock {
                        if (activeFlight === flight && flight.attempt === attempt) {
                            flight.attempt = null
                            _hostState.value = WafHostState.WaitingForHost
                        }
                    }
                    hostWaitStartedAtNanos = outcome.occurredAtNanos
                }

                AttemptOutcome.TimedOut -> {
                    cookieStore.clearNoxCookie()
                    return WafResolution.Unavailable(WafRecoveryDisposition.TIMED_OUT)
                }

                is AttemptOutcome.Failed -> {
                    return WafResolution.Unavailable(outcome.disposition)
                }

                is AttemptOutcome.Candidate -> {
                    val verificationBudgetNanos = remainingChallengeNanos -
                        (monotonicNanos() - outcome.occurredAtNanos).coerceAtLeast(0L)
                    if (verificationBudgetNanos <= 0L) {
                        cookieStore.clearNoxCookie()
                        return WafResolution.Unavailable(WafRecoveryDisposition.TIMED_OUT)
                    }
                    val currentState = _hostState.value as? WafHostState.Verifying
                    if (currentState?.request?.id == attempt.request.id) {
                        _hostState.value = currentState.copy(checkingCookie = true)
                    }
                    cookieStore.setNoxCookie(outcome.value, currentEpochMillis())
                    val verified = try {
                        withTimeout(nanosToCeilingMillis(verificationBudgetNanos).milliseconds) {
                            verifier()
                        }
                    } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
                        cookieStore.clearNoxCookie()
                        return WafResolution.Unavailable(WafRecoveryDisposition.TIMED_OUT)
                    } catch (error: CancellationException) {
                        if (!currentCoroutineContext().isActive) throw error
                        false
                    } catch (_: Throwable) {
                        false
                    }
                    return if (verified) {
                        WafResolution.Verified
                    } else {
                        cookieStore.clearNoxCookie()
                        WafResolution.Unavailable(WafRecoveryDisposition.VERIFICATION_FAILED)
                    }
                }
            }
        }
    }

    private suspend fun awaitUsableHost(waitStartedAtNanos: Long): HostRecord? {
        val deadline = waitStartedAtNanos + millisToNanos(config.hostWaitTimeoutMillis)
        while (true) {
            selectUsableHost(hosts.value, deadline)?.let { return it }
            val remainingNanos = deadline - monotonicNanos()
            if (remainingNanos <= 0L) return null
            val arrived = withTimeoutOrNull(nanosToCeilingMillis(remainingNanos).milliseconds) {
                hosts.map { records -> selectUsableHost(records, deadline) }.first { it != null }
            }
            if (arrived != null) return arrived
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun awaitAttemptOutcome(
        attempt: ActiveAttempt,
        remainingNanos: Long,
    ): AttemptOutcome {
        val deadline = attempt.startedAtNanos + remainingNanos
        while (true) {
            currentHostLoss(attempt)?.let { return AttemptOutcome.HostLost(it) }
            val waitNanos = deadline - monotonicNanos()
            if (waitNanos <= 0L) return AttemptOutcome.TimedOut
            val event = select<CoordinatorEvent?> {
                events.onReceive { it }
                onTimeout(nanosToCeilingMillis(waitNanos)) { null }
            } ?: return AttemptOutcome.TimedOut

            when (event) {
                is CoordinatorEvent.HostChanged -> {
                    if (
                        event.record.registration === attempt.host &&
                        event.record.generation > attempt.hostGeneration &&
                        !event.record.usable
                    ) {
                        return if (event.record.changedAtNanos >= deadline) {
                            AttemptOutcome.TimedOut
                        } else {
                            AttemptOutcome.HostLost(event.record.changedAtNanos)
                        }
                    }
                }

                is CoordinatorEvent.CookieSubmitted -> {
                    if (event.attemptId == attempt.request.id) {
                        currentHostLoss(attempt)?.let { return AttemptOutcome.HostLost(it) }
                        return if (event.occurredAtNanos >= deadline) {
                            AttemptOutcome.TimedOut
                        } else {
                            AttemptOutcome.Candidate(event.value, event.occurredAtNanos)
                        }
                    }
                }

                is CoordinatorEvent.AttemptFailed -> {
                    if (event.attemptId == attempt.request.id) {
                        currentHostLoss(attempt)?.let { return AttemptOutcome.HostLost(it) }
                        return if (event.occurredAtNanos >= deadline) {
                            AttemptOutcome.TimedOut
                        } else {
                            AttemptOutcome.Failed(event.disposition)
                        }
                    }
                }
            }
        }
    }

    private fun currentHostLoss(attempt: ActiveAttempt): Long? {
        val current = hosts.value.firstOrNull { it.registration === attempt.host }
            ?: return monotonicNanos()
        if (current.generation <= attempt.hostGeneration) return null
        return current.lastUnavailableAtNanos?.takeIf { it >= attempt.startedAtNanos }
    }

    private fun selectUsableHost(records: List<HostRecord>, deadlineNanos: Long): HostRecord? =
        records.lastOrNull { it.usable && it.changedAtNanos < deadlineNanos }

    private fun drainPendingEvents() {
        while (events.tryReceive().isSuccess) {
            // Before an attempt exists, host state is authoritative and browser callbacks are stale.
        }
    }

    private fun finishLocked(
        flight: ActiveFlight,
        resolution: WafResolution,
        cancelRunner: Boolean,
    ) {
        if (activeFlight !== flight) return
        flight.result.complete(resolution)
        if (cancelRunner) flight.runner?.cancel()
        activeFlight = null
        _hostState.value = WafHostState.Idle
    }

    private fun monotonicNanos(): Long = clockOrigin.elapsedNow().inWholeNanoseconds

    private fun millisToNanos(millis: Long): Long = millis * NANOS_PER_MILLI

    private fun nanosToCeilingMillis(nanos: Long): Long =
        ((nanos + NANOS_PER_MILLI - 1L) / NANOS_PER_MILLI).coerceAtLeast(1L)

    private companion object {
        const val NANOS_PER_MILLI = 1_000_000L
    }
}

internal expect fun currentEpochMillis(): Long
