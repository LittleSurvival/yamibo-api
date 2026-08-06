package io.github.littlesurvival.waf

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

internal actual val platformNoxWebViewSupported: Boolean = false

internal actual fun clearPlatformNoxCookie() = Unit

@Composable
internal actual fun PlatformNoxWebView(
    request: WafBrowserRequest,
    modifier: Modifier,
    onCookieHeader: (String) -> Unit,
    onError: () -> Unit,
) = Unit
