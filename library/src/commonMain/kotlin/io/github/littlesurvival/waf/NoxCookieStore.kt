package io.github.littlesurvival.waf

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Keeps short-lived WAF clearance separate from the caller-owned Discuz login cookie header.
 *
 * Values are intentionally never exposed through `toString` or diagnostic exceptions.
 */
internal class NoxCookieStore {
    private val authenticationHeader = MutableStateFlow("")
    private val noxValue = MutableStateFlow<String?>(null)
    private val noxIssuedAtEpochMillis = MutableStateFlow<Long?>(null)

    fun setAuthenticationCookies(rawHeader: String) {
        authenticationHeader.value = withoutNox(rawHeader)
    }

    fun setNoxCookie(value: String, issuedAtEpochMillis: Long) {
        noxValue.value = normalizeNoxValue(value)
            ?: throw IllegalArgumentException("Invalid NOX cookie value")
        noxIssuedAtEpochMillis.value = issuedAtEpochMillis
    }

    fun clearNoxCookie() {
        noxValue.value = null
        noxIssuedAtEpochMillis.value = null
    }

    fun clearAll() {
        authenticationHeader.value = ""
        clearNoxCookie()
    }

    fun currentHeader(): String? {
        val auth = authenticationHeader.value
        val nox = noxValue.value
        return buildList {
            if (auth.isNotEmpty()) add(auth)
            if (nox != null) add("$NOX_COOKIE_NAME=$nox")
        }.joinToString("; ").ifEmpty { null }
    }

    fun isNoxSoftExpired(nowEpochMillis: Long, softLifetimeMillis: Long): Boolean {
        val issuedAt = noxIssuedAtEpochMillis.value ?: return true
        return noxValue.value == null || nowEpochMillis - issuedAt >= softLifetimeMillis
    }

    companion object {
        const val NOX_COOKIE_NAME = "nox_jst_v1"

        fun extractNoxValue(rawCookieHeader: String): String? =
            parsePairs(rawCookieHeader)
                .lastOrNull { (name, _) -> name.equals(NOX_COOKIE_NAME, ignoreCase = true) }
                ?.second
                ?.let(::normalizeNoxValue)

        fun normalizeNoxValue(value: String): String? {
            val normalized = value.trim()
            if (normalized.isEmpty() || normalized.length > 4_096) return null
            if (normalized.any { it <= '\u001f' || it == '\u007f' || it == ';' || it.isWhitespace() }) {
                return null
            }
            return normalized
        }

        fun withoutNox(rawCookieHeader: String): String =
            parsePairs(rawCookieHeader)
                .filterNot { (name, _) -> name.equals(NOX_COOKIE_NAME, ignoreCase = true) }
                .joinToString("; ") { (name, value) -> "$name=$value" }

        private fun parsePairs(rawCookieHeader: String): List<Pair<String, String>> =
            rawCookieHeader
                .replace("\r", "")
                .replace("\n", "")
                .split(';')
                .mapNotNull { segment ->
                    val separator = segment.indexOf('=')
                    if (separator <= 0) return@mapNotNull null
                    val name = segment.substring(0, separator).trim()
                    val value = segment.substring(separator + 1).trim()
                    if (name.isEmpty() || name.any { it.isWhitespace() || it <= '\u001f' }) null
                    else name to value
                }
    }
}
