package io.github.littlesurvival.waf

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import io.github.littlesurvival.YamiboClient

/**
 * Mounts the API-owned browser recovery UI for [client].
 *
 * Applications should place one host above their navigation content. Network code never retains
 * this composable or a platform UI object; it communicates through the client-owned coordinator.
 */
@Composable
fun YamiboWafChallengeHost(
    client: YamiboClient,
    isForeground: Boolean,
    modifier: Modifier = Modifier,
) {
    WafChallengeHost(client, isForeground, modifier, browser = null)
}

/**
 * Supplies an application-owned browser on platforms without a built-in WebView (for example JVM).
 * The browser must use an isolated, ephemeral cookie context for each session, seed cookies only
 * for the trusted Yamibo origin, and dispose that context when this content leaves composition.
 * Never render untrusted external navigation with the session's cookies or persist its clearance.
 * Request coordination, cookie verification and replay policy remain owned by the client.
 */
@Composable
fun YamiboWafChallengeHost(
    client: YamiboClient,
    isForeground: Boolean,
    modifier: Modifier = Modifier,
    browser: @Composable (WafBrowserSession, Modifier) -> Unit,
) {
    WafChallengeHost(client, isForeground, modifier, browser)
}

@Composable
private fun WafChallengeHost(
    client: YamiboClient,
    isForeground: Boolean,
    modifier: Modifier,
    browser: (@Composable (WafBrowserSession, Modifier) -> Unit)?,
) {
    val supported = browser != null || platformNoxWebViewSupported
    val currentForeground by rememberUpdatedState(isForeground)
    val state by client.wafCoordinator.hostState.collectAsState()

    SideEffect {
        client.wafCoordinator.setHostAvailability(
            mounted = supported,
            isForeground = supported && currentForeground,
        )
    }
    DisposableEffect(client, supported) {
        onDispose {
            client.wafCoordinator.setHostAvailability(mounted = false, isForeground = false)
        }
    }

    val verifying = state as? WafHostState.Verifying ?: return
    val request = verifying.request

    key(client, request.id) {
        if (browser != null) {
            val session = remember(client, request.id) {
                WafBrowserSession(
                    url = request.url,
                    userAgent = request.userAgent,
                    cookieHeader = request.cookieHeader,
                    submit = { client.wafCoordinator.submitCookie(request.id, it) },
                    fail = { client.wafCoordinator.fail(request.id, WafRecoveryDisposition.VERIFICATION_FAILED) },
                    cancel = { client.wafCoordinator.cancel(request.id) },
                )
            }
            browser(session, modifier.fillMaxSize())
        } else PlatformNoxWebView(
            request = request,
            modifier = modifier.fillMaxSize(),
            onCookieHeader = { client.wafCoordinator.submitCookie(request.id, it) },
            onError = {
                client.wafCoordinator.fail(
                    request.id,
                    WafRecoveryDisposition.VERIFICATION_FAILED,
                )
            },
        )
    }
}

internal expect val platformNoxWebViewSupported: Boolean

internal expect fun clearPlatformNoxCookie()

@Composable
internal expect fun PlatformNoxWebView(
    request: WafBrowserRequest,
    modifier: Modifier,
    onCookieHeader: (String) -> Unit,
    onError: () -> Unit,
)
