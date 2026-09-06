package com.example.todoalarm.ui

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.rememberUpdatedState

private const val TURNSTILE_LOG_TAG = "PaykiTodoTurnstile"

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun TurnstileChallenge(
    challengeUrl: String,
    reloadKey: Int,
    onToken: (String) -> Unit,
    onError: () -> Unit,
    onUnavailable: () -> Unit,
) {
    val currentOnToken = rememberUpdatedState(onToken)
    val currentOnError = rememberUpdatedState(onError)
    val currentOnUnavailable = rememberUpdatedState(onUnavailable)

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp),
        factory = { context ->
            val allowedHosts = setOfNotNull(
                Uri.parse(challengeUrl).host,
                "challenges.cloudflare.com",
            )
            WebView(context).apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        super.onPageFinished(view, url)
                        view.evaluateJavascript(
                            """
                            (function() {
                                if (window.__paykiTodoTurnstileWatcher) return;
                                window.__paykiTodoTurnstileWatcher = true;
                                var lastToken = '';
                                var pollHandle = null;
                                function reportToken() {
                                    var token = '';
                                    var fields = document.querySelectorAll('input[name="cf-turnstile-response"], textarea[name="cf-turnstile-response"]');
                                    for (var i = 0; i < fields.length; i++) {
                                        if (fields[i].value) {
                                            token = fields[i].value;
                                            break;
                                        }
                                    }
                                    if (!token && window.turnstile && window.turnstile.getResponse) {
                                        try {
                                            token = window.turnstile.getResponse(window.__paykiTodoTurnstileWidgetId) || '';
                                        } catch (ignored) {}
                                    }
                                    if (token && token !== lastToken && window.PaykiTodoTurnstile) {
                                        lastToken = token;
                                        window.PaykiTodoTurnstile.onToken(token);
                                        if (pollHandle) window.clearInterval(pollHandle);
                                    }
                                }
                                reportToken();
                                if (!lastToken) pollHandle = window.setInterval(reportToken, 500);
                            })();
                            """.trimIndent(),
                            null,
                        )
                    }

                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        val url = request.url
                        val isAllowedInternalDocument = url.scheme == "about" &&
                            (url.toString() == "about:blank" || url.toString() == "about:srcdoc")
                        return !isAllowedInternalDocument && url.host !in allowedHosts
                    }

                    override fun onReceivedError(
                        view: WebView,
                        request: WebResourceRequest,
                        error: android.webkit.WebResourceError,
                    ) {
                        Log.w(TURNSTILE_LOG_TAG, "WebView resource error; mainFrame=${request.isForMainFrame}, code=${error.errorCode}, host=${request.url.host}")
                        if (request.isForMainFrame) onError()
                    }

                    override fun onReceivedHttpError(
                        view: WebView,
                        request: WebResourceRequest,
                        errorResponse: WebResourceResponse,
                    ) {
                        Log.w(TURNSTILE_LOG_TAG, "WebView HTTP error; mainFrame=${request.isForMainFrame}, status=${errorResponse.statusCode}, host=${request.url.host}")
                        if (request.isForMainFrame) onError()
                    }

                    override fun onRenderProcessGone(
                        view: WebView,
                        detail: android.webkit.RenderProcessGoneDetail,
                    ): Boolean {
                        onError()
                        return true
                    }
                }
                addJavascriptInterface(
                    TurnstileBridge(
                        onToken = { token -> currentOnToken.value(token) },
                        onError = { currentOnError.value() },
                        onUnavailable = { currentOnUnavailable.value() },
                    ),
                    "PaykiTodoTurnstile"
                )
                tag = reloadKey
                loadUrl(challengeUrl)
            }
        },
        update = { webView ->
            if (webView.tag != reloadKey) {
                webView.tag = reloadKey
                webView.reload()
            }
        }
    )
}

private class TurnstileBridge(
    private val onToken: (String) -> Unit,
    private val onError: () -> Unit,
    private val onUnavailable: () -> Unit,
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val tokenLock = Any()
    private var lastDeliveredToken: String? = null

    @JavascriptInterface
    fun onToken(token: String?) {
        val normalized = token?.trim().orEmpty()
        if (normalized.isBlank()) {
            mainHandler.post(onError)
            return
        }
        synchronized(tokenLock) {
            if (normalized == lastDeliveredToken) return
            lastDeliveredToken = normalized
        }
        mainHandler.post {
            onToken(normalized)
        }
    }

    @JavascriptInterface
    fun onError(errorCode: String?) {
        Log.w(TAG, "Turnstile error callback received; code=${errorCode.orEmpty()}")
        mainHandler.post(onError)
    }

    @JavascriptInterface
    fun onUnavailable() {
        Log.w(TAG, "Turnstile unavailable callback received")
        mainHandler.post(onUnavailable)
    }

    private companion object {
        const val TAG = "PaykiTodoTurnstile"
    }
}
