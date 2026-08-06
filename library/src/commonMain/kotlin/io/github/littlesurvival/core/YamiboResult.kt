package io.github.littlesurvival.core

import io.github.littlesurvival.waf.WafRecoveryDisposition

sealed class YamiboResult<out T> {
    abstract fun message(): String

    data class Success<T>(val value: T) : YamiboResult<T>() {
        override fun message(): String = value.toString()
    }
    data class Failure(val reason: String, val exception: Throwable? = null)
        : YamiboResult<Nothing>() {
        override fun message(): String = reason
    }

    /** The request was intercepted by an edge WAF and requires browser verification. */
    data class WafChallenge(
        val provider: WafProvider,
        val statusCode: Int,
        val url: String,
        val recoveryDisposition: WafRecoveryDisposition = WafRecoveryDisposition.UNRESOLVED,
    ) : YamiboResult<Nothing>() {
        override fun message(): String = when (recoveryDisposition) {
            WafRecoveryDisposition.FOREGROUND_REQUIRED -> "需要回到應用程式完成網站驗證"
            WafRecoveryDisposition.CANCELLED -> "網站驗證已取消"
            WafRecoveryDisposition.TIMED_OUT -> "網站驗證逾時"
            WafRecoveryDisposition.VERIFICATION_FAILED -> "網站驗證失敗"
            WafRecoveryDisposition.REPLAY_NOT_ALLOWED -> "網站驗證完成，請重新執行此操作"
            WafRecoveryDisposition.UNRESOLVED -> "網站要求完成瀏覽器驗證"
        }
    }

    /** The website is currently under maintenance (HTTP 503 or maintenance HTML detected). */
    data object Maintenance : YamiboResult<Nothing>() {
        override fun message(): String = "又到了論壇備份的時間了，大家來杯紅茶休息三十分鐘吧"
    }

    /** The user is not logged in or their session has expired. */
    data object NotLoggedIn : YamiboResult<Nothing>() {
        override fun message(): String = "登入狀態已失效或尚未登入，請重新登入"
    }

    data class NoPermission(val reason: String) : YamiboResult<Nothing>() {
        override fun message(): String = reason
    }
}

fun <T, R> YamiboResult<T>.mapSuccess(transform: (T) -> R): YamiboResult<R> = when (this) {
    is YamiboResult.Success -> YamiboResult.Success(transform(value))
    is YamiboResult.Failure -> this
    is YamiboResult.WafChallenge -> this
    is YamiboResult.NotLoggedIn -> this
    is YamiboResult.NoPermission -> this
    is YamiboResult.Maintenance -> this
}

enum class WafProvider {
    BAIDU_NOX,
}
