package io.github.littlesurvival

import io.github.littlesurvival.core.FetchResult
import io.github.littlesurvival.core.ParseResult
import io.github.littlesurvival.core.YamiboResult

interface Fetcher<T> {
    suspend fun getResult(url: String): FetchResult<T>

    /** Replaces caller-managed cookies; this does not import `nox_jst_v1`. */
    fun setCookies(cookie: String)

    /** Clears caller-managed cookies and preserves `nox_jst_v1` unless [clearNox] is true. */
    fun clearCookies(clearNox: Boolean = false)

}
