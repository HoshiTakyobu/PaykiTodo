package com.example.todoalarm.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        TodoItem::class,
        TaskGroup::class,
        RecurringTaskTemplate::class,
        ReminderChainLog::class,
        ScheduleTemplate::class,
        PlanningNote::class
    ],
    version = 9,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun todoDao(): TodoDao
}
