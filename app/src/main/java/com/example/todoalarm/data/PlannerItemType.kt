package com.example.todoalarm.data

enum class PlannerItemType {
    TODO,
    EVENT;

    companion object {
        fun fromStorage(value: String?): PlannerItemType {
            return entries.firstOrNull { it.name == value } ?: TODO
        }
    }
}
