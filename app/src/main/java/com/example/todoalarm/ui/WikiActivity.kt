package com.example.todoalarm.ui

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import androidx.activity.ComponentActivity
import com.example.todoalarm.R

class WikiActivity : ComponentActivity() {
    private var reader: OfflineContentWebView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openReader()
    }
    private fun openReader() {
        runCatching {
            val webView = OfflineContentWebView(this, wiki = true, onFailure = { runOnUiThread { showFailure() } }).apply { displayWiki() }
            reader = webView; setContentView(webView)
        }.onFailure { showFailure() }
    }
    private fun showFailure() {
        reader?.release(); reader = null
        setContentView(android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            addView(android.widget.TextView(this@WikiActivity).apply { text = "使用说明未能打开，请重试或返回。"; textSize = 17f })
            addView(android.widget.Button(this@WikiActivity).apply { text = "重试阅读"; setOnClickListener { openReader() } })
        })
    }
    override fun onDestroy() { reader?.release(); reader = null; super.onDestroy() }
}
