package com.example.todolistt.data.repository

import com.example.todolistt.data.local.Task
import com.example.todolistt.data.local.TaskDao
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {
    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()

    suspend fun insert(task: Task): Long {
        return taskDao.insertTask(task)
    }

    suspend fun update(task: Task) {
        taskDao.updateTask(task)
    }

    suspend fun updateCategory(oldName: String, newName: String) {
        taskDao.updateTasksCategory(oldName, newName)
    }

    suspend fun delete(task: Task) {
        taskDao.deleteTask(task)
    }
}
