package com.example.todoalarm.ui

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
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
import org.json.JSONObject

private const val TURNSTILE_LOG_TAG = "PaykiTodoTurnstile"
private const val TURNSTILE_POLL_INTERVAL_MS = 500L
private const val TURNSTILE_POLL_TIMEOUT_MS = 120_000L

private const val TURNSTILE_TOKEN_PROBE_SCRIPT = """
(function() {
    var token = '';
    var fields = document.querySelectorAll('input[name="cf-turnstile-response"], textarea[name="cf-turnstile-response"]');
    for (var i = 0; i < fields.length; i++) {
        if (fields[i].value) {
            token = fields[i].value;
            break;
        }
    }
    if (!token && window.turnstile && typeof window.turnstile.getResponse === 'function') {
        try {
            token = window.turnstile.getResponse(window.__paykiTodoTurnstileWidgetId) || '';
        } catch (ignored) {}
    }
    return {
        token: token,
        turnstileReady: !!window.turnstile,
        rendered: !!window.__paykiTodoTurnstileRendered
    };
})()
"""

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
            TurnstileChallengeWebView(
                context = context,
                challengeUrl = challengeUrl,
                onToken = { currentOnToken.value(it) },
                onError = { currentOnError.value() },
                onUnavailable = { currentOnUnavailable.value() },
            ).also { webView ->
                webView.tag = reloadKey
                webView.loadChallenge()
            }
        },
        update = { webView ->
            if (webView.tag != reloadKey) {
                webView.tag = reloadKey
                webView.reloadChallenge()
            }
        },
        onRelease = { webView ->
            webView.destroy()
        },
    )
}

private class TurnstileChallengeWebView(
    context: Context,
    private val challengeUrl: String,
    private val onToken: (String) -> Unit,
    private val onError: () -> Unit,
    private val onUnavailable: () -> Unit,
) : WebView(context) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val allowedHosts = setOfNotNull(
        Uri.parse(challengeUrl).host,
        "challenges.cloudflare.com",
    )
    private var pollGeneration = 0
    private var pollingStartedAt = 0L
    private var lastDeliveredToken: String? = null
    private var destroyed = false

    private val bridge = object {
        @JavascriptInterface
        fun onToken(token: String?) {
            mainHandler.post {
                deliverToken(token.orEmpty())
            }
        }

        @JavascriptInterface
        fun onError(errorCode: String?) {
            Log.w(TURNSTILE_LOG_TAG, "Turnstile error callback received; code=${errorCode.orEmpty()}")
            mainHandler.post {
                stopTokenPolling()
                onError()
            }
        }

        @JavascriptInterface
        fun onUnavailable() {
            Log.w(TURNSTILE_LOG_TAG, "Turnstile unavailable callback received")
            mainHandler.post {
                stopTokenPolling()
                onUnavailable()
            }
        }
    }

    init {
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
        addJavascriptInterface(bridge, "PaykiTodoTurnstile")
        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                startTokenPolling()
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
                Log.w(
                    TURNSTILE_LOG_TAG,
                    "WebView resource error; mainFrame=${request.isForMainFrame}, code=${error.errorCode}, host=${request.url.host}",
                )
                if (request.isForMainFrame) {
                    stopTokenPolling()
                    onError()
                }
            }

            override fun onReceivedHttpError(
                view: WebView,
                request: WebResourceRequest,
                errorResponse: WebResourceResponse,
            ) {
                Log.w(
                    TURNSTILE_LOG_TAG,
                    "WebView HTTP error; mainFrame=${request.isForMainFrame}, status=${errorResponse.statusCode}, host=${request.url.host}",
                )
                if (request.isForMainFrame) {
                    stopTokenPolling()
                    onError()
                }
            }

            override fun onRenderProcessGone(
                view: WebView,
                detail: android.webkit.RenderProcessGoneDetail,
            ): Boolean {
                stopTokenPolling()
                onError()
                return true
            }
        }
    }

    fun loadChallenge() {
        loadUrl(challengeUrl)
    }

    fun reloadChallenge() {
        stopTokenPolling()
        lastDeliveredToken = null
        reload()
    }

    private fun startTokenPolling() {
        stopTokenPolling()
        lastDeliveredToken = null
        pollingStartedAt = SystemClock.elapsedRealtime()
        val generation = ++pollGeneration
        probeToken(generation)
    }

    private fun stopTokenPolling() {
        pollGeneration += 1
        mainHandler.removeCallbacksAndMessages(null)
    }

    private fun probeToken(generation: Int) {
        if (generation != pollGeneration || destroyed) return
        evaluateJavascript(TURNSTILE_TOKEN_PROBE_SCRIPT) { rawResult ->
            if (generation != pollGeneration || destroyed) return@evaluateJavascript
            val token = rawResult
                ?.takeUnless { it.isBlank() || it == "null" }
                ?.let { result -> runCatching { JSONObject(result).optString("token") }.getOrDefault("") }
                .orEmpty()
            if (deliverToken(token)) return@evaluateJavascript
            if (SystemClock.elapsedRealtime() - pollingStartedAt >= TURNSTILE_POLL_TIMEOUT_MS) {
                Log.w(TURNSTILE_LOG_TAG, "Timed out waiting for a Turnstile token")
                stopTokenPolling()
                onError()
                return@evaluateJavascript
            }
            mainHandler.postDelayed(
                { probeToken(generation) },
                TURNSTILE_POLL_INTERVAL_MS,
            )
        }
    }

    private fun deliverToken(rawToken: String): Boolean {
        val token = rawToken.trim()
        if (token.isBlank() || token == lastDeliveredToken) return false
        lastDeliveredToken = token
        stopTokenPolling()
        Log.i(TURNSTILE_LOG_TAG, "Turnstile token received; length=${token.length}")
        onToken(token)
        return true
    }

    override fun destroy() {
        destroyed = true
        stopTokenPolling()
        removeJavascriptInterface("PaykiTodoTurnstile")
        super.destroy()
    }
}
