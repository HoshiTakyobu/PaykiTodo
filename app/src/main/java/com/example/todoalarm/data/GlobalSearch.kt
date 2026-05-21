package com.example.todoalarm.data

data class GlobalSearchResult(
    val query: String,
    val todos: List<TodoItem>,
    val events: List<TodoItem>,
    val planningNodes: List<PlanningNodeSearchResult>,
    val aiReports: List<AiReport>
) {
    companion object {
        fun empty(query: String = ""): GlobalSearchResult {
            return GlobalSearchResult(
                query = query,
                todos = emptyList(),
                events = emptyList(),
                planningNodes = emptyList(),
                aiReports = emptyList()
            )
        }
    }
}

data class PlanningNodeSearchResult(
    val node: PlanningNode,
    val noteTitle: String
)
