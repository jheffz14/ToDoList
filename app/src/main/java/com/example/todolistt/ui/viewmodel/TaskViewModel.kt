package com.example.todolistt.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.todolistt.data.local.Category
import com.example.todolistt.data.local.Priority
import com.example.todolistt.data.local.Task
import com.example.todolistt.data.local.TaskStatus
import com.example.todolistt.data.repository.CategoryRepository
import com.example.todolistt.data.repository.TaskRepository
import com.example.todolistt.data.repository.ThemeRepository
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskViewModel(
    private val repository: TaskRepository,
    private val categoryRepository: CategoryRepository,
    private val themeRepository: ThemeRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery
    
    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    private val _selectedMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH))
    private val _selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))

    private val _selectedStatus = MutableStateFlow<TaskStatus?>(null)

    val isDarkMode: StateFlow<Boolean> = themeRepository.isDarkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val tasks: StateFlow<List<Task>> = combine(
        repository.allTasks,
        _searchQuery,
        _selectedCategory,
        _selectedStatus
    ) { tasks, query, category, status ->
        tasks.filter { task ->
            !task.isArchived &&
            (query.isEmpty() || task.title.contains(query, ignoreCase = true) || (task.subcategory?.contains(query, ignoreCase = true) ?: false)) &&
            (category == null || task.category == category) &&
            (status == null || task.status == status)
        }.sortedWith(compareByDescending<Task> { it.priority }.thenByDescending { it.createdAt })
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val categories: StateFlow<List<String>> = categoryRepository.allCategories
        .combine(repository.allTasks) { dbCategories, tasks ->
            val defaults = listOf("Personal", "Work", "Meeting", "Others")
            (defaults + dbCategories.map { it.name } + tasks.map { it.category }).distinct().filter { it.isNotBlank() }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = listOf("Personal", "Work", "Meeting", "Others")
        )

    fun toggleTheme() {
        viewModelScope.launch {
            themeRepository.setDarkMode(!isDarkMode.value)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addCategory(name: String) {
        viewModelScope.launch {
            categoryRepository.insert(Category(name))
        }
    }

    fun deleteCategory(categoryName: String) {
        viewModelScope.launch {
            categoryRepository.delete(categoryName)
            // Reset filter if the deleted category was selected
            if (_selectedCategory.value == categoryName) {
                _selectedCategory.value = null
            }
            // Reassign tasks
            val allTasks = repository.allTasks.first()
            allTasks.filter { it.category == categoryName }.forEach {
                repository.update(it.copy(category = "Others"))
            }
        }
    }

    val stats: StateFlow<TaskStats> = combine(
        repository.allTasks,
        _selectedCategory,
        _selectedMonth,
        _selectedYear
    ) { tasks, _, month, year ->
        calculateStats(tasks, month, year)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TaskStats()
    )

    fun setDateFilter(month: Int, year: Int) {
        _selectedMonth.value = month
        _selectedYear.value = year
    }

    private fun calculateStats(tasks: List<Task>, filterMonth: Int, filterYear: Int): TaskStats {
        val calendar = Calendar.getInstance()
        
        val filteredTasks = tasks.filter { task ->
            calendar.timeInMillis = task.createdAt
            val taskMonth = calendar.get(Calendar.MONTH)
            val taskYear = calendar.get(Calendar.YEAR)
            taskMonth == filterMonth && taskYear == filterYear
        }

        val completed = filteredTasks.count { it.status == TaskStatus.COMPLETED }
        val pending = filteredTasks.count { it.status == TaskStatus.PENDING }
        val total = filteredTasks.size
        val completionRate = if (total > 0) completed.toFloat() / total else 0f
        
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        val completedToday = filteredTasks.count { 
            it.status == TaskStatus.COMPLETED && it.createdAt >= today.timeInMillis 
        }

        // Category Distribution (for open tasks)
        val categoryDistribution = filteredTasks.filter { it.status != TaskStatus.COMPLETED }
            .groupBy { it.category }
            .mapValues { it.value.size }

        // Daily Completion for the last 7 days
        val dateFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val dailyCompletion = (0..6).reversed().map { daysAgo ->
            val date = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -daysAgo)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val nextDate = (date.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
            
            val count = tasks.count { 
                it.status == TaskStatus.COMPLETED && 
                it.createdAt >= date.timeInMillis && 
                it.createdAt < nextDate.timeInMillis 
            }
            dateFormat.format(date.time) to count
        }

        return TaskStats(
            totalTasks = total,
            completedTasks = completed,
            pendingTasks = pending,
            ongoingTasks = 0,
            completionRate = completionRate,
            completedToday = completedToday,
            categoryDistribution = categoryDistribution,
            dailyCompletion = dailyCompletion
        )
    }

    fun updateTaskStatus(task: Task, newStatus: TaskStatus) {
        viewModelScope.launch {
            repository.update(task.copy(
                status = newStatus,
                isCompleted = newStatus == TaskStatus.COMPLETED
            ))
        }
    }

    fun addTask(
        title: String,
        description: String,
        category: String = "Personal",
        subcategory: String? = null,
        priority: Priority = Priority.MEDIUM,
        targetDate: Long? = null,
        targetEndDate: Long? = null,
        targetTime: Long? = null,
        startTime: Long? = null,
        endTime: Long? = null
    ) {
        viewModelScope.launch {
            // Also ensure category exists in DB if it's new
            val defaults = listOf("Personal", "Work", "Meeting", "Others")
            if (!defaults.contains(category)) {
                categoryRepository.insert(Category(category))
            }
            
            repository.insert(Task(
                title = title,
                description = description,
                category = category,
                subcategory = subcategory,
                priority = priority,
                status = TaskStatus.PENDING,
                targetDate = targetDate,
                targetEndDate = targetEndDate,
                targetTime = targetTime,
                startTime = startTime,
                endTime = endTime
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

    fun archiveTask(task: Task) {
        viewModelScope.launch {
            repository.update(task.copy(isArchived = true))
        }
    }

    fun setCategoryFilter(category: String?) {
        _selectedCategory.value = category
    }

    fun deleteCompletedTasks() {
        viewModelScope.launch {
            val allTasks = repository.allTasks.first()
            allTasks.filter { it.status == TaskStatus.COMPLETED }.forEach {
                repository.delete(it)
            }
        }
    }

    fun deleteAllTasks() {
        viewModelScope.launch {
            val allTasks = repository.allTasks.first()
            allTasks.forEach { repository.delete(it) }
        }
    }

    fun setStatusFilter(status: TaskStatus?) {
        _selectedStatus.value = status
    }
}

class TaskViewModelFactory(
    private val repository: TaskRepository,
    private val categoryRepository: CategoryRepository,
    private val themeRepository: ThemeRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(repository, categoryRepository, themeRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
