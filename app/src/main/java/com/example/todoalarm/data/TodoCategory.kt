package com.example.todoalarm.data

enum class TodoCategory(
    val key: String,
    val label: String
) {
    IMPORTANT("important", "重要"),
    URGENT("urgent", "紧急"),
    FOCUS("focus", "专注"),
    ROUTINE("routine", "例行");

    companion object {
        fun fromKey(key: String): TodoCategory {
            return entries.firstOrNull { it.key == key } ?: ROUTINE
        }
    }
}
