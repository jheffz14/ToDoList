package com.example.todolistt.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.todolistt.data.local.Priority
import com.example.todolistt.data.local.Task
import com.example.todolistt.data.local.TaskStatus
import com.example.todolistt.data.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskViewModel(private val repository: TaskRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow<String?>(null)
    private val _selectedStatus = MutableStateFlow<TaskStatus?>(null)

    val tasks: StateFlow<List<Task>> = combine(
        repository.allTasks,
        _searchQuery,
        _selectedCategory,
        _selectedStatus
    ) { tasks, query, category, status ->
        tasks.filter { task ->
            (query.isEmpty() || task.title.contains(query, ignoreCase = true)) &&
            (category == null || task.category == category) &&
            (status == null || task.status == status)
        }.sortedWith(compareByDescending<Task> { it.priority }.thenByDescending { it.createdAt })
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun addTask(
        title: String,
        description: String,
        category: String = "Personal",
        subcategory: String? = null,
        priority: Priority = Priority.MEDIUM,
        targetDate: Long? = null,
        targetTime: Long? = null
    ) {
        viewModelScope.launch {
            repository.insert(Task(
                title = title,
                description = description,
                category = category,
                subcategory = subcategory,
                priority = priority,
                status = TaskStatus.PENDING,
                targetDate = targetDate,
                targetTime = targetTime
            ))
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            repository.update(task)
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            val newStatus = if (task.status == TaskStatus.COMPLETED) TaskStatus.PENDING else TaskStatus.COMPLETED
            repository.update(task.copy(
                status = newStatus,
                isCompleted = newStatus == TaskStatus.COMPLETED
            ))
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.delete(task)
        }
    }

    fun setCategoryFilter(category: String?) {
        _selectedCategory.value = category
    }

    fun setStatusFilter(status: TaskStatus?) {
        _selectedStatus.value = status
    }
}

class TaskViewModelFactory(private val repository: TaskRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
