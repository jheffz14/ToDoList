package com.example.todolistt.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.outlined.History
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todolistt.data.local.Priority
import com.example.todolistt.data.local.RecurrenceType
import com.example.todolistt.data.local.Task
import com.example.todolistt.data.local.TaskStatus
import com.example.todolistt.ui.theme.SketchError
import com.example.todolistt.ui.theme.SketchPrimary
import com.example.todolistt.ui.viewmodel.TaskViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun TaskScreen(
    viewModel: TaskViewModel, 
    onNavigateToDashboard: () -> Unit,
    onNavigateToArchive: () -> Unit,
    initialTaskId: Int? = null
) {
    val tasks by viewModel.tasks.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()
    val selectedStatus by viewModel.selectedStatus.collectAsState()
    val selectedRecurrence by viewModel.selectedRecurrence.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    var showManageCategoriesGlobal by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    var showDeleteRecurrenceDialog by remember { mutableStateOf<Task?>(null) }
    
    val selectedTasks = remember { mutableStateListOf<Int>() }
    var isSelectionMode by remember { mutableStateOf(false) }

    // Handle opening task from widget
    LaunchedEffect(initialTaskId) {
        if (initialTaskId != null) {
            val task = tasks.find { it.id == initialTaskId }
            if (task != null) {
                taskToEdit = task
                showDialog = true
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.background).padding(top = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSelectionMode) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { 
                                isSelectionMode = false
                                selectedTasks.clear()
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel")
                            }
                            Text(
                                text = "${selectedTasks.size} Selected",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        IconButton(onClick = {
                            showClearConfirmDialog = "Delete selected tasks?" to {
                                tasks.filter { it.id in selectedTasks }.forEach { viewModel.deleteTask(it) }
                                isSelectionMode = false
                                selectedTasks.clear()
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Selected", tint = Color.Red)
                        }
                    } else {
                        Text(
                            text = "MY TASKS",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 2.sp
                            )
                        )
                        Row {
                            IconButton(onClick = onNavigateToArchive) {
                            Icon(Icons.Default.Archive, contentDescription = "View Archive")
                        }
                        IconButton(onClick = {
                                showClearConfirmDialog = "Clear all tasks?" to { viewModel.deleteAllTasks() }
                            }) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Data")
                            }
                        }
                    }
                }

                // Global Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    placeholder = { Text("Search tasks or categories...", fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(20.dp))
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                )

                // Improved Filter UI
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        // Month Selector Dropdown
                        var showMonthMenu by remember { mutableStateOf(false) }
                        Box {
                            FilterChip(
                                selected = true,
                                onClick = { showMonthMenu = true },
                                label = { 
                                    val monthName = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(
                                        Calendar.getInstance().apply {
                                            set(Calendar.MONTH, selectedMonth)
                                            set(Calendar.YEAR, selectedYear)
                                        }.time
                                    )
                                    Text(monthName, fontSize = 11.sp) 
                                },
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                shape = RoundedCornerShape(12.dp)
                            )
                            DropdownMenu(expanded = showMonthMenu, onDismissRequest = { showMonthMenu = false }) {
                                // Show range: 12 months back to 24 months forward
                                for (i in -12..24) {
                                    val cal = Calendar.getInstance().apply { add(Calendar.MONTH, i) }
                                    val m = cal.get(Calendar.MONTH)
                                    val y = cal.get(Calendar.YEAR)
                                    val label = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            viewModel.setDateFilter(m, y)
                                            showMonthMenu = false
                                        },
                                        leadingIcon = {
                                            if (selectedMonth == m && selectedYear == y) {
                                                Icon(Icons.Default.Check, contentDescription = null)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        // Status Filter Dropdown
                        var showStatusMenu by remember { mutableStateOf(false) }
                        Box {
                            FilterChip(
                                selected = selectedStatus != null,
                                onClick = { showStatusMenu = true },
                                label = { Text(selectedStatus?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "All Status", fontSize = 11.sp) },
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                shape = RoundedCornerShape(12.dp)
                            )
                            DropdownMenu(expanded = showStatusMenu, onDismissRequest = { showStatusMenu = false }) {
                                DropdownMenuItem(text = { Text("All Status") }, onClick = { 
                                    viewModel.setStatusFilter(null)
                                    showStatusMenu = false
                                })
                                DropdownMenuItem(text = { Text("Pending") }, onClick = { 
                                    viewModel.setStatusFilter(TaskStatus.PENDING)
                                    showStatusMenu = false
                                })
                                DropdownMenuItem(text = { Text("Completed") }, onClick = { 
                                    viewModel.setStatusFilter(TaskStatus.COMPLETED)
                                    showStatusMenu = false
                                })
                            }
                        }
                    }

                    item {
                        // Recurrence Filter Dropdown
                        var showRecurrenceMenu by remember { mutableStateOf(false) }
                        Box {
                            FilterChip(
                                selected = selectedRecurrence != null,
                                onClick = { showRecurrenceMenu = true },
                                label = { Text(selectedRecurrence?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Recurrence", fontSize = 11.sp) },
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                shape = RoundedCornerShape(12.dp)
                            )
                            DropdownMenu(expanded = showRecurrenceMenu, onDismissRequest = { showRecurrenceMenu = false }) {
                                DropdownMenuItem(text = { Text("All Recurrences") }, onClick = { 
                                    viewModel.setRecurrenceFilter(null)
                                    showRecurrenceMenu = false
                                })
                                RecurrenceType.entries.forEach { type ->
                                    if (type != RecurrenceType.NONE) {
                                        DropdownMenuItem(
                                            text = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                            onClick = {
                                                viewModel.setRecurrenceFilter(type)
                                                showRecurrenceMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        // Category Filter Dropdown (Single-select)
                        var showCategoryMenu by remember { mutableStateOf(false) }
                        Box {
                            FilterChip(
                                selected = selectedCategory != null,
                                onClick = { showCategoryMenu = true },
                                label = { 
                                    Text(selectedCategory ?: "Categories", fontSize = 11.sp) 
                                },
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                shape = RoundedCornerShape(12.dp)
                            )
                            DropdownMenu(
                                expanded = showCategoryMenu, 
                                onDismissRequest = { showCategoryMenu = false },
                                modifier = Modifier.widthIn(min = 200.dp)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("All Categories") }, 
                                    onClick = { 
                                        viewModel.setCategoryFilter(null)
                                        showCategoryMenu = false
                                    },
                                    leadingIcon = { if (selectedCategory == null) Icon(Icons.Default.Check, contentDescription = null) }
                                )
                                HorizontalDivider()
                                categories.forEach { category ->
                                    val isSelected = selectedCategory == category
                                    DropdownMenuItem(
                                        text = { Text(category) },
                                        onClick = { 
                                            viewModel.setCategoryFilter(category)
                                            showCategoryMenu = false
                                        },
                                        leadingIcon = {
                                            if (isSelected) Icon(Icons.Default.Check, contentDescription = null)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (selectedStatus != null || selectedRecurrence != null || selectedCategory != null) {
                        item {
                            TextButton(
                                onClick = {
                                    viewModel.clearAllFilters()
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text("Clear", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = true,
                    onClick = { /* Stay here */ },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Tasks") },
                    label = { Text("Tasks") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToDashboard,
                    icon = { Icon(Icons.Outlined.History, contentDescription = "Analytics") },
                    label = { Text("Analytics") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { showManageCategoriesGlobal = true },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { viewModel.toggleTheme() },
                    icon = { Icon(if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode, contentDescription = "Theme") },
                    label = { Text("Theme") }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    taskToEdit = null
                    showDialog = true 
                },
                containerColor = SketchPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.border(2.dp, MaterialTheme.colorScheme.onBackground, RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(tasks, key = { it.id }) { task ->
                    val isSelected = selectedTasks.contains(task.id)
                    SketchTaskItem(
                        task = task,
                        onToggleStatus = { newStatus -> viewModel.updateTaskStatus(task, newStatus) },
                        onDelete = { 
                            if (task.recurrenceType != RecurrenceType.NONE) {
                                showDeleteRecurrenceDialog = task
                            } else {
                                viewModel.deleteTask(task)
                            }
                        },
                        onArchive = { viewModel.archiveTask(task) },
                        onClick = {
                            if (isSelectionMode) {
                                if (isSelected) selectedTasks.remove(task.id) else selectedTasks.add(task.id)
                                if (selectedTasks.isEmpty()) isSelectionMode = false
                            } else {
                                taskToEdit = task
                                showDialog = true
                            }
                        },
                        onLongClick = {
                            isSelectionMode = true
                            selectedTasks.add(task.id)
                        },
                        isSelected = isSelected
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        showClearConfirmDialog?.let { (message, onConfirm) ->
            AlertDialog(
                onDismissRequest = { showClearConfirmDialog = null },
                title = { Text("Confirm Action") },
                text = { Text(message) },
                confirmButton = {
                    Button(onClick = {
                        onConfirm()
                        showClearConfirmDialog = null
                    }) { Text("Clear") }
                },
                dismissButton = {
                    TextButton(onClick = { showClearConfirmDialog = null }) { Text("Cancel") }
                }
            )
        }

        if (showDeleteRecurrenceDialog != null) {
            AlertDialog(
                onDismissRequest = { showDeleteRecurrenceDialog = null },
                title = { Text("Delete Recurring Task") },
                text = { Text("Do you want to delete only this instance or all future instances of this recurring task?") },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteRecurrenceDialog?.let { viewModel.deleteTask(it, deleteFuture = true) }
                            showDeleteRecurrenceDialog = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Delete All", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showDeleteRecurrenceDialog?.let { viewModel.deleteTask(it, deleteFuture = false) }
                        showDeleteRecurrenceDialog = null
                    }) {
                        Text("Only This")
                    }
                }
            )
        }

        if (showManageCategoriesGlobal) {
            ManageCategoriesDialog(
                categories = categories,
                onDismiss = { showManageCategoriesGlobal = false },
                onDelete = { viewModel.deleteCategory(it) }
            )
        }


        val categoriesForDialog by viewModel.categories.collectAsState()

        if (showDialog) {
            TaskDialog(
                task = taskToEdit,
                onDismiss = { showDialog = false },
                onConfirm = { updatedTask ->
                    if (taskToEdit == null) {
                        viewModel.addTask(
                            updatedTask.title,
                            updatedTask.description,
                            updatedTask.category,
                            updatedTask.subcategory,
                            updatedTask.priority,
                            updatedTask.targetDate,
                            updatedTask.targetEndDate,
                            updatedTask.targetTime,
                            updatedTask.startTime,
                            updatedTask.endTime,
                            updatedTask.recurrenceType
                        )
                    } else {
                        viewModel.updateTask(updatedTask)
                    }
                    showDialog = false
                },
                availableCategories = categoriesForDialog,
                onDeleteCategory = { viewModel.deleteCategory(it) }
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SketchTaskItem(
    task: Task,
    onToggleStatus: (TaskStatus) -> Unit,
    onDelete: () -> Unit,
    onArchive: () -> Unit = {},
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    isSelected: Boolean = false
) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }
    val timeFormatter = remember { 
        SimpleDateFormat("h:mm a", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }
    var showStatusMenu by remember { mutableStateOf(false) }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                onArchive()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val color = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.EndToStart -> Color.Gray
                else -> Color.Transparent
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color, RoundedCornerShape(16.dp))
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart || dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                    Icon(
                        Icons.Default.Archive,
                        contentDescription = "Archive",
                        tint = Color.White
                    )
                }
            }
        }
    ) {
        val isNear = remember(task.startTime) {
            task.startTime?.let {
                val now = System.currentTimeMillis()
                val diff = it - now
                diff in 0..1800000 // 30 minutes
            } ?: false
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .border(
                    width = if (isSelected) 3.dp else 2.dp,
                    color = if (isSelected) SketchPrimary else if (isNear && task.status != TaskStatus.COMPLETED) SketchError else MaterialTheme.colorScheme.onBackground,
                    shape = RoundedCornerShape(16.dp)
                ),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) SketchPrimary.copy(alpha = 0.2f)
                                 else if (task.status == TaskStatus.PENDING) SketchPrimary.copy(alpha = 0.1f) 
                                 else if (isNear && task.status != TaskStatus.COMPLETED) SketchError.copy(alpha = 0.05f) 
                                 else MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelected) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Selected", tint = SketchPrimary, modifier = Modifier.padding(end = 12.dp))
                }

                Box {
                    IconButton(onClick = { showStatusMenu = true }) {
                        val icon = when (task.status) {
                            TaskStatus.COMPLETED -> Icons.Default.CheckCircle
                            else -> Icons.Default.RadioButtonUnchecked
                        }
                        Icon(
                            icon,
                            contentDescription = "Toggle status",
                            tint = if (task.status == TaskStatus.COMPLETED) SketchPrimary else if (isNear) SketchError else MaterialTheme.colorScheme.onBackground
                        )
                    }
                    DropdownMenu(expanded = showStatusMenu, onDismissRequest = { showStatusMenu = false }) {
                        listOf(TaskStatus.PENDING, TaskStatus.COMPLETED).forEach { status ->
                            DropdownMenuItem(
                                text = { Text(status.name) },
                                onClick = {
                                    onToggleStatus(status)
                                    showStatusMenu = false
                                }
                            )
                        }
                    }
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                textDecoration = if (task.status == TaskStatus.COMPLETED) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                                color = if (isNear && task.status != TaskStatus.COMPLETED) SketchError else MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        PriorityBadge(task.priority)

                        if (task.recurrenceType != RecurrenceType.NONE) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                Icons.Default.Sync, // Using Sync icon for recurrence visibility
                                contentDescription = "Recurring: ${task.recurrenceType.name}",
                                tint = SketchPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        if (isNear && task.status != TaskStatus.COMPLETED) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                Icons.Default.Warning, 
                                contentDescription = "Near", 
                                tint = SketchError,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    
                    if (task.subcategory != null) {
                        Text(
                            text = "#${task.subcategory}",
                            style = MaterialTheme.typography.labelSmall,
                            color = SketchPrimary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(12.dp))
                        Text(
                            text = " ${task.category}",
                            style = MaterialTheme.typography.labelSmall
                        )
                        task.targetDate?.let { date ->
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(12.dp))
                            val dateStr = if (task.targetEndDate != null && task.targetEndDate != date) {
                                "${dateFormatter.format(Date(date))} - ${dateFormatter.format(Date(task.targetEndDate))}"
                            } else {
                                dateFormatter.format(Date(date))
                            }
                            Text(
                                text = " $dateStr",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    if (task.startTime != null || task.endTime != null) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(12.dp))
                            val startStr = task.startTime?.let { timeFormatter.format(Date(it)) } ?: "??"
                            val endStr = task.endTime?.let { timeFormatter.format(Date(it)) } ?: "??"
                            Text(
                                text = " $startStr - $endStr",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun PriorityBadge(priority: Priority) {
    val color = when (priority) {
        Priority.HIGH -> Color(0xFFFA5252)
        Priority.MEDIUM -> Color(0xFFFAB005)
        Priority.LOW -> Color(0xFF40C057)
    }
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, color)
    ) {
        Text(
            text = priority.name,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
            color = color
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun TaskDialog(
    task: Task? = null,
    onDismiss: () -> Unit,
    onConfirm: (Task) -> Unit,
    onDeleteCategory: ((String) -> Unit)? = null,
    availableCategories: List<String> = listOf("Personal", "Work", "Meeting", "Others")
) {
    var title by remember { mutableStateOf(task?.title ?: "") }
    var description by remember { mutableStateOf(task?.description ?: "") }
    var category by remember { mutableStateOf(task?.category ?: "Personal") }
    var customCategory by remember { mutableStateOf("") }
    var isAddingCustomCategory by remember { mutableStateOf(false) }
    var subcategory by remember { mutableStateOf(task?.subcategory ?: "") }
    var priority by remember { mutableStateOf(task?.priority ?: Priority.MEDIUM) }
    
    var categorySearchQuery by remember { mutableStateOf("") }
    
    var showFromDatePicker by remember { mutableStateOf(false) }
    var showToDatePicker by remember { mutableStateOf(false) }
    
    var targetDate by remember { mutableStateOf(task?.targetDate) }
    var targetEndDate by remember { mutableStateOf(task?.targetEndDate) }
    var recurrenceType by remember { mutableStateOf(task?.recurrenceType ?: RecurrenceType.NONE) }

    var startTime by remember { mutableStateOf(task?.startTime) }
    var endTime by remember { mutableStateOf(task?.endTime) }
    
    var showTimePickerForStart by remember { mutableStateOf(false) }
    var showTimePickerForEnd by remember { mutableStateOf(false) }

    val filteredCategories = remember(categorySearchQuery, availableCategories) {
        if (categorySearchQuery.isBlank()) availableCategories
        else availableCategories.filter { it.contains(categorySearchQuery, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.padding(12.dp).fillMaxWidth(), // Reduced padding
        title = { 
            Text(
                if (task == null) "NEW TASK" else "EDIT TASK", 
                fontWeight = FontWeight.ExtraBold, 
                fontSize = 20.sp // Reduced from 24sp
            ) 
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(vertical = 4.dp), 
                verticalArrangement = Arrangement.spacedBy(10.dp) // Reduced spacing from 16dp
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth().height(56.dp), // Reduced from 64dp
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.bodyMedium // Reduced from bodyLarge
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Category", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    
                    OutlinedTextField(
                        value = categorySearchQuery,
                        onValueChange = { categorySearchQuery = it },
                        placeholder = { Text("Search category...", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth().height(48.dp), // Reduced from 52dp
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = isAddingCustomCategory,
                                onClick = { isAddingCustomCategory = true },
                                label = { Text("+ Custom", fontSize = 10.sp) },
                                modifier = Modifier.height(28.dp)
                            )
                        }
                        items(filteredCategories) { cat ->
                            FilterChip(
                                selected = category == cat && !isAddingCustomCategory,
                                onClick = { 
                                    category = cat
                                    isAddingCustomCategory = false
                                },
                                label = { Text(cat, fontSize = 10.sp) }, // Reduced font
                                modifier = Modifier.height(28.dp) // Reduced height
                            )
                        }
                    }

                    if (isAddingCustomCategory) {
                        OutlinedTextField(
                            value = customCategory,
                            onValueChange = { customCategory = it },
                            label = { Text("Custom Category", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Schedule", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { showFromDatePicker = true },
                                modifier = Modifier.fillMaxWidth().height(44.dp), // Reduced height
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)),
                                contentPadding = PaddingValues(start = 4.dp, end = 24.dp)
                            ) {
                                Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                val dateText = targetDate?.let { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(it)) } ?: "From"
                                Column {
                                    Text("From", fontSize = 7.sp, color = Color.Gray)
                                    Text(dateText, fontSize = 11.sp)
                                }
                            }
                            if (targetDate != null) {
                                IconButton(
                                    onClick = { targetDate = null; targetEndDate = null },
                                    modifier = Modifier.align(Alignment.CenterEnd).size(24.dp).padding(end = 2.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(12.dp), tint = Color.Gray)
                                }
                            }
                        }
                        
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { if (targetDate != null) showToDatePicker = true },
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (targetDate != null) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f) else Color.LightGray.copy(alpha = 0.3f)),
                                enabled = targetDate != null,
                                contentPadding = PaddingValues(start = 4.dp, end = 24.dp)
                            ) {
                                Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                val dateText = targetEndDate?.let { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(it)) } ?: "To"
                                Column {
                                    Text("To", fontSize = 7.sp, color = if (targetDate != null) Color.Gray else Color.LightGray)
                                    Text(dateText, fontSize = 11.sp, color = if (targetDate != null) Color.Unspecified else Color.LightGray)
                                }
                            }
                            if (targetEndDate != null) {
                                IconButton(
                                    onClick = { targetEndDate = null },
                                    modifier = Modifier.align(Alignment.CenterEnd).size(24.dp).padding(end = 2.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(12.dp), tint = Color.Gray)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TimeButtonSmall(
                            label = "Start",
                            time = startTime, 
                            onClick = { showTimePickerForStart = true },
                            onClear = { startTime = null },
                            modifier = Modifier.weight(1f)
                        )
                        TimeButtonSmall(
                            label = "End",
                            time = endTime, 
                            onClick = { showTimePickerForEnd = true },
                            onClear = { endTime = null },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Priority", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Priority.entries.forEach { p ->
                                FilterChip(
                                    selected = priority == p,
                                    onClick = { priority = p },
                                    label = { Text(p.name, fontSize = 9.sp) },
                                    modifier = Modifier.height(26.dp)
                                )
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Recurrence", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        RecurrenceType.entries.forEach { type ->
                            if (type != RecurrenceType.NONE) {
                                FilterChip(
                                    selected = recurrenceType == type,
                                    onClick = { 
                                        recurrenceType = if (recurrenceType == type) RecurrenceType.NONE else type 
                                    },
                                    label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 9.sp) },
                                    modifier = Modifier.height(26.dp)
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = subcategory,
                    onValueChange = { subcategory = it },
                    label = { Text("Notes", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 1,
                    textStyle = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val finalCategory = if (isAddingCustomCategory && customCategory.isNotBlank()) customCategory else category
                        val updated = (task ?: Task(title = title, description = description)).copy(
                            title = title,
                            description = description,
                            category = finalCategory,
                            subcategory = if (subcategory.isBlank()) null else subcategory,
                            priority = priority,
                            targetDate = targetDate,
                            targetEndDate = targetEndDate,
                            startTime = startTime,
                            endTime = endTime,
                            recurrenceType = recurrenceType
                        )
                        onConfirm(updated)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SketchPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(44.dp).fillMaxWidth().padding(horizontal = 12.dp)
            ) {
                Text("SAVE", fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
            ) { 
                Text("CANCEL", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )

    if (showFromDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = targetDate ?: System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showFromDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        targetDate = it
                        if (targetEndDate == null || targetEndDate!! < it) {
                            targetEndDate = it
                        }
                    }
                    showFromDatePicker = false
                }) { Text("OK") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showToDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = targetEndDate ?: targetDate ?: System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showToDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        if (targetDate != null && it >= targetDate!!) {
                            targetEndDate = it
                        }
                    }
                    showToDatePicker = false
                }) { Text("OK") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePickerForStart) {
        SketchTimePickerDialog(
            onDismiss = { showTimePickerForStart = false },
            onTimeSelected = { startTime = it }
        )
    }

    if (showTimePickerForEnd) {
        SketchTimePickerDialog(
            onDismiss = { showTimePickerForEnd = false },
            onTimeSelected = { endTime = it }
        )
    }
}

@Composable
fun ManageCategoriesDialog(
    categories: List<String>,
    onDismiss: () -> Unit,
    onDelete: (String) -> Unit
) {
    val defaults = listOf("Personal", "Work", "Meeting", "Others")
    val customCategories = categories.filter { cat -> 
        defaults.none { it.equals(cat, ignoreCase = true) } 
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("MANAGE CATEGORIES", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                if (customCategories.isEmpty()) {
                    Text("No custom categories added.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
                } else {
                    LazyColumn {
                        items(customCategories) { cat ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(cat, fontWeight = FontWeight.Bold)
                                IconButton(onClick = { onDelete(cat) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                                }
                            }
                            HorizontalDivider(thickness = 0.5.dp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss, 
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.onBackground), 
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SketchPrimary)
            ) {
                Text("CLOSE", color = Color.White)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SketchTimePickerDialog(onDismiss: () -> Unit, onTimeSelected: (Long) -> Unit) {
    val currentTime = Calendar.getInstance()
    val timePickerState = rememberTimePickerState(
        initialHour = currentTime.get(Calendar.HOUR_OF_DAY),
        initialMinute = currentTime.get(Calendar.MINUTE),
        is24Hour = false
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                    set(Calendar.MINUTE, timePickerState.minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                // Use a fixed UTC "midnight" to avoid local timezone shifts
                val utcMidnight = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val localMidnight = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                // Calculate offset from midnight in UTC to avoid local timezone shifts
                val offset = (timePickerState.hour * 3600000L) + (timePickerState.minute * 60000L)
                onTimeSelected(offset)
                onDismiss()
            }) { Text("OK") }
        },
        text = { TimePicker(state = timePickerState) }
    )
}

@Composable
fun TimeButtonSmall(
    label: String,
    time: Long?,
    onClick: () -> Unit,
    onClear: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val formatter = remember { 
        SimpleDateFormat("h:mm a", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }
    Box(modifier = modifier) {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().height(44.dp),
            contentPadding = PaddingValues(start = 4.dp, end = 24.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, if (enabled) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f) else Color.LightGray.copy(alpha = 0.3f)),
            enabled = enabled
        ) {
            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Column {
                Text(label, fontSize = 7.sp, color = if (enabled) Color.Gray else Color.LightGray)
                Text(
                    time?.let { 
                        // Note: Task storage might use full timestamps or offsets, 
                        // ensure consistency with what's passed in.
                        formatter.format(Date(it)) 
                    } ?: "--:--", 
                    fontSize = 11.sp, 
                    color = if (enabled) Color.Unspecified else Color.LightGray
                )
            }
        }
        if (time != null) {
            IconButton(
                onClick = onClear,
                modifier = Modifier.align(Alignment.CenterEnd).size(24.dp).padding(end = 2.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(12.dp), tint = Color.Gray)
            }
        }
    }
}

