package com.example.todoalarm.ui

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import androidx.activity.ComponentActivity
import com.example.todoalarm.R

class WikiActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val webView = WebView(this).apply {
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest): WebResourceResponse? {
                    val font = when (request.url.toString()) {
                        "file:///android_asset/fonts/regular.ttf" -> R.font.sarasa_ui_regular
                        "file:///android_asset/fonts/semibold.ttf" -> R.font.sarasa_ui_semibold
                        else -> return super.shouldInterceptRequest(view, request)
                    }
                    return WebResourceResponse("font/ttf", null, resources.openRawResource(font))
                }
            }
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = false
            settings.allowFileAccess = true
            settings.allowContentAccess = false
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            loadUrl("file:///android_asset/wiki/index.html")
        }
        setContentView(webView)
    }
}
