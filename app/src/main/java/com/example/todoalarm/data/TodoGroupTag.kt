package com.example.todoalarm.data

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "todo_group_tags",
    primaryKeys = ["todoId", "groupId"],
    indices = [
        Index(value = ["todoId"], name = "index_todo_group_tags_todoId"),
        Index(value = ["groupId"], name = "index_todo_group_tags_groupId")
    ]
)
data class TodoGroupTag(
    val todoId: Long,
    val groupId: Long
)
