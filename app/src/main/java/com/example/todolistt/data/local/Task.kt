package com.example.todolistt.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class Priority {
    LOW, MEDIUM, HIGH
}

enum class TaskStatus {
    PENDING, ONGOING, COMPLETED, INCOMING
}

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String,
    val category: String = "Personal",
    val subcategory: String? = null,
    val priority: Priority = Priority.MEDIUM,
    val status: TaskStatus = TaskStatus.PENDING,
    val targetDate: Long? = null,
    val targetTime: Long? = null, // Store as milliseconds from start of day
    val audioPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false // Keep for backward compatibility or use status instead
)
