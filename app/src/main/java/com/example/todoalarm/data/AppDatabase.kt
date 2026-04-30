package com.example.todoalarm.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        TodoItem::class,
        TaskGroup::class,
        RecurringTaskTemplate::class,
        ReminderChainLog::class,
        ScheduleTemplate::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun todoDao(): TodoDao
}
