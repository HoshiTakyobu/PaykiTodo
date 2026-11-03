package com.example.todoalarm.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebMessage
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.todoalarm.BuildConfig
import com.example.todoalarm.R
import com.example.todoalarm.data.AppSettingsStore
import com.example.todoalarm.data.ThemeMode
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.File

internal class OfflineContentWebView(context: Context, private val wiki: Boolean = false, private val onFailure: () -> Unit = {}) : WebView(context) {
    private var payload = ""
    private var ready = false
    private var disposed = false
    private var failureReported = false
    private val initializationTimeout = Runnable { fail() }
    private val origin = "https://payki.content"
    private val settingsStore = AppSettingsStore(context)
    private var preferences = settingsStore.currentSettings()
    init {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = false
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.blockNetworkLoads = true
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.setSupportMultipleWindows(false)
        webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest): WebResourceResponse {
                if (request.url.scheme != "https" || request.url.host != "payki.content" || request.url.query != null)
                    return blocked()
                val path = request.url.path.orEmpty().removePrefix("/")
                if (path == "content-viewer/body-font.ttf") {
                    val stream = when (preferences.mainFont) {
                        "SARASA" -> bundledBodyFont()
                        "CUSTOM" -> runCatching { File(preferences.customFontPath).inputStream() }.getOrNull()
                        else -> null
                    }
                    return if (stream != null) WebResourceResponse("font/ttf", null, stream) else blocked()
                }
                if (path !in Assets) return blocked()
                return runCatching {
                    val mime = when (path.substringAfterLast('.')) { "js" -> "application/javascript"; "css" -> "text/css"; else -> "text/html" }
                    WebResourceResponse(mime, "UTF-8", context.assets.open(path))
                }.getOrElse { blocked() }
            }
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest): Boolean {
                val url = request.url
                if (url.scheme == "https" && url.host == "payki.content" && url.path == entryPath) return false
                if (request.hasGesture() && url.scheme in setOf("https", "http") && url.userInfo == null)
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url)) }
                return true
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                if (disposed || failureReported || url != origin + entryPath) return
                val check = if (wiki) "document.querySelector('details nav')?.children.length > 0"
                    else "typeof marked !== 'undefined' && typeof DOMPurify !== 'undefined' && document.getElementById('content') !== null"
                evaluateJavascript(check) { result ->
                    if (disposed || failureReported) return@evaluateJavascript
                    if (result == "true") { removeCallbacks(initializationTimeout); ready = true; send() }
                    else fail()
                }
            }
            override fun onReceivedError(view: WebView?, request: WebResourceRequest, error: android.webkit.WebResourceError) {
                if (request.isForMainFrame) fail()
            }
            override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest, response: WebResourceResponse) {
                if (request.isForMainFrame && response.statusCode >= 400) fail()
            }
            override fun onRenderProcessGone(view: WebView?, detail: android.webkit.RenderProcessGoneDetail?): Boolean {
                fail(); release(); return true
            }
        }
        postDelayed(initializationTimeout, 15_000)
        loadUrl(origin + entryPath)
    }
    private val entryPath get() = if (wiki) "/wiki/index.html" else "/content-viewer/index.html"
    // This font is a packaged TTF file. Resources can read its bytes; the @RawRes lint annotation is narrower.
    @android.annotation.SuppressLint("ResourceType")
    private fun bundledBodyFont() = resources.openRawResource(R.font.sarasa_ui_regular)
    fun display(content: String, dark: Boolean, fontSize: Float = 17f) {
        preferences = settingsStore.currentSettings()
        val font = when (preferences.mainFont) { "SARASA", "CUSTOM" -> "PaykiBody"; "SERIF" -> "serif"; "MONOSPACE" -> "monospace"; else -> "sans-serif" }
        val value = JSONObject().put("content", content).put("dark", dark).put("fontSize", fontSize)
            .put("fontFamily", font).put("version", BuildConfig.VERSION_NAME).toString()
        if (value != payload) { payload = value; send() }
    }
    fun displayWiki() {
        val dark = preferences.themeMode == ThemeMode.DARK || (preferences.themeMode == ThemeMode.SYSTEM && resources.configuration.uiMode and 0x30 == 0x20)
        display("", dark, 17f * resources.configuration.fontScale)
    }
    private fun send() {
        if (!disposed && !failureReported && ready && payload.isNotEmpty())
            runCatching { postWebMessage(WebMessage(payload), Uri.parse(origin)) }.onFailure { fail() }
    }
    private fun fail() {
        if (disposed || failureReported) return
        failureReported = true; ready = false; removeCallbacks(initializationTimeout); onFailure()
    }
    fun release() {
        if (disposed) return
        disposed = true; ready = false; payload = ""; removeCallbacks(initializationTimeout)
        (parent as? ViewGroup)?.removeView(this)
        stopLoading(); removeAllViews(); destroy()
    }
    companion object {
        private val Assets = setOf("wiki/index.html", "wiki/wiki.js", "content-viewer/index.html", "content-viewer/viewer.css", "content-viewer/viewer.js", "content-viewer/vendor/marked.js", "content-viewer/vendor/purify.js")
        private fun blocked() = WebResourceResponse("text/plain", "UTF-8", 403, "Blocked", emptyMap(), ByteArrayInputStream(byteArrayOf()))
    }
}

@Composable
internal fun OfflineContentReader(content: String, dark: Boolean, fontSize: Float, modifier: Modifier = Modifier,
    createWebView: (Context, () -> Unit) -> OfflineContentWebView = { context, onFailure -> OfflineContentWebView(context, onFailure = onFailure) }) {
    var failed by remember(content) { mutableStateOf(false) }
    if (failed) {
        Column(modifier.verticalScroll(rememberScrollState())) {
            Text("阅读器未能打开。可选择并复制原文，或重试。")
            TextButton(onClick = { failed = false }) { Text("重试阅读") }
            SelectionContainer { Text(content.ifBlank { "（空报告）" }, Modifier.fillMaxWidth()) }
        }
    } else {
        AndroidView(factory = { context -> android.widget.FrameLayout(context).apply {
            try { addView(createWebView(context) { post { failed = true } },
                android.widget.FrameLayout.LayoutParams(-1, -1)) }
            catch (_: Exception) { post { failed = true } }
        } }, modifier = modifier,
            update = { (it.getChildAt(0) as? OfflineContentWebView)?.display(content, dark, fontSize) },
            onRelease = { (it.getChildAt(0) as? OfflineContentWebView)?.release(); it.removeAllViews() })
    }
}
