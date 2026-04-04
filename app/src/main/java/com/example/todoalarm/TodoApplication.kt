package com.example.todoalarm

import android.app.Application
import androidx.room.Room
import com.example.todoalarm.alarm.AlarmScheduler
import com.example.todoalarm.alarm.ReminderNotifier
import com.example.todoalarm.data.AppDatabase
import com.example.todoalarm.data.AppSettingsStore
import com.example.todoalarm.data.TodoRepository

class TodoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLogger.install(this)
    }

    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "todo-alarm.db"
        ).fallbackToDestructiveMigration()
            .build()
    }

    val repository: TodoRepository by lazy {
        TodoRepository(database.todoDao())
    }

    val settingsStore: AppSettingsStore by lazy {
        AppSettingsStore(applicationContext)
    }

    val alarmScheduler: AlarmScheduler by lazy {
        AlarmScheduler(applicationContext)
    }

    val reminderNotifier: ReminderNotifier by lazy {
        ReminderNotifier(applicationContext)
    }
}
