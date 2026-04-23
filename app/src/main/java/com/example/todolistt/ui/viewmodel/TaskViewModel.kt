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
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import com.example.todolistt.widget.TaskWidgetProvider
import android.content.Intent
import android.widget.Toast
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
    
    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    private val _selectedMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH))
    val selectedMonth: StateFlow<Int> = _selectedMonth

    private val _selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val selectedYear: StateFlow<Int> = _selectedYear

    private val _selectedStatus = MutableStateFlow<TaskStatus?>(null)
    val selectedStatus: StateFlow<TaskStatus?> = _selectedStatus

    private val _selectedRecurrence = MutableStateFlow<RecurrenceType?>(null)
    val selectedRecurrence: StateFlow<RecurrenceType?> = _selectedRecurrence

    val isDarkMode: StateFlow<Boolean> = themeRepository.isDarkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val tasks: StateFlow<List<Task>> = combine(
        repository.allTasks,
        _searchQuery,
        _selectedCategory,
        _selectedStatus,
        _selectedRecurrence,
        _selectedMonth,
        _selectedYear
    ) { params: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val tasks = params[0] as List<Task>
        val query = params[1] as String
        val category = params[2] as String?
        val status = params[3] as TaskStatus?
        val recurrence = params[4] as RecurrenceType?
        val month = params[5] as Int
        val year = params[6] as Int

        val calendar = Calendar.getInstance()
        tasks.filter { task ->
            if (task.isArchived) return@filter false
            
            val matchesQuery = query.isEmpty() || 
                              task.title.contains(query, ignoreCase = true) || 
                              (task.subcategory?.contains(query, ignoreCase = true) ?: false)
            if (!matchesQuery) return@filter false

            if (category != null && task.category != category) return@filter false

            if (status != null && task.status != status) return@filter false
            
            if (recurrence != null && task.recurrenceType != recurrence) return@filter false

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
            // Check if any tasks are using this category before deleting
            val all = repository.allTasks.first()
            val isUsed = all.any { it.category == categoryName }
            if (isUsed) {
                // Show Toast message explaining why it can't be deleted
                Toast.makeText(
                    context, 
                    "Cannot delete category '$categoryName' while it has tasks assigned to it.", 
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }
            
            categoryRepository.delete(categoryName)
            if (_selectedCategory.value == categoryName) {
                _selectedCategory.value = null
            }
        }
    }

    val stats: StateFlow<TaskStats> = combine(
        repository.allTasks,
        _selectedCategory,
        _selectedMonth,
        _selectedYear
    ) { tasks, category, month, year ->
        val activeTasks = tasks.filter { 
            !it.isArchived && (category == null || it.category == category) 
        }
        calculateStats(tasks, activeTasks, month, year)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TaskStats()
    )

    fun setDateFilter(month: Int, year: Int) {
        _selectedMonth.value = month
        _selectedYear.value = year
    }

    private fun calculateStats(allTasks: List<Task>, filteredTasks: List<Task>, filterMonth: Int, filterYear: Int): TaskStats {
        val calendar = Calendar.getInstance()
        
        val tasksInMonth = filteredTasks.filter { task ->
            calendar.timeInMillis = task.createdAt
            val taskMonth = calendar.get(Calendar.MONTH)
            val taskYear = calendar.get(Calendar.YEAR)
            taskMonth == filterMonth && taskYear == filterYear
        }

        val completed = tasksInMonth.count { it.status == TaskStatus.COMPLETED }
        val pending = tasksInMonth.count { it.status == TaskStatus.PENDING }
        val total = tasksInMonth.size
        val completionRate = if (total > 0) completed.toFloat() / total else 0f
        
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        val completedToday = tasksInMonth.count { 
            it.status == TaskStatus.COMPLETED && it.createdAt >= today.timeInMillis 
        }

        // Updated Category Stats (Pending count per category)
        val categoryDistribution = tasksInMonth.filter { it.status == TaskStatus.PENDING }
            .groupBy { it.category }
            .mapValues { it.value.size }

        // Recurrence Stats
        val recurrenceStats = tasksInMonth.filter { it.status == TaskStatus.PENDING && it.recurrenceType != RecurrenceType.NONE }
            .groupBy { it.recurrenceType }
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
            
            val count = allTasks.count { 
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
            dailyCompletion = dailyCompletion,
            recurrenceDistribution = recurrenceStats
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
            updateWidget()
        }
    }

    private suspend fun handleRecurringTask(task: Task) {
        val calendar = Calendar.getInstance()
        task.targetDate?.let { calendar.timeInMillis = it }
        
        when (task.recurrenceType) {
            RecurrenceType.DAILY -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            RecurrenceType.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            RecurrenceType.MONTHLY -> calendar.add(Calendar.MONTH, 1)
            RecurrenceType.YEARLY -> calendar.add(Calendar.YEAR, 1)
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
        updateWidget()
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
            updateWidget()
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
            updateWidget()
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            val newStatus = if (task.status == TaskStatus.COMPLETED) TaskStatus.PENDING else TaskStatus.COMPLETED
            repository.update(task.copy(
                status = newStatus,
                isCompleted = newStatus == TaskStatus.COMPLETED
            ))
            updateWidget()
        }
    }

    fun deleteTask(task: Task, deleteFuture: Boolean = false) {
        viewModelScope.launch {
            if (deleteFuture && task.recurrenceType != RecurrenceType.NONE) {
                val parentId = task.parentTaskId ?: task.id
                val allTasks = repository.allTasks.first()
                allTasks.filter { it.id == parentId || it.parentTaskId == parentId }.forEach {
                    repository.delete(it)
                    ReminderManager.cancelReminder(context, it)
                }
            } else {
                repository.delete(task)
                ReminderManager.cancelReminder(context, task)
            }
            updateWidget()
        }
    }

    private fun updateWidget() {
        val intent = Intent(context, TaskWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
        val ids = AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, TaskWidgetProvider::class.java))
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        context.sendBroadcast(intent)
    }

    fun archiveTask(task: Task) {
        viewModelScope.launch {
            repository.update(task.copy(isArchived = true))
            updateWidget()
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
            updateWidget()
        }
    }

    fun deleteArchivedTasks() {
        viewModelScope.launch {
            val all = repository.allTasks.first()
            all.filter { it.isArchived }.forEach {
                repository.delete(it)
                ReminderManager.cancelReminder(context, it)
            }
            updateWidget()
        }
    }

    fun setCategoryFilter(category: String?) {
        _selectedCategory.value = category
    }

    fun clearCategoryFilters() {
        _selectedCategory.value = null
    }

    fun deleteCompletedTasks() {
        viewModelScope.launch {
            val allTasks = repository.allTasks.first()
            allTasks.filter { it.status == TaskStatus.COMPLETED }.forEach {
                repository.delete(it)
            }
            updateWidget()
        }
    }

    fun deleteAllTasks() {
        viewModelScope.launch {
            val allTasks = repository.allTasks.first()
            allTasks.forEach { 
                repository.delete(it)
                ReminderManager.cancelReminder(context, it)
            }
            updateWidget()
        }
    }

    fun clearAllFilters() {
        _selectedStatus.value = null
        _selectedRecurrence.value = null
        _selectedCategory.value = null
        val now = Calendar.getInstance()
        _selectedMonth.value = now.get(Calendar.MONTH)
        _selectedYear.value = now.get(Calendar.YEAR)
    }

    fun setStatusFilter(status: TaskStatus?) {
        _selectedStatus.value = status
    }

    fun setRecurrenceFilter(recurrence: RecurrenceType?) {
        _selectedRecurrence.value = recurrence
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