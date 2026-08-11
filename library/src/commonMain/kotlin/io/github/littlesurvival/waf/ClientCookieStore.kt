package io.github.littlesurvival.waf

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** Owns the client's single composed Cookie header and NOX soft-expiry metadata. */
internal class ClientCookieStore {
    private val state = MutableStateFlow(CookieState())

    fun setAuthenticationCookies(rawHeader: String) {
        val authenticationHeader = withoutNox(rawHeader)
        state.update { current ->
            current.copy(
                header = compose(authenticationHeader, extractNoxValue(current.header)),
            )
        }
    }

    fun importCookies(rawHeader: String, issuedAtEpochMillis: Long) {
        val authenticationHeader = withoutNox(rawHeader)
        val importedNox = extractNoxValue(rawHeader)
        state.update { current ->
            val currentNox = extractNoxValue(current.header)
            current.copy(
                header = compose(authenticationHeader, importedNox ?: currentNox),
                noxIssuedAtEpochMillis = if (importedNox != null && importedNox != currentNox) {
                    issuedAtEpochMillis
                } else {
                    current.noxIssuedAtEpochMillis
                },
            )
        }
    }

    fun setNoxCookie(value: String, issuedAtEpochMillis: Long) {
        val normalized = normalizeNoxValue(value)
            ?: throw IllegalArgumentException("Invalid NOX cookie value")
        state.update { current ->
            CookieState(
                header = compose(withoutNox(current.header), normalized),
                noxIssuedAtEpochMillis = issuedAtEpochMillis,
            )
        }
    }

    fun clearNoxCookie() {
        state.update { current -> CookieState(header = withoutNox(current.header)) }
    }

    fun clearAuthenticationCookies() {
        setAuthenticationCookies("")
    }

    fun currentHeader(): String? = state.value.header.ifEmpty { null }

    fun isNoxSoftExpired(nowEpochMillis: Long, softLifetimeMillis: Long): Boolean {
        val current = state.value
        val issuedAt = current.noxIssuedAtEpochMillis ?: return true
        return extractNoxValue(current.header) == null ||
            nowEpochMillis - issuedAt >= softLifetimeMillis
    }

    private data class CookieState(
        val header: String = "",
        val noxIssuedAtEpochMillis: Long? = null,
    )

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

        private fun compose(authenticationHeader: String, noxValue: String?): String =
            buildList {
                if (authenticationHeader.isNotEmpty()) add(authenticationHeader)
                if (noxValue != null) add("$NOX_COOKIE_NAME=$noxValue")
            }.joinToString("; ")

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
