package io.github.littlesurvival.waf

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class WafBrowserSessionTest {
    @Test
    fun forwardsOnlyExplicitBrowserEventsAndRedactsDiagnostics() {
        val cookies = mutableListOf<String>()
        var failures = 0
        var cancellations = 0
        val session = WafBrowserSession(
            "https://bbs.yamibo.com/?secret=private", "private-agent", "auth=secret; nox_jst_v1=expired",
            cookies::add, { failures++ }, { cancellations++ },
        )
        assertEquals(0, cookies.size)
        assertEquals("auth=secret", session.cookieHeader)
        session.submitCookieHeader("nox_jst_v1=clearance")
        session.reportFailure()
        session.cancel()
        assertEquals(listOf("nox_jst_v1=clearance"), cookies)
        assertEquals(1, failures)
        assertEquals(1, cancellations)
        assertFalse(session.toString().contains("secret"))
        assertFalse(session.toString().contains("private"))
        assertEquals("WafBrowserSession(redacted)", session.toString())
    }
}
