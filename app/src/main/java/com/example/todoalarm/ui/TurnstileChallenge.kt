package com.example.todoalarm.ui

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
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
) {
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp),
        factory = { context ->
            val allowedHost = android.net.Uri.parse(challengeUrl).host
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        return request.url.host != allowedHost
                    }

                    override fun onReceivedError(
                        view: WebView,
                        request: WebResourceRequest,
                        error: android.webkit.WebResourceError,
                    ) {
                        if (request.isForMainFrame) onError()
                    }
                }
                addJavascriptInterface(
                    TurnstileBridge(onToken = onToken, onError = onError),
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
}
