package io.github.littlesurvival.waf

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ClientCookieStoreTest {
    @Test
    fun clientOwnsOneComposedCookieHeader() {
        val store = ClientCookieStore()
        store.setAuthenticationCookies("auth=secret; sid=abc; nox_jst_v1=stale")
        assertEquals("auth=secret; sid=abc", store.currentHeader())

        store.setNoxCookie("fresh-token", issuedAtEpochMillis = 1_000L)
        assertEquals("auth=secret; sid=abc; nox_jst_v1=fresh-token", store.currentHeader())

        store.clearNoxCookie()
        assertEquals("auth=secret; sid=abc", store.currentHeader())
    }

    @Test
    fun refreshingAuthenticationCookiesPreservesManagedNoxInTheSameHeader() {
        val store = ClientCookieStore()
        store.setAuthenticationCookies("auth=first; sid=one")
        store.setNoxCookie("fresh-token", issuedAtEpochMillis = 1_000L)

        store.setAuthenticationCookies("auth=second; sid=two")

        assertEquals(
            "auth=second; sid=two; nox_jst_v1=fresh-token",
            store.currentHeader(),
        )
    }

    @Test
    fun trustedWebViewImportReplacesManagedNox() {
        val store = ClientCookieStore()
        store.setAuthenticationCookies("auth=first; sid=one")
        store.setNoxCookie("api-token", issuedAtEpochMillis = 1_000L)

        store.importCookies(
            "auth=second; sid=two; nox_jst_v1=webview-token",
            issuedAtEpochMillis = 2_000L,
        )

        assertEquals(
            "auth=second; sid=two; nox_jst_v1=webview-token",
            store.currentHeader(),
        )
    }

    @Test
    fun trustedWebViewImportWithoutNoxPreservesManagedNox() {
        val store = ClientCookieStore()
        store.setAuthenticationCookies("auth=first")
        store.setNoxCookie("api-token", issuedAtEpochMillis = 1_000L)

        store.importCookies("auth=second", issuedAtEpochMillis = 2_000L)

        assertEquals("auth=second; nox_jst_v1=api-token", store.currentHeader())
    }

    @Test
    fun duplicateNoxCookieUsesLastValidValue() {
        assertEquals(
            "new",
            ClientCookieStore.extractNoxValue("nox_jst_v1=old; sid=x; NOX_JST_V1=new"),
        )
    }

    @Test
    fun invalidValuesAreRejectedWithoutExposingTheirContents() {
        val store = ClientCookieStore()
        assertNull(ClientCookieStore.extractNoxValue("nox_jst_v1=bad value"))
        val error = assertFailsWith<IllegalArgumentException> {
            store.setNoxCookie("bad;value", issuedAtEpochMillis = 0L)
        }
        assertFalse(error.message.orEmpty().contains("bad;value"))
    }

    @Test
    fun guestCookiesCannotReplaceAuthenticationCookies() {
        val store = ClientCookieStore()
        store.setAuthenticationCookies("auth=logged-in; sid=authenticated")
        val webViewHeader = "sid=guest; saltkey=guest; nox_jst_v1=clearance"
        store.setNoxCookie(
            requireNotNull(ClientCookieStore.extractNoxValue(webViewHeader)),
            issuedAtEpochMillis = 10L,
        )
        assertEquals(
            "auth=logged-in; sid=authenticated; nox_jst_v1=clearance",
            store.currentHeader(),
        )
    }

    @Test
    fun softExpiryNeverTreatsMissingCookieAsValid() {
        val store = ClientCookieStore()
        assertTrue(store.isNoxSoftExpired(100L, 1_000L))
        store.setNoxCookie("value", issuedAtEpochMillis = 100L)
        assertFalse(store.isNoxSoftExpired(1_099L, 1_000L))
        assertTrue(store.isNoxSoftExpired(1_100L, 1_000L))
    }
}
