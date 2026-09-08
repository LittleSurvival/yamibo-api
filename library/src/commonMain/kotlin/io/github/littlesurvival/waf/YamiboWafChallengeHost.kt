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
    val supported = platformNoxWebViewSupported
    val currentForeground by rememberUpdatedState(isForeground)
    val state by client.wafCoordinator.hostState.collectAsState()
    val registration = remember(client) { client.wafCoordinator.registerHost() }

    SideEffect {
        client.wafCoordinator.setHostAvailability(
            registration = registration,
            mounted = supported,
            isForeground = supported && currentForeground,
        )
    }
    DisposableEffect(client, registration) {
        onDispose {
            client.wafCoordinator.unregisterHost(registration)
        }
    }

    val verifying = state as? WafHostState.Verifying ?: return
    if (
        !supported ||
        !currentForeground ||
        verifying.host !== registration ||
        verifying.checkingCookie
    ) return
    val request = verifying.request

    key(request.id) {
        PlatformNoxWebView(
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
