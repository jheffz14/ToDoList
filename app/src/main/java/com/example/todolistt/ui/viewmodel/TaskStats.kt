package com.example.todolistt.ui.viewmodel

import com.example.todolistt.data.local.Priority
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
    val recurrenceDistribution: Map<RecurrenceType, Int> = emptyMap(),
    val weeklyComparison: Map<String, List<Int>> = emptyMap(), // Key: "This Week", "Last Week"
    val priorityDistribution: Map<String, Map<Priority, Int>> = emptyMap(), // Category -> (Priority -> Count)
    val hourlyCompletion: Map<Int, Int> = emptyMap() // Hour (0-23) -> Count
)
