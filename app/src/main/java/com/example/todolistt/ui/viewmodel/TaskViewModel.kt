package com.example.todolistt.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.todolistt.data.local.Category
import com.example.todolistt.data.local.Priority
import com.example.todolistt.data.local.RecurrenceType
import com.example.todolistt.data.local.Task
import com.example.todolistt.data.local.TaskStatus
import com.example.todolistt.data.repository.CategoryRepository
import com.example.todolistt.data.repository.TaskRepository
import com.example.todolistt.data.repository.ThemeRepository
import com.example.todolistt.util.ReminderManager
import android.app.Application
import androidx.lifecycle.AndroidViewModel
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
    application: Application,
    private val repository: TaskRepository,
    private val categoryRepository: CategoryRepository,
    private val themeRepository: ThemeRepository
) : AndroidViewModel(application) {

    private val context get() = getApplication<Application>()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery
    
    private val _selectedCategories = MutableStateFlow<Set<String>>(emptySet())
    val selectedCategories: StateFlow<Set<String>> = _selectedCategories

    private val _selectedMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH))
    val selectedMonth: StateFlow<Int> = _selectedMonth

    private val _selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val selectedYear: StateFlow<Int> = _selectedYear

    private val _selectedStatus = MutableStateFlow<TaskStatus?>(null)

    val isDarkMode: StateFlow<Boolean> = themeRepository.isDarkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val tasks: StateFlow<List<Task>> = combine(
        repository.allTasks,
        _searchQuery,
        _selectedCategories,
        _selectedStatus,
        _selectedMonth,
        _selectedYear
    ) { params: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val tasks = params[0] as List<Task>
        val query = params[1] as String
        @Suppress("UNCHECKED_CAST")
        val categories = params[2] as Set<String>
        val status = params[3] as TaskStatus?
        val month = params[4] as Int
        val year = params[5] as Int

        val calendar = Calendar.getInstance()
        tasks.filter { task ->
            if (task.isArchived) return@filter false
            
            val matchesQuery = query.isEmpty() || 
                              task.title.contains(query, ignoreCase = true) || 
                              (task.subcategory?.contains(query, ignoreCase = true) ?: false)
            if (!matchesQuery) return@filter false

            if (categories.isNotEmpty() && !categories.contains(task.category)) return@filter false

            if (status != null && task.status != status) return@filter false

            calendar.timeInMillis = task.createdAt
            val taskMonth = calendar.get(Calendar.MONTH)
            val taskYear = calendar.get(Calendar.YEAR)
            if (taskMonth != month || taskYear != year) return@filter false

            true
        }.sortedWith(
            compareBy<Task> { it.status == TaskStatus.COMPLETED }
            .thenByDescending { it.priority }
            .thenByDescending { it.createdAt }
        )
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
            if (_selectedCategories.value.contains(categoryName)) {
                _selectedCategories.value -= categoryName
            }
            val allTasks = repository.allTasks.first()
            allTasks.filter { it.category == categoryName }.forEach {
                repository.update(it.copy(category = "Others"))
            }
        }
    }

    val stats: StateFlow<TaskStats> = combine(
        repository.allTasks,
        _selectedCategories,
        _selectedMonth,
        _selectedYear
    ) { tasks, categories, month, year ->
        val activeTasks = tasks.filter { 
            !it.isArchived && (categories.isEmpty() || categories.contains(it.category)) 
        }
        calculateStats(activeTasks, month, year)
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

        val categoryDistribution = filteredTasks.filter { it.status != TaskStatus.COMPLETED }
            .groupBy { it.category }
            .mapValues { it.value.size }

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
            val updatedTask = task.copy(
                status = newStatus,
                isCompleted = newStatus == TaskStatus.COMPLETED
            )
            repository.update(updatedTask)
            
            if (newStatus == TaskStatus.COMPLETED) {
                ReminderManager.cancelReminder(context, task)
                // Handle Recurring Tasks
                if (task.recurrenceType != RecurrenceType.NONE) {
                    handleRecurringTask(task)
                }
            } else {
                ReminderManager.scheduleReminder(context, updatedTask)
            }
        }
    }

    private suspend fun handleRecurringTask(task: Task) {
        val calendar = Calendar.getInstance()
        task.targetDate?.let { calendar.timeInMillis = it }
        
        when (task.recurrenceType) {
            RecurrenceType.DAILY -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            RecurrenceType.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            RecurrenceType.MONTHLY -> calendar.add(Calendar.MONTH, 1)
            else -> return
        }

        val nextTask = task.copy(
            id = 0,
            status = TaskStatus.PENDING,
            isCompleted = false,
            targetDate = calendar.timeInMillis,
            createdAt = System.currentTimeMillis(),
            parentTaskId = task.parentTaskId ?: task.id
        )
        val newId = repository.insert(nextTask).toInt()
        ReminderManager.scheduleReminder(context, nextTask.copy(id = newId))
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
        endTime: Long? = null,
        recurrenceType: RecurrenceType = RecurrenceType.NONE
    ) {
        viewModelScope.launch {
            val defaults = listOf("Personal", "Work", "Meeting", "Others")
            if (!defaults.contains(category)) {
                categoryRepository.insert(Category(category))
            }
            
            val task = Task(
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
                endTime = endTime,
                recurrenceType = recurrenceType
            )
            val id = repository.insert(task).toInt()
            ReminderManager.scheduleReminder(context, task.copy(id = id))
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            repository.update(task)
            if (task.status == TaskStatus.PENDING) {
                ReminderManager.scheduleReminder(context, task)
            } else {
                ReminderManager.cancelReminder(context, task)
            }
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
            ReminderManager.cancelReminder(context, task)
        }
    }

    fun archiveTask(task: Task) {
        viewModelScope.launch {
            repository.update(task.copy(isArchived = true))
        }
    }

    val archivedTasks: StateFlow<List<Task>> = repository.allTasks
        .combine(_searchQuery) { tasks, query ->
            tasks.filter { task ->
                task.isArchived &&
                (query.isEmpty() || task.title.contains(query, ignoreCase = true))
            }.sortedByDescending { it.createdAt }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun restoreTask(task: Task) {
        viewModelScope.launch {
            repository.update(task.copy(isArchived = false))
        }
    }

    fun deleteArchivedTasks() {
        viewModelScope.launch {
            val all = repository.allTasks.first()
            all.filter { it.isArchived }.forEach {
                repository.delete(it)
            }
        }
    }

    fun toggleCategoryFilter(category: String) {
        val current = _selectedCategories.value
        if (current.contains(category)) {
            _selectedCategories.value = current - category
        } else {
            _selectedCategories.value = current + category
        }
    }

    fun clearCategoryFilters() {
        _selectedCategories.value = emptySet()
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
    private val application: Application,
    private val repository: TaskRepository,
    private val categoryRepository: CategoryRepository,
    private val themeRepository: ThemeRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(application, repository, categoryRepository, themeRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}