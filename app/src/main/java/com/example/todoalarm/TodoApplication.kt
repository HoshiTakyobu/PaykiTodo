package com.example.todoalarm

import android.app.Application
import androidx.room.Room
import com.example.todoalarm.alarm.AlarmScheduler
import com.example.todoalarm.alarm.DailyReportNotifier
import com.example.todoalarm.alarm.DailyReportScheduler
import com.example.todoalarm.alarm.EventCheckInWatchdog
import com.example.todoalarm.alarm.ReminderDispatchTracker
import com.example.todoalarm.alarm.ReminderNotifier
import com.example.todoalarm.data.AppDatabase
import com.example.todoalarm.data.BackupManager
import com.example.todoalarm.data.AppSettingsStore
import com.example.todoalarm.data.DatabaseMigrations
import com.example.todoalarm.data.LegacyAiReportMigration
import com.example.todoalarm.data.TodoRepository
import com.example.todoalarm.sync.DesktopSyncCoordinator
import com.example.todoalarm.sync.DesktopSyncService
import com.example.todoalarm.ui.QuoteRepository
import com.example.todoalarm.widget.TodoWidgetProvider
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TodoApplication : Application() {
    val applicationScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, error ->
            CrashLogger.recordNonFatal(error)
        }
    )

    override fun onCreate() {
        super.onCreate()
        CrashLogger.install(this)
        DailyReportNotifier.ensureChannel(this)
        DailyReportScheduler.scheduleNext(this)
        applicationScope.launch {
            repository.ensureDefaultGroups()
            val repairedPlanningItems = repository.ensurePlanningNodeLinkedItems(
                createEventEndTodo = settingsStore.currentSettings().planningEventEndTodoEnabled
            )
            repairedPlanningItems.forEach { item ->
                if (item.completed || item.canceled || !item.reminderEnabled) return@forEach
                ReminderDispatchTracker.clear(applicationContext, item.id)
                val scheduleMessage = alarmScheduler.schedule(item)
                if (scheduleMessage != null) {
                    repository.updateTodo(item.copy(reminderEnabled = false))
                }
            }
            LegacyAiReportMigration.migrateIfNeeded(this@TodoApplication)
            if (settingsStore.currentSettings().desktopSyncEnabled) {
                DesktopSyncService.start(applicationContext)
            }
            delay(5_000L)
            EventCheckInWatchdog.runOnce(applicationContext)
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
            DatabaseMigrations.MIGRATION_8_9,
            DatabaseMigrations.MIGRATION_9_10,
            DatabaseMigrations.MIGRATION_10_11,
            DatabaseMigrations.MIGRATION_11_12,
            DatabaseMigrations.MIGRATION_12_13,
            DatabaseMigrations.MIGRATION_13_14,
            DatabaseMigrations.MIGRATION_14_15,
            DatabaseMigrations.MIGRATION_15_16,
            DatabaseMigrations.MIGRATION_16_17,
            DatabaseMigrations.MIGRATION_17_18,
            DatabaseMigrations.MIGRATION_18_19,
            DatabaseMigrations.MIGRATION_19_20,
            DatabaseMigrations.MIGRATION_20_21,
            DatabaseMigrations.MIGRATION_21_22,
            DatabaseMigrations.MIGRATION_22_23
        )
            .build()
    }

    val repository: TodoRepository by lazy {
        TodoRepository(database.todoDao()) {
            TodoWidgetProvider.notifyWidgetDataChanged(applicationContext)
        }
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
