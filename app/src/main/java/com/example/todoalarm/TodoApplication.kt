package com.example.todoalarm

import android.app.Application
import androidx.room.Room
import com.example.todoalarm.alarm.AlarmScheduler
import com.example.todoalarm.alarm.ReminderNotifier
import com.example.todoalarm.data.AppDatabase
import com.example.todoalarm.data.BackupManager
import com.example.todoalarm.data.AppSettingsStore
import com.example.todoalarm.data.DatabaseMigrations
import com.example.todoalarm.data.TodoRepository
import com.example.todoalarm.sync.DesktopSyncCoordinator
import com.example.todoalarm.sync.DesktopSyncService
import com.example.todoalarm.ui.QuoteRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TodoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLogger.install(this)
        CoroutineScope(Dispatchers.IO).launch {
            repository.ensureDefaultGroups()
            if (settingsStore.currentSettings().desktopSyncEnabled) {
                DesktopSyncService.start(applicationContext)
            }
        }
    }

    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "todo-alarm.db"
        ).addMigrations(
            DatabaseMigrations.MIGRATION_1_2,
            DatabaseMigrations.MIGRATION_2_3,
            DatabaseMigrations.MIGRATION_3_4,
            DatabaseMigrations.MIGRATION_4_5,
            DatabaseMigrations.MIGRATION_5_6,
            DatabaseMigrations.MIGRATION_6_7,
            DatabaseMigrations.MIGRATION_7_8,
            DatabaseMigrations.MIGRATION_8_9
        )
            .build()
    }

    val repository: TodoRepository by lazy {
        TodoRepository(database.todoDao())
    }

    val settingsStore: AppSettingsStore by lazy {
        AppSettingsStore(applicationContext)
    }

    val backupManager: BackupManager by lazy {
        BackupManager(applicationContext)
    }

    val quoteRepository: QuoteRepository by lazy {
        QuoteRepository(applicationContext)
    }

    val desktopSyncCoordinator: DesktopSyncCoordinator by lazy {
        DesktopSyncCoordinator(
            context = applicationContext,
            app = this,
            settingsStore = settingsStore
        )
    }

    val alarmScheduler: AlarmScheduler by lazy {
        AlarmScheduler(applicationContext)
    }

    val reminderNotifier: ReminderNotifier by lazy {
        ReminderNotifier(applicationContext)
    }
}
