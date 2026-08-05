package io.github.littlesurvival.fetch.network

import io.github.littlesurvival.core.FetchResult
import io.github.littlesurvival.core.WafProvider

/**
 * Recognizes HTTP responses produced by Yamibo's edge WAF browser challenge.
 *
 * The detector is intentionally evidence-based and conservative. Requests routed through the
 * affected Baidu WAF edge have been observed to return HTTP 405 (HTTP 403 is also accepted as a
 * challenge status) together with a small JavaScript page that creates the `nox_jst_v1` cookie.
 * The same request succeeds after a real browser executes that page and the resulting cookie is
 * sent by the HTTP client.
 *
 * A status code alone is not sufficient evidence. Yamibo may legitimately return HTTP 405 for a
 * normal application-level failure, so this detector requires both a challenge-related status and
 * either a known NOX response-body marker or a Baidu WAF response header. This prevents ordinary
 * HTTP errors from being incorrectly converted into a browser-verification request.
 *
 * This object only classifies responses. It does not execute JavaScript, create or validate WAF
 * cookies, retry requests, or decide whether replaying a request is safe. Those responsibilities
 * belong to the application layer, where a platform WebView and request-specific replay policy are
 * available.
 */
internal object WafChallengeDetector {
    /** Status codes known to be used for an intercepted request on the affected edge route. */
    private val challengeStatusCodes = setOf(403, 405)

    /**
     * Case-insensitive markers taken from observed Baidu NOX challenge responses.
     *
     * Deliberately avoid matching the broad word `nox`: the more specific markers reduce the risk
     * of treating unrelated forum content as a challenge page.
     */
    private val baiduNoxBodyMarkers = listOf(
        "__noxexpire",
        "nox_jst_v1",
        "/nox_",
        "/nox/",
        "gangplank_",
        "bdwaf-request-id",
    )

    /**
     * Returns the recognized WAF provider, or `null` when [error] should remain a normal HTTP
     * failure.
     *
     * Body markers are checked first because POST factories reliably retain the raw error body.
     * Selected response headers provide a fallback for responses whose body is missing or has been
     * truncated. Header names are compared case-insensitively as required by HTTP semantics.
     */
    fun detect(error: FetchResult.Failure.HttpError): WafProvider? {
        // Requiring a challenge-related status keeps marker-like text in ordinary pages harmless.
        if (error.statusCode !in challengeStatusCodes) return null

        val body = error.bodyPreview.orEmpty().lowercase()
        if (baiduNoxBodyMarkers.any(body::contains)) return WafProvider.BAIDU_NOX

        // Only diagnostic headers are retained by the fetch layer; sensitive headers are excluded.
        val server = error.responseHeaders.value("server")
        val hasRequestId = error.responseHeaders.keys.any {
            it.equals("bdwaf-request-id", ignoreCase = true)
        }
        return if (server?.contains("baidu_waf", ignoreCase = true) == true || hasRequestId) {
            WafProvider.BAIDU_NOX
        } else {
            null
        }
    }

    /** Retrieves the first value of an HTTP header without relying on its wire casing. */
    private fun Map<String, List<String>>.value(name: String): String? =
        entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value?.firstOrNull()
}
