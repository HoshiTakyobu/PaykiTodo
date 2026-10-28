package com.example.todoalarm.sync

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.example.todoalarm.TodoApplication
import com.example.todoalarm.sync.RemoteSyncCoordinator.RemoteSyncResult
import com.example.todoalarm.data.*
import kotlinx.coroutines.*
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.*

/** Production sync against a loopback server with controlled outages and delayed responses. */
class AccountSyncAcceptanceTest {
    private val inst get() = InstrumentationRegistry.getInstrumentation()
    private val app get() = inst.targetContext.applicationContext as TodoApplication
    private val store get() = app.settingsStore
    private val server = "https://10.0.2.2:19443"
    private lateinit var original: BackupSnapshot
    private lateinit var saved: List<Pair<SharedPreferences, Map<String, *>>>
    private lateinit var coordinator: RemoteSyncCoordinator
    private lateinit var originalCoordinator: RemoteSyncCoordinator
    private lateinit var ssl: SSLSocketFactory
    private lateinit var hostname: HostnameVerifier
    private var screen: ActivityScenario<androidx.activity.ComponentActivity>? = null
    private val username = "sync_fixture_" + System.currentTimeMillis()
    private val password = "local-fixture-password-1234"

    @Before fun setup() = runBlocking {
        Assume.assumeTrue("Opt-in localhost server required", InstrumentationRegistry.getArguments().getString("localCloudFixture") == "true")
        originalCoordinator = app.remoteSyncCoordinator; originalCoordinator.stop()
        original = app.repository.exportSnapshot(store.currentSettings())
        val preferences = listOf("preferences", "securePreferences").map { AppSettingsStore::class.java.getDeclaredField(it).apply { isAccessible = true }.get(store) as SharedPreferences } +
            listOf("account_expiry", "remembered_login_flags").map { app.getSharedPreferences(it, Context.MODE_PRIVATE) } +
            listOfNotNull(RememberedLoginStore::class.java.getDeclaredField("encrypted").apply { isAccessible = true }.get(RememberedLoginStore(app)) as? SharedPreferences)
        saved = preferences.map { it to it.all.toMap() }
        ssl = HttpsURLConnection.getDefaultSSLSocketFactory(); hostname = HttpsURLConnection.getDefaultHostnameVerifier()
        val tls = SSLContext.getInstance("TLS")
        tls.init(null, arrayOf(object : X509TrustManager {
            override fun getAcceptedIssuers() = emptyArray<X509Certificate>()
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) = Unit
        }), SecureRandom())
        HttpsURLConnection.setDefaultSSLSocketFactory(tls.socketFactory)
        HttpsURLConnection.setDefaultHostnameVerifier { host, _ -> host == "10.0.2.2" }
        coordinator = RemoteSyncCoordinator(app, store, server); replace(coordinator)
        control("""{"outage":false,"holdUpload":false,"holdDownload":false,"release":true}""")
        store.clearRemoteAccount()
        app.repository.importSnapshot(original.copy(tasks = emptyList(), quickChecklistGroups = emptyList(), planningNotes = emptyList(), planningNodes = emptyList(), planningLineMappings = emptyList()), notify = false)
        val result = coordinator.register(username, "Sync fixture", password)
        assertTrue(result.error, result.succeeded)
    }

    @After fun restore() = runBlocking {
        if (!::original.isInitialized) return@runBlocking
        screen?.close()
        shell("svc wifi enable"); shell("svc data enable")
        runCatching { control("""{"outage":false,"holdUpload":false,"holdDownload":false,"release":true}""") }
        coordinator.stop()
        app.repository.importSnapshot(original, notify = false)
        AccountExpiryNotifier.cancel(app)
        saved.forEach { (prefs, values) -> prefs.edit().clear().apply {
            values.forEach { (key, value) -> when (value) {
                is String -> putString(key, value)
                is Boolean -> putBoolean(key, value)
                is Long -> putLong(key, value)
                is Int -> putInt(key, value)
                is Float -> putFloat(key, value)
                is Set<*> -> putStringSet(key, value.filterIsInstance<String>().toSet())
            } }
        }.commit() }
        store.updateThemeMode(original.settings.themeMode)
        replace(originalCoordinator)
        HttpsURLConnection.setDefaultSSLSocketFactory(ssl); HttpsURLConnection.setDefaultHostnameVerifier(hostname)
        AccountExpiryNotifier.schedule(app); originalCoordinator.start()
    }

    @Test fun realLoginScreenRemembersEncryptedCredentialsAndClearsOptOut() = runBlocking {
        inst.uiAutomation.rootInActiveWindow
        RememberedLoginStore(app).save("", "", false, false)
        fun launch() {
            screen = ActivityScenario.launch(Intent().setClassName(app, "androidx.activity.ComponentActivity"))
            screen!!.onActivity { activity -> activity.setContent {
                MaterialTheme {
                    com.example.todoalarm.ui.AccountAuthScreen(
                        onLogin = { name, secret, token -> coordinator.login(name, secret, token) },
                        onRegister = { name, display, secret, token -> coordinator.register(name, display, secret, token) },
                        onRecover = { _, _, _, _ -> "Not used by this fixture" }, onRecoveryCode = {},
                    )
                }
            } }
        }
        launch()
        until { visibleNodes().count { it.className?.toString() == "android.widget.EditText" } == 2 }
        val inputs = visibleNodes().filter { it.className?.toString() == "android.widget.EditText" }
        assertEquals(2, inputs.size)
        tap(inputs[0]); shell("input text $username")
        tap(inputs[1]); shell("input text $password"); shell("input keyevent KEYCODE_BACK")
        delay(400)
        fun checkboxes() = visibleNodes().filter { it.isCheckable }
        tap(checkboxes()[1]); delay(250)
        assertTrue(checkboxes()[0].isChecked); assertTrue(checkboxes()[1].isChecked)
        tap(checkboxes().last()); delay(250)
        tap(visibleNodes().last { it.text?.toString() == "登录" });
        until { RememberedLoginStore(app).rememberPassword }
        assertEquals(username, RememberedLoginStore(app).username)
        assertTrue("Remembered credential must match without logging its value", password == RememberedLoginStore(app).password)
        screen!!.close(); launch(); delay(1000)
        assertTrue(visibleNodes().any { it.text?.toString() == username })
        assertTrue(checkboxes()[0].isChecked); assertTrue(checkboxes()[1].isChecked)
        tap(checkboxes()[1]); delay(250)
        assertEquals("", RememberedLoginStore(app).password)
        tap(checkboxes()[0]); delay(250)
        assertEquals("", RememberedLoginStore(app).username)
        stage("S08", "Physical Android taps and keyboard on real login screen: optional password selects account memory, successful local login persists encrypted values, reopening prefills account, opting out clears both")
    }

    private fun visibleNodes(): List<AccessibilityNodeInfo> {
        fun visit(node: AccessibilityNodeInfo): List<AccessibilityNodeInfo> = listOf(node) +
            (0 until node.childCount).flatMap { index -> node.getChild(index)?.let(::visit).orEmpty() }
        return inst.uiAutomation.rootInActiveWindow?.let(::visit).orEmpty()
    }
    private fun tap(node: AccessibilityNodeInfo) {
        val bounds = Rect(); node.getBoundsInScreen(bounds)
        shell("input tap ${bounds.centerX()} ${bounds.centerY()}")
        Thread.sleep(300)
    }

    @Test fun realAccountPanelConfirmsCloudChoicesAndRestoresLocalSafetyCopy() = runBlocking {
        inst.uiAutomation.rootInActiveWindow
        add("local before cloud replacement")
        until { !store.currentSettings().remoteSyncDirty }
        val cloud = app.repository.exportSnapshot(store.currentSettings()).copy(tasks = listOf(TodoItem(id = 9901,
            title = "visible cloud replacement", dueAtMillis = Long.MAX_VALUE, reminderAtMillis = null,
            reminderEnabled = false, ringEnabled = false, vibrateEnabled = false)))
        coordinator.stop()
        publish(cloud)
        // Keep a deliberate local/cloud conflict until the user confirms.
        store.markRemoteSyncDirty()
        screen = ActivityScenario.launch(Intent().setClassName(app, "androidx.activity.ComponentActivity"))
        screen!!.onActivity { activity -> activity.setContent {
            MaterialTheme {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    com.example.todoalarm.ui.AccountSettingsPanel(store.currentSettings(), {})
                }
            }
        } }
        suspend fun click(label: String) {
            repeat(20) {
                val node = visibleNodes().firstOrNull { it.text?.toString() == label && it.isVisibleToUser }
                if (node != null) { tap(node); delay(400); stage("UI", "Clicked $label"); return }
                shell("input swipe 540 1900 540 800 400"); delay(150)
            }
            error("Visible account control not found: $label")
        }
        click("检查云端")
        until { visibleNodes().any { it.text?.toString() == "确认恢复云端副本" } }
        click("取消")
        assertTrue(app.repository.getAllTodos().any { it.title == "local before cloud replacement" })
        click("检查云端")
        until { visibleNodes().any { it.text?.toString() == "确认恢复云端副本" } }
        click("确认")
        until { app.repository.getAllTodos().singleOrNull()?.title == "visible cloud replacement" }
        click("恢复操作前副本")
        until { visibleNodes().any { it.text?.toString() == "恢复操作前副本" } }
        click("确认恢复")
        until { app.repository.getAllTodos().any { it.title == "local before cloud replacement" } }
        until { !store.currentSettings().remoteSyncDirty }
        click("保留本机上传")
        until { visibleNodes().any { it.text?.toString() == "确认保留本机上传" } }
        click("确认")
        until { !store.currentSettings().remoteSyncDirty }
        assertTrue(remote().tasks.any { it.title == "local before cloud replacement" })
        stage("S09", "Physical account-panel taps: cancel leaves data untouched; confirm cloud restore replaces data; restore-before-copy recovers local task; confirm keep-local uploads recovered task")
    }

    @Test fun credentialMemoryAutoUploadOutageConcurrentWritesConflictCopiesAndReauthentication() = runBlocking {
        val remembered = RememberedLoginStore(app)
        remembered.save(username, password, true, true)
        assertTrue("Encrypted roundtrip must match without logging its value", password == RememberedLoginStore(app).password)
        assertFalse(File(app.applicationInfo.dataDir, "shared_prefs/remembered_login_encrypted.xml").readText().contains(password))
        assertFalse(BackupArchiveCodec.encode(app.repository.exportSnapshot(store.currentSettings())).toString(Charsets.ISO_8859_1).contains(password))
        remembered.forgetPassword(); assertEquals("", RememberedLoginStore(app).password); assertEquals(username, remembered.username)
        stage("S01", "Encrypted credential roundtrip, reopen, forget password and archive exclusion")

        add("automatic local write")
        until { !store.currentSettings().remoteSyncDirty && store.currentSettings().remoteSyncRevision > 0 }
        assertTrue(remote().tasks.any { it.title == "automatic local write" })
        stage("S02", "Repository write automatically uploads to real local server; LAN stays disabled")
        control("""{"outage":true}""")
        shell("svc wifi disable"); shell("svc data disable")
        add("offline write")
        delay(1500)
        assertTrue(AppSettingsStore(app).currentSettings().remoteSyncDirty)
        shell("svc wifi enable"); shell("svc data enable")
        until { runCatching { state(); true }.getOrDefault(false) }
        control("""{"outage":false,"release":true}""")
        until { !store.currentSettings().remoteSyncDirty }
        assertTrue(remote().tasks.any { it.title == "offline write" })
        stage("S03", "Network disabled and service outage retain dirty on disk; Wi-Fi/data restoration automatically uploads without manual sync")

        control("""{"holdUpload":true}""")
        add("upload first")
        until { runCatching { state().optBoolean("entered") }.getOrDefault(false) }
        add("upload in flight")
        assertTrue(store.currentSettings().remoteSyncDirty)
        control("""{"holdUpload":false,"release":true}""")
        until { !store.currentSettings().remoteSyncDirty && store.currentSettings().remoteSyncRevision >= 4 }
        assertTrue(app.repository.getAllTodos().any { it.title == "upload in flight" })
        assertTrue(remote().tasks.any { it.title == "upload in flight" })
        stage("S04", "Real delayed HTTP upload automatically drains a newer local write without a manual sync")

        val cloud = app.repository.exportSnapshot(store.currentSettings()).copy(tasks = listOf(TodoItem(id = 9001, title = "cloud replacement", dueAtMillis = Long.MAX_VALUE,
            reminderAtMillis = null, reminderEnabled = false, ringEnabled = false, vibrateEnabled = false)))
        publish(cloud)
        control("""{"holdDownload":true}""")
        val download = async(Dispatchers.IO) { coordinator.syncNow() }
        until { runCatching { state().optBoolean("entered") }.getOrDefault(false) }
        add("download in flight")
        control("""{"holdDownload":false,"release":true}""")
        download.await()
        assertTrue(app.repository.getAllTodos().any { it.title == "download in flight" })
        assertFalse(app.repository.getAllTodos().any { it.title == "cloud replacement" })
        assertTrue(store.currentSettings().remoteSyncDirty)
        stage("S05", "Delayed cloud download refuses to replace new local writes; conflict retained")

        val stale = coordinator.inspectCloud()
        add("changed after review")
        assertNotNull(runCatching { coordinator.resolveCloud(stale, false) }.exceptionOrNull())
        val reviewed = coordinator.inspectCloud()
        coordinator.resolveCloud(reviewed, false)
        assertEquals("cloud replacement", app.repository.getAllTodos().single().title)
        coordinator.restoreNewestSafetyCopy()
        assertTrue(app.repository.getAllTodos().any { it.title == "changed after review" })
        // Restoring a safety copy intentionally queues an automatic upload.
        // Review the settled revision before confirming another manual upload.
        until { !store.currentSettings().remoteSyncDirty }
        coordinator.resolveCloud(coordinator.inspectCloud(), true)
        assertTrue(remote().tasks.any { it.title == "changed after review" })
        stage("S06", "Stale review rejected; confirmed restore keeps local recovery copy; restore copy and keep-local upload verified")

        control("""{"outage":true}""")
        add("relogin dirty write")
        val revision = store.currentSettings().remoteSyncRevision
        store.clearExpiredRemoteSessionIfNeeded(Long.MAX_VALUE)
        assertTrue(store.hasLocalAccountAccess())
        coordinator.login(username, password)
        assertEquals(revision, store.currentSettings().remoteSyncRevision)
        assertTrue(store.currentSettings().remoteSyncDirty)
        coordinator.stop()
        store.clearRemoteAccount()
        assertFalse(store.hasLocalAccountAccess())
        assertTrue(app.repository.getAllTodos().any { it.title == "relogin dirty write" })
        stage("S07", "Same-account reauthentication preserves revision and dirty under failed sync; explicit logout locks without deleting tasks")
    }

    @Test fun completingWhileUploadIsHeldKeepsMainThreadResponsiveAndEventuallyUploads() = runBlocking {
        val task = add("responsive completion")
        until { !store.currentSettings().remoteSyncDirty && store.currentSettings().remoteSyncRevision > 0 }
        control("""{"holdUpload":true,"release":false}""")
        add("held network write")
        until { state().optBoolean("entered") }
        val viewModels = androidx.lifecycle.ViewModelStore()
        try {
            val started = android.os.SystemClock.elapsedRealtime()
            withContext(Dispatchers.Main) {
                val model = androidx.lifecycle.ViewModelProvider(viewModels,
                    androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory(app))[com.example.todoalarm.ui.TodoViewModel::class.java]
                model.completeTodo(task)
            }
            val dispatchMillis = android.os.SystemClock.elapsedRealtime() - started
            assertTrue("Main-thread completion dispatch took ${dispatchMillis}ms", dispatchMillis < 1_000)
            until { app.repository.getTodo(task.id)?.completed == true }
            repeat(5) { withContext(Dispatchers.Main) { /* UI queue remains live during network hold. */ }; delay(40) }
            assertTrue(state().optBoolean("entered"))
            stage("S08", "Completion saved locally while upload held; main dispatch ${dispatchMillis}ms and five UI heartbeats passed")
        } finally {
            control("""{"holdUpload":false,"release":true}""")
            withContext(Dispatchers.Main) { viewModels.clear() }
        }
        until { !store.currentSettings().remoteSyncDirty }
        assertTrue(remote().tasks.single { it.id == task.id }.completed)
        stage("S09", "Windows bridge account $username")
    }

    @Test fun openedWindowsAutomaticallyReceivesAndroidCompletion() = runBlocking {
        Assume.assumeTrue("Opt-in concurrently running Windows fixture required",
            InstrumentationRegistry.getArguments().getString("liveWindowsFixture") == "true")
        val task = add("live completion propagation")
        add("live untouched task")
        until { !store.currentSettings().remoteSyncDirty && store.currentSettings().remoteSyncRevision > 0 }
        control(JSONObject().put("release", true).put("liveUsername", username)
            .put("livePhase", "android-ready").toString())
        until { state().optString("livePhase") == "windows-ready" }
        val viewModels = androidx.lifecycle.ViewModelStore()
        try {
            withContext(Dispatchers.Main) {
                val model = androidx.lifecycle.ViewModelProvider(viewModels,
                    androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory(app))[com.example.todoalarm.ui.TodoViewModel::class.java]
                model.completeTodo(task)
            }
            until { app.repository.getTodo(task.id)?.completed == true }
            until { state().optString("livePhase") == "windows-completed" }
            assertTrue(remote().tasks.single { it.id == task.id }.completed)
            assertFalse(app.repository.getAllTodos().single { it.title == "live untouched task" }.completed)
            stage("S10", "Already-open Windows automatically removed completed task and persisted Android completion; untouched task retained")
        } finally {
            withContext(Dispatchers.Main) { viewModels.clear() }
        }
    }

    @Test fun completionAutoUploadsAndMissedDirtyFlagIsDetectedAfterRestart() = runBlocking {
        val task = add("completion propagation")
        until { !store.currentSettings().remoteSyncDirty && store.currentSettings().remoteSyncRevision > 0 }
        val beforeCompletion = store.currentSettings().remoteSyncRevision
        app.repository.setCompleted(task.id, true)
        until { !store.currentSettings().remoteSyncDirty && store.currentSettings().remoteSyncRevision > beforeCompletion }
        assertTrue(remote().tasks.single { it.id == task.id }.completed)
        coordinator.stop()
        val local = app.repository.exportSnapshot(store.currentSettings())
        app.repository.importSnapshot(local.copy(tasks = local.tasks.map { if (it.id == task.id) it.copy(title = "unreported local edit") else it }), notify = false)
        assertFalse(store.currentSettings().remoteSyncDirty)
        assertTrue(coordinator.syncNow() is RemoteSyncResult.Uploaded)
        assertEquals("unreported local edit", remote().tasks.single { it.id == task.id }.title)
    }

    @Test fun legacyDivergenceBlocksRepeatedAutomaticPollsUntilConfirmed() = runBlocking {
        val task = add("old cloud content")
        until { !store.currentSettings().remoteSyncDirty && store.currentSettings().remoteSyncRevision > 0 }
        coordinator.stop()
        val revision = store.currentSettings().remoteSyncRevision
        val local = app.repository.exportSnapshot(store.currentSettings())
        app.repository.importSnapshot(local.copy(tasks = local.tasks.map { if (it.id == task.id) it.copy(title = "new phone content") else it }), notify = false)
        store.rememberRemoteDataFingerprint("")
        repeat(3) { assertTrue(coordinator.syncNow() is RemoteSyncResult.Conflict) }
        assertEquals(revision, store.currentSettings().remoteSyncRevision)
        assertEquals("old cloud content", remote().tasks.single { it.id == task.id }.title)
        assertEquals("new phone content", app.repository.getAllTodos().single { it.id == task.id }.title)
        coordinator.resolveCloud(coordinator.inspectCloud(), true)
        assertEquals("new phone content", remote().tasks.single { it.id == task.id }.title)
        assertTrue(coordinator.syncNow() is RemoteSyncResult.UpToDate)
    }

    private suspend fun add(title: String): TodoItem {
        val generation = store.remoteChangeGeneration()
        val item = app.repository.addTodo(TodoItem(title = title, dueAtMillis = Long.MAX_VALUE,
            reminderAtMillis = null, reminderEnabled = false, ringEnabled = false, vibrateEnabled = false))
        // The repository deliberately posts follow-up work asynchronously.
        until { store.remoteChangeGeneration() > generation }
        return item
    }
    private suspend fun until(predicate: suspend () -> Boolean) { repeat(750) { if (predicate()) return; delay(100) }; error("Local sync fixture timeout") }
    private fun replace(value: RemoteSyncCoordinator) {
        val lazy = TodoApplication::class.java.getDeclaredField("remoteSyncCoordinator\$delegate").apply { isAccessible = true }.get(app)
        lazy.javaClass.getDeclaredField("_value").apply { isAccessible = true }.set(lazy, value)
    }
    private fun control(value: String) { request("/fixture/control", "POST", value.toByteArray()) }
    private fun state() = JSONObject(request("/fixture/state", "GET").toString(Charsets.UTF_8))
    private fun remote(): BackupSnapshot = BackupArchiveCodec.decode(RemoteSyncCrypto.decrypt(request("/v2/sync/snapshot", "GET", authenticated = true), store.currentSettings().remoteSyncSecret))
    private fun publish(snapshot: BackupSnapshot) {
        val payload = RemoteSyncCrypto.encrypt(BackupArchiveCodec.encode(snapshot), store.currentSettings().remoteSyncSecret)
        request("/v2/sync/snapshot", "PUT", payload, true, store.currentSettings().remoteSyncRevision)
    }
    private fun request(path: String, method: String, body: ByteArray? = null, authenticated: Boolean = false, revision: Long? = null): ByteArray {
        val connection = URL(server + path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method; connection.connectTimeout = 10000; connection.readTimeout = 25000
            if (authenticated) connection.setRequestProperty("Authorization", "Bearer " + store.currentSettings().remoteSyncAccessToken)
            if (revision != null) { connection.setRequestProperty("If-Match", "\"$revision\""); connection.setRequestProperty("X-Payki-Operation-Id", "fixture:" + java.util.UUID.randomUUID()); connection.setRequestProperty("Content-Type", "application/octet-stream") }
            if (body != null) { connection.doOutput = true; connection.setFixedLengthStreamingMode(body.size); connection.outputStream.use { it.write(body) } }
            check(connection.responseCode in 200..299) { "Local fixture HTTP ${connection.responseCode}" }
            return connection.inputStream.use { it.readBytes() }
        } finally { connection.disconnect() }
    }
    private fun stage(id: String, result: String) {
        val folder = File(app.getExternalFilesDir(null), "interactive-acceptance").apply { mkdirs() }
        File(folder, "sync-steps.txt").appendText("$id PASS $result\n")
        File(app.filesDir, "sync-steps.txt").appendText("$id PASS $result\n")
    }
    private fun shell(command: String) { android.os.ParcelFileDescriptor.AutoCloseInputStream(inst.uiAutomation.executeShellCommand(command)).use { it.readBytes() } }
}
