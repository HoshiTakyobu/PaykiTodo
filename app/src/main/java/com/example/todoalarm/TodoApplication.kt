package com.example.todoalarm

import android.app.Application
import androidx.room.Room
import com.example.todoalarm.alarm.AlarmScheduler
import com.example.todoalarm.alarm.DailyReportNotifier
import com.example.todoalarm.alarm.DailyReportScheduler
import com.example.todoalarm.alarm.DailyBriefNotifier
import com.example.todoalarm.alarm.DailyBriefScheduler
import com.example.todoalarm.alarm.OngoingEventNotifier
import com.example.todoalarm.alarm.PlanningAiSwitchNotifier
import com.example.todoalarm.alarm.EventCheckInWatchdog
import com.example.todoalarm.alarm.ReminderDispatchTracker
import com.example.todoalarm.alarm.ReminderNotifier
import com.example.todoalarm.data.AppDatabase
import com.example.todoalarm.data.BackupManager
import com.example.todoalarm.data.AppSettingsStore
import com.example.todoalarm.data.DatabaseMigrations
import com.example.todoalarm.data.LegacyAiReportMigration
import com.example.todoalarm.data.PlanningAiProvider
import com.example.todoalarm.data.TodoRepository
import com.example.todoalarm.sync.DesktopSyncCoordinator
import com.example.todoalarm.sync.DesktopSyncService
import com.example.todoalarm.sync.RemoteSyncCoordinator
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
        val skipReminderRecovery = SafeStartupGuard.enterStartup(this)
        runCatching {
            alarmScheduler.cancelLegacyBackupAlarms()
            DailyReportNotifier.ensureChannel(this)
            PlanningAiSwitchNotifier.ensureChannel(this)
            DailyBriefNotifier.ensureChannel(this)
            DailyBriefScheduler.scheduleNext(this)
            OngoingEventNotifier.ensureChannel(this)
        }.onFailure(CrashLogger::recordNonFatal)
        applicationScope.launch {
            try {
                repository.ensureDefaultGroups()
                if (!skipReminderRecovery && settingsStore.hasLocalAccountAccess()) {
                    repository.getAllTodos().forEach(alarmScheduler::recover)
                    val replenishedRecurringItems = repository.ensureRecurringInstancesAhead()
                    val repairedPlanningItems = repository.ensurePlanningNodeLinkedItems(
                        createEventEndTodo = settingsStore.currentSettings().planningEventEndTodoEnabled
                    )
                    (replenishedRecurringItems + repairedPlanningItems).forEach { item ->
                        if (item.completed || item.canceled) return@forEach
                        ReminderDispatchTracker.clear(applicationContext, item.id)
                        val scheduleMessage = alarmScheduler.schedule(item)
                        if (scheduleMessage != null) {
                            repository.updateTodo(item.copy(reminderEnabled = false))
                            alarmScheduler.cancel(item.id)
                        }
                    }
                }
                LegacyAiReportMigration.migrateIfNeeded(this@TodoApplication)
                runCatching { DailyReportScheduler.recoverMostRecentMissedReports(this@TodoApplication) }
                    .onFailure(CrashLogger::recordNonFatal)
                runCatching { DailyReportScheduler.scheduleNext(applicationContext) }
                    .onFailure(CrashLogger::recordNonFatal)
                if (settingsStore.currentSettings().desktopSyncEnabled && settingsStore.hasValidRemoteSession()) {
                    DesktopSyncService.start(applicationContext)
                }
                delay(5_000L)
                EventCheckInWatchdog.runOnce(applicationContext)
            } catch (exception: Exception) {
                CrashLogger.recordNonFatal(exception)
            } finally {
                SafeStartupGuard.markStartupSuccessful(applicationContext)
            }
        }
        applicationScope.launch {
            while (true) {
                try { com.example.todoalarm.alarm.AiReportExpiryJobService.monitor(this@TodoApplication) }
                catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                catch (error: Exception) { CrashLogger.recordNonFatal(error); delay(60_000) }
            }
        }
        remoteSyncCoordinator.start()
        calendarCollaboration.start()
        com.example.todoalarm.sync.AccountExpiryNotifier.schedule(this)
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
            DatabaseMigrations.MIGRATION_22_23,
            DatabaseMigrations.MIGRATION_23_24,
            DatabaseMigrations.MIGRATION_24_25,
            DatabaseMigrations.MIGRATION_25_26,
            DatabaseMigrations.MIGRATION_26_27,
            DatabaseMigrations.MIGRATION_27_28,
            DatabaseMigrations.MIGRATION_28_29,
            DatabaseMigrations.MIGRATION_29_30,
            DatabaseMigrations.MIGRATION_30_31,
            DatabaseMigrations.MIGRATION_31_32,
            DatabaseMigrations.MIGRATION_32_33,
            DatabaseMigrations.MIGRATION_33_34
        )
            .build()
    }

    val repository: TodoRepository by lazy {
        TodoRepository(database) {
            // Room writes are initiated by the UI coroutine. Do not make a local
            // edit wait for widget IPC, SharedPreferences commits, or network
            // scheduling. Those are follow-up effects and belong off the caller's
            // thread; the database transaction itself remains synchronous.
            applicationScope.launch {
                remoteSyncCoordinator.onLocalDataChanged()
            }
            applicationScope.launch {
                calendarCollaboration.onLocalDataChanged()
            }
            applicationScope.launch {
                TodoWidgetProvider.notifyWidgetDataChanged(applicationContext)
            }
        }
    }

    val settingsStore: AppSettingsStore by lazy {
        AppSettingsStore(applicationContext)
    }

    val backupManager: BackupManager by lazy {
        BackupManager(applicationContext)
    }

    val localAutoBackup: com.example.todoalarm.data.LocalAutoBackup by lazy {
        com.example.todoalarm.data.LocalAutoBackup(this)
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

    val remoteSyncCoordinator: RemoteSyncCoordinator by lazy {
        RemoteSyncCoordinator(this, settingsStore)
    }

    val calendarCollaboration: com.example.todoalarm.sync.CalendarCollaborationCoordinator by lazy {
        com.example.todoalarm.sync.CalendarCollaborationCoordinator(this)
    }

    val alarmScheduler: AlarmScheduler by lazy {
        AlarmScheduler(applicationContext)
    }

    val reminderNotifier: ReminderNotifier by lazy {
        ReminderNotifier(applicationContext)
    }

    fun activatePlanningAiProvider(provider: PlanningAiProvider) {
        if (!settingsStore.activatePlanningAiProvider(provider)) return
        PlanningAiSwitchNotifier.post(applicationContext, provider)
        remoteSyncCoordinator.onLocalDataChanged()
    }
}
