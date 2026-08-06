package io.github.littlesurvival.waf

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.os.Message
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

internal actual val platformNoxWebViewSupported: Boolean = true

internal actual fun clearPlatformNoxCookie() {
    CookieManager.getInstance().apply {
        setCookie(YAMIBO_ORIGIN, "${ClientCookieStore.NOX_COOKIE_NAME}=; Max-Age=0; Path=/; Secure")
        flush()
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal actual fun PlatformNoxWebView(
    request: WafBrowserRequest,
    modifier: Modifier,
    onCookieHeader: (String) -> Unit,
    onError: () -> Unit,
) {
    val webViewState = remember(request.id) { mutableStateOf<WebView?>(null) }
    val pollerState = remember(request.id) { mutableStateOf<Runnable?>(null) }

    DisposableEffect(request.id) {
        onDispose {
            pollerState.value?.let { poller -> webViewState.value?.removeCallbacks(poller) }
            webViewState.value?.apply {
                stopLoading()
                webChromeClient = null
                webViewClient = WebViewClient()
                clearHistory()
                removeAllViews()
                destroy()
            }
            webViewState.value = null
            pollerState.value = null
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply webView@ {
                webViewState.value = this
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = false
                    allowFileAccess = false
                    allowContentAccess = false
                    javaScriptCanOpenWindowsAutomatically = false
                    setSupportMultipleWindows(false)
                    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    loadsImagesAutomatically = false
                    cacheMode = WebSettings.LOAD_DEFAULT
                    userAgentString = request.userAgent
                }

                val cookies = CookieManager.getInstance().apply {
                    setAcceptCookie(true)
                    setAcceptThirdPartyCookies(this@webView, false)
                    setCookie(
                        YAMIBO_ORIGIN,
                        "${ClientCookieStore.NOX_COOKIE_NAME}=; Max-Age=0; Path=/; Secure",
                    )
                }
                ClientCookieStore.withoutNox(request.cookieHeader)
                    .split(';')
                    .map(String::trim)
                    .filter(String::isNotEmpty)
                    .forEach { cookies.setCookie(YAMIBO_ORIGIN, it) }
                cookies.flush()

                val poller = object : Runnable {
                    private var lastSubmittedNox: String? = null

                    override fun run() {
                        val header = cookies.getCookie(YAMIBO_ORIGIN).orEmpty()
                        val nox = ClientCookieStore.extractNoxValue(header)
                        if (nox != null && nox != lastSubmittedNox) {
                            lastSubmittedNox = nox
                            stopLoading()
                            onCookieHeader(header)
                            return
                        }
                        if (webViewState.value === this@webView) {
                            postDelayed(this, COOKIE_POLL_INTERVAL_MILLIS)
                        }
                    }
                }
                pollerState.value = poller

                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        if (!isAllowedYamiboUrl(url)) onError()
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?,
                    ): Boolean = !isAllowedYamiboUrl(request?.url?.toString())

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?,
                    ) {
                        if (request?.isForMainFrame == true) onError()
                    }
                }
                webChromeClient = object : WebChromeClient() {
                    override fun onCreateWindow(
                        view: WebView?,
                        isDialog: Boolean,
                        isUserGesture: Boolean,
                        resultMsg: Message?,
                    ): Boolean = false
                }
                loadUrl(request.url)
                postDelayed(poller, COOKIE_POLL_INTERVAL_MILLIS)
            }
        },
        update = { webViewState.value = it },
    )
}

private fun isAllowedYamiboUrl(rawUrl: String?): Boolean {
    val uri = rawUrl?.let(Uri::parse) ?: return false
    return uri.scheme.equals("https", ignoreCase = true) &&
        uri.host.equals(YAMIBO_HOST, ignoreCase = true)
}

private const val YAMIBO_HOST = "bbs.yamibo.com"
private const val YAMIBO_ORIGIN = "https://bbs.yamibo.com/"
private const val COOKIE_POLL_INTERVAL_MILLIS = 250L
