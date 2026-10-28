package com.example.todoalarm.data

import com.example.todoalarm.CrashLogger
import com.example.todoalarm.TodoApplication
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** One application-lifetime writer; a burst of edits saves the latest local state. */
class LocalAutoBackup(private val app: TodoApplication) {
    private val requests = Channel<Unit>(Channel.CONFLATED)

    init {
        app.applicationScope.launch {
            for (ignored in requests) {
                delay(500)
                while (requests.tryReceive().isSuccess) { /* Fold pending edits into this snapshot. */ }
                val settings = app.settingsStore.currentSettings()
                val directory = settings.backupDirectoryUri
                if (!settings.autoBackupEnabled || directory == null) continue
                runCatching {
                    val snapshot = app.repository.exportSnapshot(settings)
                    app.backupManager.autoBackupToDirectory(directory, snapshot)
                }.onFailure(CrashLogger::recordNonFatal)
            }
        }
    }

    fun request() { requests.trySend(Unit) }
}
