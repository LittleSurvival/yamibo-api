package io.github.littlesurvival.waf

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import platform.Foundation.NSError
import platform.Foundation.NSHTTPCookie
import platform.Foundation.NSHTTPCookieDomain
import platform.Foundation.NSHTTPCookieName
import platform.Foundation.NSHTTPCookiePath
import platform.Foundation.NSHTTPCookieSecure
import platform.Foundation.NSHTTPCookieValue
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationAction
import platform.WebKit.WKNavigationActionPolicy
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKWebView
import platform.WebKit.WKWebsiteDataStore
import platform.darwin.NSObject
import kotlin.coroutines.resume

internal actual val platformNoxWebViewSupported: Boolean = true

internal actual fun clearPlatformNoxCookie() {
    val store = WKWebsiteDataStore.defaultDataStore().httpCookieStore
    store.getAllCookies { cookies ->
        cookies.orEmpty()
            .filterIsInstance<NSHTTPCookie>()
            .filter { it.name.equals(ClientCookieStore.NOX_COOKIE_NAME, ignoreCase = true) }
            .forEach { store.deleteCookie(it, completionHandler = null) }
    }
}

private class NoxNavigationDelegate(
    private val onError: () -> Unit,
) : NSObject(), WKNavigationDelegateProtocol {
    @ObjCSignatureOverride
    override fun webView(
        webView: WKWebView,
        didFailNavigation: WKNavigation?,
        withError: NSError,
    ) {
        onError()
    }

    @ObjCSignatureOverride
    override fun webView(
        webView: WKWebView,
        didFailProvisionalNavigation: WKNavigation?,
        withError: NSError,
    ) {
        onError()
    }

    @ObjCSignatureOverride
    override fun webView(
        webView: WKWebView,
        decidePolicyForNavigationAction: WKNavigationAction,
        decisionHandler: (platform.WebKit.WKNavigationActionPolicy) -> Unit,
    ) {
        val allowed = isAllowedYamiboUrl(decidePolicyForNavigationAction.request.URL)
        decisionHandler(
            if (allowed) {
                WKNavigationActionPolicy.WKNavigationActionPolicyAllow
            } else {
                WKNavigationActionPolicy.WKNavigationActionPolicyCancel
            }
        )
        if (!allowed) onError()
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun PlatformNoxWebView(
    request: WafBrowserRequest,
    modifier: Modifier,
    onCookieHeader: (String) -> Unit,
    onError: () -> Unit,
) {
    val webViewState = remember(request.id) { mutableStateOf<WKWebView?>(null) }
    val delegate = remember(request.id) { NoxNavigationDelegate(onError) }

    LaunchedEffect(request.id) {
        var lastSubmittedNox: String? = null
        while (true) {
            val webView = webViewState.value
            if (webView != null) {
                val header = readCookieHeader(webView)
                val nox = ClientCookieStore.extractNoxValue(header)
                if (nox != null && nox != lastSubmittedNox) {
                    lastSubmittedNox = nox
                    webView.stopLoading()
                    onCookieHeader(header)
                    return@LaunchedEffect
                }
            }
            delay(COOKIE_POLL_INTERVAL_MILLIS)
        }
    }

    DisposableEffect(request.id) {
        onDispose {
            webViewState.value?.apply {
                stopLoading()
                navigationDelegate = null
            }
            webViewState.value = null
        }
    }

    UIKitView(
        modifier = modifier,
        factory = {
            WKWebView().apply {
                customUserAgent = request.userAgent
                navigationDelegate = delegate
                webViewState.value = this
                val target = NSURL(string = request.url)
                seedCookies(this, request.cookieHeader) {
                    loadRequest(NSURLRequest(target))
                }
            }
        },
        update = { webViewState.value = it },
    )
}

private suspend fun readCookieHeader(webView: WKWebView): String =
    suspendCancellableCoroutine { continuation ->
        webView.configuration.websiteDataStore.httpCookieStore.getAllCookies { cookies ->
            val header = cookies.orEmpty()
                .filterIsInstance<NSHTTPCookie>()
                .joinToString("; ") { cookie -> "${cookie.name}=${cookie.value}" }
            if (continuation.isActive) continuation.resume(header)
        }
    }

private fun isAllowedYamiboUrl(url: NSURL?): Boolean =
    url?.scheme.equals("https", ignoreCase = true) &&
        url?.host.equals("bbs.yamibo.com", ignoreCase = true)

private fun seedCookies(webView: WKWebView, rawHeader: String, onComplete: () -> Unit) {
    val store = webView.configuration.websiteDataStore.httpCookieStore
    store.getAllCookies { existingCookies ->
        val staleNoxCookies = existingCookies.orEmpty()
            .filterIsInstance<NSHTTPCookie>()
            .filter { it.name.equals(ClientCookieStore.NOX_COOKIE_NAME, ignoreCase = true) }
        if (staleNoxCookies.isEmpty()) {
            seedAuthenticationCookies(store, rawHeader, onComplete)
            return@getAllCookies
        }

        var remaining = staleNoxCookies.size
        staleNoxCookies.forEach { cookie ->
            store.deleteCookie(cookie) {
                remaining -= 1
                if (remaining == 0) seedAuthenticationCookies(store, rawHeader, onComplete)
            }
        }
    }
}

private fun seedAuthenticationCookies(
    store: platform.WebKit.WKHTTPCookieStore,
    rawHeader: String,
    onComplete: () -> Unit,
) {
    val cookies = ClientCookieStore.withoutNox(rawHeader).split(';').mapNotNull { segment ->
        val separator = segment.indexOf('=')
        if (separator <= 0) return@mapNotNull null
        val name = segment.substring(0, separator).trim()
        val value = segment.substring(separator + 1).trim()
        if (name.isEmpty()) return@mapNotNull null
        NSHTTPCookie.cookieWithProperties(
            mapOf(
                NSHTTPCookieName to name,
                NSHTTPCookieValue to value,
                NSHTTPCookieDomain to "bbs.yamibo.com",
                NSHTTPCookiePath to "/",
                NSHTTPCookieSecure to "TRUE",
            )
        )
    }
    if (cookies.isEmpty()) {
        onComplete()
        return
    }

    var remaining = cookies.size
    cookies.forEach { cookie ->
        store.setCookie(cookie) {
            remaining -= 1
            if (remaining == 0) onComplete()
        }
    }
}

private const val COOKIE_POLL_INTERVAL_MILLIS = 250L
