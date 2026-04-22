package com.example.todolistt.ui.viewmodel

data class TaskStats(
    val totalTasks: Int = 0,
    val completedTasks: Int = 0,
    val completionRate: Float = 0f,
    val completedToday: Int = 0
)
