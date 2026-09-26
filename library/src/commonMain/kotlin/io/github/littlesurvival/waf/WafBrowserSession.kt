package io.github.littlesurvival.waf

/** A single trusted verification session. Cookies and user-agent values must never be logged. */
class WafBrowserSession internal constructor(
    val url: String,
    val userAgent: String,
    cookieHeader: String,
    private val submit: (String) -> Unit,
    private val fail: () -> Unit,
    private val cancel: () -> Unit,
) {
    /** Seed authentication only; an expired clearance must not be resubmitted as a new solution. */
    val cookieHeader: String = ClientCookieStore.withoutNox(cookieHeader)

    /** Submit cookies obtained from the trusted origin; the API verifies clearance before replay. */
    fun submitCookieHeader(value: String) = submit(value)

    fun reportFailure() = fail()

    fun cancel() = cancel.invoke()

    // Do not include URL query strings, headers, callback captures or credentials.
    override fun toString(): String = "WafBrowserSession(redacted)"
}
