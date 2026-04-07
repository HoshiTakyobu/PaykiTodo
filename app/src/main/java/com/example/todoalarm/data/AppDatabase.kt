package com.example.todoalarm.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [TodoItem::class, TaskGroup::class, RecurringTaskTemplate::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun todoDao(): TodoDao
}
