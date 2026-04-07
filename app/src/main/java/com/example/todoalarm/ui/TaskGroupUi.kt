package com.example.todoalarm.ui

import androidx.compose.ui.graphics.Color
import com.example.todoalarm.data.TaskGroup
import com.example.todoalarm.data.TodoCategory
import com.example.todoalarm.data.TodoItem

data class ResolvedTaskGroup(
    val id: Long,
    val name: String,
    val colorHex: String
)

fun resolveTaskGroup(item: TodoItem, groups: List<TaskGroup>): ResolvedTaskGroup {
    val matched = groups.firstOrNull { it.id == item.groupId }
    if (matched != null) {
        return ResolvedTaskGroup(matched.id, matched.name, matched.colorHex)
    }
    val fallback = when (TodoCategory.fromKey(item.categoryKey)) {
        TodoCategory.IMPORTANT -> ResolvedTaskGroup(0, "重要", "#BF7B4D")
        TodoCategory.URGENT -> ResolvedTaskGroup(0, "紧急", "#FF6B4A")
        TodoCategory.FOCUS -> ResolvedTaskGroup(0, "专注", "#4E87E1")
        TodoCategory.ROUTINE -> ResolvedTaskGroup(0, "例行", "#4CB782")
    }
    return fallback
}

fun resolveTaskGroup(item: TodoItem, group: TaskGroup?): ResolvedTaskGroup {
    return if (group != null) {
        group.toResolvedTaskGroup()
    } else {
        resolveTaskGroup(item, emptyList())
    }
}

fun TaskGroup.toResolvedTaskGroup(): ResolvedTaskGroup = ResolvedTaskGroup(
    id = id,
    name = name,
    colorHex = colorHex
)

fun taskGroupEmoji(group: ResolvedTaskGroup): String = when (group.name) {
    "重要" -> "\u2B50"
    "紧急" -> "\u26A0\uFE0F"
    "专注" -> "\uD83C\uDFAF"
    "例行" -> "\uD83E\uDDFD"
    else -> "\uD83D\uDCC1"
}

fun colorFromHex(hex: String): Color {
    return runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color(0xFF4CB782))
}
