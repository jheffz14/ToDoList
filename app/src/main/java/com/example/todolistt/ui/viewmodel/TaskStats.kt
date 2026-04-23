package com.example.todolistt.ui.viewmodel

import com.example.todolistt.data.local.RecurrenceType

data class TaskStats(
    val totalTasks: Int = 0,
    val completedTasks: Int = 0,
    val pendingTasks: Int = 0,
    val ongoingTasks: Int = 0,
    val completionRate: Float = 0f,
    val completedToday: Int = 0,
    val categoryDistribution: Map<String, Int> = emptyMap(),
    val dailyCompletion: List<Pair<String, Int>> = emptyList(),
    val recurrenceDistribution: Map<RecurrenceType, Int> = emptyMap()
)
