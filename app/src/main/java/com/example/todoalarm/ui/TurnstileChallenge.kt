package com.example.todoalarm.ui

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Handler
import android.os.Looper
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

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun TurnstileChallenge(
    challengeUrl: String,
    reloadKey: Int,
    onToken: (String) -> Unit,
    onError: () -> Unit,
    onUnavailable: () -> Unit,
) {
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
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        return request.url.host !in allowedHosts
                    }

                    override fun onReceivedError(
                        view: WebView,
                        request: WebResourceRequest,
                        error: android.webkit.WebResourceError,
                    ) {
                        if (request.isForMainFrame) onError()
                    }

                    override fun onReceivedHttpError(
                        view: WebView,
                        request: WebResourceRequest,
                        errorResponse: WebResourceResponse,
                    ) {
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
                        onToken = onToken,
                        onError = onError,
                        onUnavailable = onUnavailable
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

    @JavascriptInterface
    fun onToken(token: String?) {
        val normalized = token?.trim().orEmpty()
        mainHandler.post {
            if (normalized.isNotBlank()) onToken(normalized) else onError()
        }
    }

    @JavascriptInterface
    fun onError() {
        mainHandler.post(onError)
    }

    @JavascriptInterface
    fun onUnavailable() {
        mainHandler.post(onUnavailable)
    }
}
