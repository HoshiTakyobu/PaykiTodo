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
        PlanningNote::class,
        PlanningLineMapping::class,
        PlanningNode::class,
        AiReport::class,
        EventCheckIn::class,
        TodoGroupTag::class,
        RecurringInstanceSkip::class
    ],
    version = 28,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun todoDao(): TodoDao
}
