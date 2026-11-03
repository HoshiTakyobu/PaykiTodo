package com.example.todoalarm.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.json.JSONTokener
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class OfflineContentReaderTest {
    @get:Rule val compose = createComposeRule()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private fun eval(view: OfflineContentWebView, code: String): String {
        var value=""; val latch=CountDownLatch(1)
        instrumentation.runOnMainSync { view.evaluateJavascript(code) { value=it; latch.countDown() } }
        assertTrue(latch.await(8,TimeUnit.SECONDS)); return JSONTokener(value).nextValue().toString()
    }
    private fun ready(view: OfflineContentWebView, text: String) {
        val deadline=System.currentTimeMillis()+15_000
        while(System.currentTimeMillis()<deadline) { if(eval(view,"document.body.innerText").contains(text)) return; Thread.sleep(80) }
        eval(view,"window.__paykiOrigins=[];window.addEventListener('message',e=>window.__paykiOrigins.push({origin:e.origin,sourceNull:e.source===null}));'listening'")
        instrumentation.runOnMainSync { view.display(text + "\n诊断重发", false, 18f) }
        Thread.sleep(300)
        fail("Offline reader never rendered: "+eval(view,"document.body.innerText") + "; " +
            eval(view,"JSON.stringify({href:location.href,origin:location.origin,marked:typeof marked,purify:typeof DOMPurify,events:window.__paykiOrigins})"))
    }
    private fun screenshot(name:String) {
        val folder=File(instrumentation.targetContext.getExternalFilesDir(null),"ux-reader").apply{mkdirs()}
        instrumentation.uiAutomation.takeScreenshot()?.let{bitmap->File(folder,"$name.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()}
    }
    @Test fun sharedSyntaxSecurityAndNarrowLayoutWorkOffline() {
        val raw=instrumentation.context.assets.open("report-syntax.md").bufferedReader().use{it.readText()}
        lateinit var reader:OfflineContentWebView
        compose.setContent { AndroidView(factory={OfflineContentWebView(it).also{view->reader=view;view.display(raw,false,17f)}},modifier=Modifier.fillMaxSize()) }
        compose.waitForIdle(); ready(reader,"今日回顾")
        assertEquals("true",eval(reader,"String(document.querySelectorAll('table').length===1&&document.querySelectorAll('h6').length===1&&document.querySelectorAll('input:disabled').length===2)"))
        assertEquals("true",eval(reader,"String(document.querySelectorAll('img,iframe,script:not([src]),a[href^=javascript],a[href^=file]').length===0)"))
        assertEquals("true",eval(reader,"String(document.documentElement.scrollWidth<=document.documentElement.clientWidth+1)"))
        screenshot("report-light")
        instrumentation.runOnMainSync{reader.display(raw,true,24f)}
        val deadline = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < deadline && eval(reader,"String(document.documentElement.classList.contains('dark'))") != "true") Thread.sleep(50)
        assertEquals("true",eval(reader,"String(document.documentElement.classList.contains('dark')&&getComputedStyle(document.documentElement).fontSize==='24px')"))
        assertEquals("true",eval(reader,"String(document.documentElement.scrollWidth<=document.documentElement.clientWidth+1)"))
        screenshot("report-dark-large")
    }
    @Test fun packagedHelpIsHtmlWithCollapsibleDirectory() {
        lateinit var reader:OfflineContentWebView
        compose.setContent { AndroidView(factory={OfflineContentWebView(it,true).also{view->reader=view;view.displayWiki()}},modifier=Modifier.fillMaxSize()) }
        compose.waitForIdle(); ready(reader,"今日安排")
        val helpDeadline = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < helpDeadline && eval(reader,"String(document.querySelector('details nav').children.length>=10)") != "true") Thread.sleep(50)
        assertEquals(eval(reader,"JSON.stringify({client:document.documentElement.clientWidth,scroll:document.documentElement.scrollWidth,inner:innerWidth,chapters:document.querySelector('details nav').children.length})"),
            "true",eval(reader,"String(document.querySelector('details nav').children.length>=10&&document.documentElement.scrollWidth<=document.documentElement.clientWidth+1)"))
        assertTrue(eval(reader,"document.body.innerText").contains("18765")); screenshot("help-single-column")
    }
    @Test fun failedInitializationShowsSelectableRawTextAndRetries() {
        var attempts = 0
        lateinit var reader: OfflineContentWebView
        compose.setContent {
            OfflineContentReader("# 保留原文\n初始化失败后仍可复制", false, 17f, Modifier.fillMaxSize(), createWebView = { context, failure ->
                attempts++
                if (attempts == 1) throw IllegalStateException("Synthetic WebView initialization failure")
                OfflineContentWebView(context, onFailure = failure).also { reader = it }
            })
        }
        compose.onNodeWithText("# 保留原文\n初始化失败后仍可复制").assertIsDisplayed()
        compose.onNodeWithText("重试阅读").performClick()
        compose.waitForIdle(); ready(reader, "初始化失败后仍可复制")
        assertEquals(2, attempts)
    }
    @Test fun rendererLossRemovesTheViewAndWithdrawsRenderedContent() {
        lateinit var reader: OfflineContentWebView
        compose.setContent {
            OfflineContentReader("# 本账号报告\n原文不丢失", false, 17f, Modifier.fillMaxSize(), createWebView = { context, failure ->
                OfflineContentWebView(context, onFailure = failure).also { reader = it }
            })
        }
        compose.waitForIdle(); ready(reader, "本账号报告")
        instrumentation.runOnMainSync { assertTrue(reader.webViewClient.onRenderProcessGone(reader, null)) }
        compose.onNodeWithText("# 本账号报告\n原文不丢失").assertIsDisplayed()
        instrumentation.runOnMainSync { assertNull(reader.parent); reader.release() }
    }
    @Test fun bundledBodyFontLoadsAsARealLocalResource() {
        val store = com.example.todoalarm.data.AppSettingsStore(instrumentation.targetContext)
        val original = store.currentSettings()
        try {
            store.updateDisplayPreferences(mainFont = "SARASA")
            lateinit var reader: OfflineContentWebView
            compose.setContent { AndroidView(factory = { OfflineContentWebView(it).also { view ->
                reader = view; view.display("# 本地字体\n中文笔画和 123 ABC", false, 17f)
            } }, modifier = Modifier.fillMaxSize()) }
            compose.waitForIdle(); ready(reader, "中文笔画")
            eval(reader, "document.fonts.load('17px PaykiBody','中文').then(()=>window.__fontLoaded=true);'requested'")
            val deadline = System.currentTimeMillis() + 10_000
            while (System.currentTimeMillis() < deadline && eval(reader, "String(window.__fontLoaded===true)") != "true") Thread.sleep(50)
            assertEquals("true", eval(reader, "String([...document.fonts].some(f=>f.family==='PaykiBody'&&f.status==='loaded'))"))
            screenshot("report-bundled-font")
        } finally {
            store.updateDisplayPreferences(original.mainFont, original.customFontPath, original.countdownPastDays, original.hiddenCalendarIds)
        }
    }
    @Test fun emptyPlainMalformedAndLargeReportsReuseOneReaderWithoutLosingText() {
        lateinit var reader: OfflineContentWebView
        compose.setContent { AndroidView(factory = { OfflineContentWebView(it).also { view ->
            reader = view; view.display("", false, 17f)
        } }, modifier = Modifier.fillMaxSize()) }
        compose.waitForIdle(); ready(reader, "（空报告）")
        instrumentation.runOnMainSync { reader.display("普通文本保持原样\n第二行", false, 17f) }
        ready(reader, "普通文本保持原样")
        instrumentation.runOnMainSync { reader.display("未闭合代码\n```text\nstart < end\n保留最后一行", false, 17f) }
        ready(reader, "保留最后一行")
        assertTrue(eval(reader, "document.body.innerText").contains("start < end"))
        val large = "# 长报告\n" + "很多中文段落和时间记录 14:00–17:00，DDL仍为15:00。\n\n".repeat(2000) + "最后内容没有丢失"
        instrumentation.runOnMainSync { reader.display(large, true, 24f) }
        ready(reader, "最后内容没有丢失")
        assertEquals("true", eval(reader, "String(document.documentElement.scrollWidth<=document.documentElement.clientWidth+1)"))
        instrumentation.runOnMainSync { assertNotNull(reader.parent) }
    }
    @Test fun actualHelpActivityRendersOfflineAndBackReleasesItsReader() {
        lateinit var reader: OfflineContentWebView
        androidx.test.core.app.ActivityScenario.launch(WikiActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                reader = activity.findViewById<android.view.ViewGroup>(android.R.id.content).getChildAt(0) as OfflineContentWebView
            }
            ready(reader, "今日安排")
            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            val deadline = System.currentTimeMillis() + 5_000
            while (System.currentTimeMillis() < deadline && scenario.state != androidx.lifecycle.Lifecycle.State.DESTROYED) Thread.sleep(50)
            assertEquals(androidx.lifecycle.Lifecycle.State.DESTROYED, scenario.state)
            instrumentation.runOnMainSync { assertNull(reader.parent) }
        }
    }
}
