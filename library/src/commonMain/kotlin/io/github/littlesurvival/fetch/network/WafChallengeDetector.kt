package io.github.littlesurvival.fetch.network

import io.github.littlesurvival.core.FetchResult
import io.github.littlesurvival.core.WafProvider

/**
 * Recognizes HTTP responses produced by Yamibo's edge WAF browser challenge.
 *
 * The detector is intentionally evidence-based and conservative. Requests routed through the
 * affected Baidu WAF edge have been observed to return HTTP 405 together with a small JavaScript
 * page that creates the `nox_jst_v1` cookie.
 * The same request succeeds after a real browser executes that page and the resulting cookie is
 * sent by the HTTP client.
 *
 * A status code alone is not sufficient evidence. Yamibo may legitimately return HTTP 405 for a
 * normal application-level failure, so this detector requires both a challenge-related status and
 * a known NOX response-body marker. Baidu diagnostic headers are retained for diagnostics but do
 * not initiate recovery alone. This prevents ordinary errors served by the same edge from being
 * incorrectly converted into a browser-verification request.
 *
 * This object only classifies responses. It does not execute JavaScript, create or validate WAF
 * cookies, retry requests, or decide whether replaying a request is safe.
 */
internal object WafChallengeDetector {
    /** Status codes known to be used for an intercepted request on the affected edge route. */
    private const val CHALLENGE_STATUS_CODE = 405

    /**
     * Case-insensitive markers taken from observed Baidu NOX challenge responses.
     *
     * Deliberately avoid matching the broad word `nox`: the more specific markers reduce the risk
     * of treating unrelated forum content as a challenge page.
     */
    private val baiduNoxBodyMarkers = listOf(
        "__noxexpire",
        "/nox_",
        "gangplank_",
    )

    /**
     * Returns the recognized WAF provider, or `null` when [error] should remain a normal HTTP
     * failure.
     *
     * Body markers are checked first because POST factories reliably retain the raw error body.
     * The supplied evidence contains the full 395-character body for every intercepted request,
     * so header-only responses deliberately remain ordinary HTTP failures.
     */
    fun detect(error: FetchResult.Failure.HttpError): WafProvider? {
        // Requiring a challenge-related status keeps marker-like text in ordinary pages harmless.
        if (error.statusCode != CHALLENGE_STATUS_CODE) return null

        val body = error.bodyPreview.orEmpty().lowercase()
        return if (baiduNoxBodyMarkers.any(body::contains)) WafProvider.BAIDU_NOX else null
    }
}
