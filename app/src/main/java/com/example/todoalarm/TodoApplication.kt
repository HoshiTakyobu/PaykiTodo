package com.example.todoalarm

import android.app.Application
import androidx.room.Room
import com.example.todoalarm.alarm.AlarmScheduler
import com.example.todoalarm.data.AppDatabase
import com.example.todoalarm.data.TodoRepository

class TodoApplication : Application() {
    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "todo-alarm.db"
        ).build()
    }

    val repository: TodoRepository by lazy {
        TodoRepository(database.todoDao())
    }

    val alarmScheduler: AlarmScheduler by lazy {
        AlarmScheduler(applicationContext)
    }
}

