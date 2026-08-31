package com.example.todoalarm.sync

import com.example.todoalarm.data.BackupSnapshot

/** A safe-to-display summary; it intentionally contains no credentials or payload text. */
data class RemoteBackupSummary(
    val exportedAtMillis: Long,
    val todoCount: Int,
    val eventCount: Int,
    val groupCount: Int,
    val planningNoteCount: Int,
    val historyCount: Int,
    val activityRecordCount: Int
)

data class RemoteBackupDiff(
    val addedTodos: Int,
    val removedTodos: Int,
    val changedTodos: Int,
    val addedEvents: Int,
    val removedEvents: Int,
    val changedEvents: Int,
    val addedPlanningNotes: Int,
    val removedPlanningNotes: Int,
    val changedPlanningNotes: Int
) {
    val hasChanges: Boolean
        get() = listOf(
            addedTodos, removedTodos, changedTodos,
            addedEvents, removedEvents, changedEvents,
            addedPlanningNotes, removedPlanningNotes, changedPlanningNotes
        ).any { it > 0 }
}

data class RemoteBackupInspection(
    val snapshot: BackupSnapshot,
    val remote: RemoteBackupSummary,
    val local: RemoteBackupSummary,
    val diff: RemoteBackupDiff
)

fun BackupSnapshot.remoteSummary(): RemoteBackupSummary {
    return RemoteBackupSummary(
        exportedAtMillis = exportedAtMillis,
        todoCount = tasks.count { it.isTodo },
        eventCount = tasks.count { it.isEvent },
        groupCount = groups.size,
        planningNoteCount = planningNotes.size,
        historyCount = tasks.count { it.isHistory },
        activityRecordCount = activityRecords.size
    )
}

fun BackupSnapshot.diffAgainst(local: BackupSnapshot): RemoteBackupDiff {
    fun <T> diff(
        remoteItems: List<T>,
        localItems: List<T>,
        id: (T) -> Long,
        changed: (T, T) -> Boolean
    ): IntArray {
        val remoteById = remoteItems.associateBy(id)
        val localById = localItems.associateBy(id)
        val added = remoteById.keys.count { it !in localById }
        val removed = localById.keys.count { it !in remoteById }
        val changedCount = remoteById.keys.intersect(localById.keys).count { key ->
            changed(remoteById.getValue(key), localById.getValue(key))
        }
        return intArrayOf(added, removed, changedCount)
    }

    val todoDiff = diff(
        tasks.filter { it.isTodo },
        local.tasks.filter { it.isTodo },
        id = { it.id },
        changed = { remote, current -> remote != current }
    )
    val eventDiff = diff(
        tasks.filter { it.isEvent },
        local.tasks.filter { it.isEvent },
        id = { it.id },
        changed = { remote, current -> remote != current }
    )
    val noteDiff = diff(
        planningNotes,
        local.planningNotes,
        id = { it.id },
        changed = { remote, current -> remote != current }
    )
    return RemoteBackupDiff(
        addedTodos = todoDiff[0],
        removedTodos = todoDiff[1],
        changedTodos = todoDiff[2],
        addedEvents = eventDiff[0],
        removedEvents = eventDiff[1],
        changedEvents = eventDiff[2],
        addedPlanningNotes = noteDiff[0],
        removedPlanningNotes = noteDiff[1],
        changedPlanningNotes = noteDiff[2]
    )
}
