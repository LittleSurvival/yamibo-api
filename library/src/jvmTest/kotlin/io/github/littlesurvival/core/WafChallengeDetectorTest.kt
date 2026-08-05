package io.github.littlesurvival.core

import io.github.littlesurvival.YamiboClient
import io.github.littlesurvival.fetch.network.WafChallengeDetector
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class WafChallengeDetectorTest {
    @Test
    fun detectsObservedBaiduNox405Body() {
        val error = httpError(
            statusCode = 405,
            body = """
                <!DOCTYPE html>
                <html><head><meta charset="UTF-8">
                <script>window.__noxExpire=30;window.__noxDomain="";window.__noxImd=1;</script>
                <script src="/edge/static/wb/2.1/nox_20260413.js"></script>
                <script src="/edge/static/wb/2.0/gangplank_20251103.js"></script>
                </head></html>
            """.trimIndent(),
        )

        assertEquals(WafProvider.BAIDU_NOX, WafChallengeDetector.detect(error))
    }

    @Test
    fun detectsBaiduWafHeadersWhenChallengeBodyIsUnavailable() {
        val error = httpError(
            statusCode = 403,
            body = null,
            headers = mapOf(
                "Server" to listOf("BAIDU_WAF"),
                "BDWAF-Request-ID" to listOf("redacted"),
            ),
        )

        assertEquals(WafProvider.BAIDU_NOX, WafChallengeDetector.detect(error))
    }

    @Test
    fun doesNotTreatAnOrdinary405AsWafChallenge() {
        val error = httpError(
            statusCode = 405,
            body = "<html><body>Method Not Allowed</body></html>",
        )

        assertNull(WafChallengeDetector.detect(error))
    }

    @Test
    fun requiresAChallengeStatusEvenWhenBodyContainsNoxText() {
        val error = httpError(
            statusCode = 503,
            body = "<script src=\"/static/wb/2.1/nox_20260413.js\"></script>",
        )

        assertNull(WafChallengeDetector.detect(error))
    }

    @Test
    fun yamiboClientMapsObservedResponseToStructuredChallenge() {
        val url = "https://bbs.yamibo.com/forum.php?mod=viewthread&tid=572400"
        val result = YamiboClient().mapFetchFailure(
            httpError(
                statusCode = 405,
                body = "<script>window.__noxExpire=30</script>",
                url = url,
            ),
            url,
        )

        val challenge = assertIs<YamiboResult.WafChallenge>(result)
        assertEquals(WafProvider.BAIDU_NOX, challenge.provider)
        assertEquals(405, challenge.statusCode)
        assertEquals(url, challenge.url)
    }

    @Test
    fun yamiboClientKeepsOrdinary405AsFailure() {
        val result = YamiboClient().mapFetchFailure(
            httpError(
                statusCode = 405,
                body = "<html><body>Method Not Allowed</body></html>",
            ),
            "https://bbs.yamibo.com/",
        )

        assertIs<YamiboResult.Failure>(result)
    }

    @Test
    fun mapSuccessPreservesChallengeMetadata() {
        val challenge = YamiboResult.WafChallenge(
            provider = WafProvider.BAIDU_NOX,
            statusCode = 405,
            url = "https://bbs.yamibo.com/",
        )

        assertEquals(challenge, challenge.mapSuccess { "unused" })
    }

    private fun httpError(
        statusCode: Int,
        body: String?,
        url: String = "https://bbs.yamibo.com/",
        headers: Map<String, List<String>> = emptyMap(),
    ) = FetchResult.Failure.HttpError(
        statusCode = statusCode,
        url = url,
        bodyPreview = body,
        responseHeaders = headers,
    )
}
