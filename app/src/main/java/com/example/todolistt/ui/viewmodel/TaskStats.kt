package com.example.todolistt.ui.viewmodel

data class TaskStats(
    val totalTasks: Int = 0,
    val completedTasks: Int = 0,
    val pendingTasks: Int = 0,
    val ongoingTasks: Int = 0,
    val completionRate: Float = 0f,
    val completedToday: Int = 0,
    val categoryDistribution: Map<String, Int> = emptyMap(),
    val dailyCompletion: List<Pair<String, Int>> = emptyList()
)
