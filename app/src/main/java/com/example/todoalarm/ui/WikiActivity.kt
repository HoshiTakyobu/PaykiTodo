package com.example.todoalarm.ui

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity

class WikiActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val webView = WebView(this).apply {
            webViewClient = WebViewClient()
            settings.javaScriptEnabled = false
            settings.domStorageEnabled = false
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            loadUrl("file:///android_asset/wiki/index.html")
        }
        setContentView(webView)
    }
}
