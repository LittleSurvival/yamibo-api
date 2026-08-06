package io.github.littlesurvival.waf

import io.github.littlesurvival.core.WafProvider

/** Configuration for the optional browser-based WAF recovery owned by a Yamibo client. */
data class WafRecoveryConfig(
    val enabled: Boolean = true,
    val safeProbeUrl: String = "https://bbs.yamibo.com/",
    val challengeTimeoutMillis: Long = 45_000L,
    val noxSoftLifetimeMillis: Long = 30L * 60L * 1_000L,
) {
    init {
        require(safeProbeUrl.startsWith("https://bbs.yamibo.com/")) {
            "The WAF probe must use the Yamibo HTTPS origin"
        }
        require(challengeTimeoutMillis in 100L..120_000L)
        require(noxSoftLifetimeMillis in 60_000L..3_600_000L)
    }
}

/** Why a detected challenge could not be transparently recovered. */
enum class WafRecoveryDisposition {
    UNRESOLVED,
    FOREGROUND_REQUIRED,
    CANCELLED,
    TIMED_OUT,
    VERIFICATION_FAILED,
    REPLAY_NOT_ALLOWED,
}

internal class WafBrowserRequest(
    val id: Long,
    val provider: WafProvider,
    val statusCode: Int,
    val url: String,
    val userAgent: String,
    val cookieHeader: String,
) {
    override fun toString(): String =
        "WafBrowserRequest(id=$id, provider=$provider, statusCode=$statusCode, url=$url)"
}

internal sealed interface WafHostState {
    data object Idle : WafHostState

    data class Verifying(
        val request: WafBrowserRequest,
        val checkingCookie: Boolean = false,
    ) : WafHostState
}

internal sealed interface WafResolution {
    data object Verified : WafResolution
    data class Unavailable(val disposition: WafRecoveryDisposition) : WafResolution
}
