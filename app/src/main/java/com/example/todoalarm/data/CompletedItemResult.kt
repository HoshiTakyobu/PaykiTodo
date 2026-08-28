package com.example.todoalarm.data

data class CompletedItemResult(
    val item: TodoItem,
    val affectedItems: List<TodoItem> = listOf(item)
)
