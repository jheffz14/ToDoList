@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
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
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todolistt.data.local.Priority
import com.example.todolistt.data.local.RecurrenceType
import com.example.todolistt.data.local.Task
import com.example.todolistt.data.local.TaskStatus
import com.example.todolistt.ui.theme.SketchError
import com.example.todolistt.ui.theme.SketchPrimary
import com.example.todolistt.ui.viewmodel.TaskViewModel
import com.example.todolistt.util.ExportUtility
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TaskScreen(
    viewModel: TaskViewModel,
    onNavigateToTasks: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateToArchive: () -> Unit,
    initialTaskId: Int? = null,
    initialAddTask: Boolean = false,
    voiceTitle: String? = null,
    voicePriority: Priority? = null,
    voiceDate: Long? = null,
    voiceEndDate: Long? = null,
    voiceStartTime: Long? = null,
    voiceEndTime: Long? = null,
    voiceCategory: String? = null,
    voiceRecurrence: RecurrenceType? = null,
    onVoiceComplete: () -> Unit = {},
    onStartVoiceInput: () -> Unit = {}
) {
    val tasks by viewModel.tasks.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()
    val selectedStatus by viewModel.selectedStatus.collectAsState()
    val selectedRecurrence by viewModel.selectedRecurrence.collectAsState()
    val selectedPriority by viewModel.selectedPriority.collectAsState()

    val context = LocalContext.current

    var showHistoryConfirmDialog by remember { mutableStateOf<Task?>(null) }
    var showDialog by remember { mutableStateOf(initialAddTask) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    var showManageCategoriesGlobal by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    var showDeleteRecurrenceDialog by remember { mutableStateOf<Task?>(null) }

    // Update showDialog when voiceTitle is received
    LaunchedEffect(voiceTitle, voicePriority, voiceDate, voiceEndDate, voiceStartTime, voiceEndTime, voiceCategory, voiceRecurrence) {
        if (!voiceTitle.isNullOrBlank()) {
            taskToEdit = null
            showDialog = true
        }
    }

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
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.background).padding(top = 20.dp)) {
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
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 2.sp,
                                fontSize = 24.sp // Slightly smaller to give room for icons
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            var showHelpDialog by remember { mutableStateOf(false) }
                            IconButton(onClick = { showHelpDialog = true }, modifier = Modifier.size(40.dp)) {
                                Icon(Icons.Default.HelpOutline, contentDescription = "Help", modifier = Modifier.size(22.dp))
                            }

                            if (showHelpDialog) {
                                AlertDialog(
                                    onDismissRequest = { showHelpDialog = false },
                                    title = { Text("Voice Commands Guide") },
                                    text = {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("Try saying:", fontWeight = FontWeight.Bold)
                                            Text("• 'Jogging daily now to May 3 at 5 AM to 7 AM'", fontSize = 12.sp)
                                            Text("• 'Meeting Work tomorrow at 9 AM to 10 AM'", fontSize = 12.sp)
                                            Text("• 'Buy groceries high priority today'", fontSize = 12.sp)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Keywords detected:", fontWeight = FontWeight.Bold)
                                            Text("• Recurrence: 'daily', 'every day'", fontSize = 11.sp)
                                            Text("• Range: 'to', 'until' (for time and dates)", fontSize = 11.sp)
                                            Text("• Category: 'Work', 'Personal', 'Meeting'", fontSize = 11.sp)
                                        }
                                    },
                                    confirmButton = { TextButton(onClick = { showHelpDialog = false }) { Text("Close") } }
                                )
                            }
                            IconButton(onClick = onNavigateToArchive, modifier = Modifier.size(40.dp)) {
                                Icon(Icons.Default.Archive, contentDescription = "View Archive", modifier = Modifier.size(22.dp))
                            }
                            var showExportMenu by remember { mutableStateOf(false) }
                            val csvPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                                contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("text/csv")
                            ) { uri ->
                                uri?.let { ExportUtility.exportTasksToCsv(context, tasks, it) }
                            }
                            val pdfPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                                contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/pdf")
                            ) { uri ->
                                uri?.let { ExportUtility.exportTasksToPdf(context, tasks, it) }
                            }

                            Box {
                                IconButton(onClick = { showExportMenu = true }, modifier = Modifier.size(40.dp)) {
                                    Icon(Icons.Default.Share, contentDescription = "Export", modifier = Modifier.size(22.dp))
                                }
                                DropdownMenu(
                                    expanded = showExportMenu,
                                    onDismissRequest = { showExportMenu = false },
                                    modifier = Modifier.widthIn(max = 250.dp)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Export as PDF") },
                                        onClick = {
                                            pdfPickerLauncher.launch("tasks_report_${System.currentTimeMillis()}.pdf")
                                            showExportMenu = false
                                        },
                                        leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Export as CSV") },
                                        onClick = {
                                            csvPickerLauncher.launch("tasks_report_${System.currentTimeMillis()}.csv")
                                            showExportMenu = false
                                        },
                                        leadingIcon = { Icon(Icons.Default.TableChart, contentDescription = null) }
                                    )
                                }
                            }
                            IconButton(onClick = {
                                showClearConfirmDialog = "Clear all tasks?" to { viewModel.deleteAllTasks() }
                            }, modifier = Modifier.size(40.dp)) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Data", modifier = Modifier.size(22.dp), tint = Color.Red)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Global Search Bar (Moved Upper)
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
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
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
                                    Text(monthName, fontSize = 10.sp)
                                },
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(28.dp)
                            )
                            DropdownMenu(
                                expanded = showMonthMenu,
                                onDismissRequest = { showMonthMenu = false },
                                modifier = Modifier.widthIn(min = 150.dp, max = 280.dp).heightIn(max = 400.dp)
                            ) {
                                // Show range: 12 months back to 24 months forward
                                for (i in -12..24) {
                                    val cal = Calendar.getInstance().apply { add(Calendar.MONTH, i) }
                                    val m = cal.get(Calendar.MONTH)
                                    val y = cal.get(Calendar.YEAR)
                                    val label = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
                                    DropdownMenuItem(
                                        text = { Text(label, fontSize = 14.sp) },
                                        onClick = {
                                            viewModel.setDateFilter(m, y)
                                            showMonthMenu = false
                                        },
                                        leadingIcon = {
                                            if (selectedMonth == m && selectedYear == y) {
                                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                    item {
                        // Priority Filter Dropdown
                        var showPriorityMenu by remember { mutableStateOf(false) }
                        Box {
                            FilterChip(
                                selected = selectedPriority != null,
                                onClick = { showPriorityMenu = true },
                                label = { Text(selectedPriority?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Priority", fontSize = 10.sp) },
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(12.dp)) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(28.dp)
                            )
                            DropdownMenu(
                                expanded = showPriorityMenu, 
                                onDismissRequest = { showPriorityMenu = false },
                                modifier = Modifier.widthIn(max = 200.dp)
                            ) {
                                DropdownMenuItem(text = { Text("All Priorities", fontSize = 14.sp) }, onClick = {
                                    viewModel.setPriorityFilter(null)
                                    showPriorityMenu = false
                                })
                                Priority.entries.forEach { priority ->
                                    DropdownMenuItem(text = { Text(priority.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 14.sp) }, onClick = {
                                        viewModel.setPriorityFilter(priority)
                                        showPriorityMenu = false
                                    })
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
                        label = { Text(selectedStatus?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Status", fontSize = 10.sp) },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(12.dp)) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(28.dp)
                    )
                            DropdownMenu(
                                expanded = showStatusMenu, 
                                onDismissRequest = { showStatusMenu = false },
                                modifier = Modifier.widthIn(max = 200.dp)
                            ) {
                                DropdownMenuItem(text = { Text("All Status", fontSize = 14.sp) }, onClick = {
                                    viewModel.setStatusFilter(null)
                                    showStatusMenu = false
                                })
                                DropdownMenuItem(text = { Text("Pending", fontSize = 14.sp) }, onClick = {
                                    viewModel.setStatusFilter(TaskStatus.PENDING)
                                    showStatusMenu = false
                                })
                                DropdownMenuItem(text = { Text("Completed", fontSize = 14.sp) }, onClick = {
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
                        label = { Text(selectedRecurrence?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Recurrence", fontSize = 10.sp) },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(12.dp)) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(28.dp)
                    )
                            DropdownMenu(
                                expanded = showRecurrenceMenu, 
                                onDismissRequest = { showRecurrenceMenu = false },
                                modifier = Modifier.widthIn(max = 200.dp)
                            ) {
                                DropdownMenuItem(text = { Text("All Recurrences", fontSize = 14.sp) }, onClick = {
                                    viewModel.setRecurrenceFilter(null)
                                    showRecurrenceMenu = false
                                })
                                // Only show Today (NONE) and Daily in the filter
                                listOf(RecurrenceType.NONE, RecurrenceType.DAILY).forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(if (type == RecurrenceType.NONE) "One-Time" else type.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 14.sp) },
                                        onClick = {
                                            viewModel.setRecurrenceFilter(type)
                                            showRecurrenceMenu = false
                                        }
                                    )
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
                        label = { Text(selectedCategory ?: "Category", fontSize = 10.sp) },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(12.dp)) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(28.dp)
                    )
                            DropdownMenu(
                                expanded = showCategoryMenu,
                                onDismissRequest = { showCategoryMenu = false },
                                modifier = Modifier.widthIn(min = 150.dp, max = 280.dp).heightIn(max = 400.dp)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("All Categories", fontSize = 14.sp) },
                                    onClick = {
                                        viewModel.setCategoryFilter(null)
                                        showCategoryMenu = false
                                    },
                                    leadingIcon = { if (selectedCategory == null) Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                )
                                HorizontalDivider()
                                categories.forEach { category ->
                                    val isSelected = selectedCategory == category
                                    DropdownMenuItem(
                                        text = { Text(category, fontSize = 14.sp) },
                                        onClick = {
                                            viewModel.setCategoryFilter(category)
                                            showCategoryMenu = false
                                        },
                                        leadingIcon = {
                                            if (isSelected) Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (selectedPriority != null || searchQuery.isNotEmpty() || selectedStatus != null || selectedRecurrence != null || selectedCategory != null) {
                        item {
                            TextButton(onClick = { viewModel.clearAllFilters(); viewModel.setSearchQuery("") }) {
                                Text("Clear All", fontSize = 10.sp, color = Color.Red)
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                FloatingActionButton(
                    onClick = onStartVoiceInput,
                    containerColor = MaterialTheme.colorScheme.secondary,
                    shape = CircleShape,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "Voice Add", tint = Color.White)
                }
                FloatingActionButton(
                    onClick = {
                        taskToEdit = null
                        showDialog = true
                    },
                    containerColor = SketchPrimary,
                    shape = CircleShape,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Task", tint = Color.White)
                }
            }
        },
        bottomBar = {
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = true,
                    onClick = { /* Already here */ },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Tasks") },
                    label = { Text("Tasks") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToHistory,
                    icon = { Icon(Icons.Outlined.History, contentDescription = "History") },
                    label = { Text("History") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToDashboard,
                    icon = { Icon(Icons.Default.BarChart, contentDescription = "Analytics") },
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
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(tasks, key = { it.id }) { task ->
                    val isSelected = selectedTasks.contains(task.id)
                    SketchTaskItem(
                        task = task,
                        onToggleStatus = { newStatus -> 
                            if (newStatus == TaskStatus.COMPLETED) {
                                showHistoryConfirmDialog = task
                            } else {
                                viewModel.updateTaskStatus(task, newStatus)
                            }
                        },
                        onDelete = {
                            showClearConfirmDialog = "Are you sure you want to delete this task?" to {
                                if (task.recurrenceType != RecurrenceType.NONE) {
                                    showDeleteRecurrenceDialog = task
                                } else {
                                    viewModel.deleteTask(task)
                                }
                            }
                        },
                        onArchive = { viewModel.archiveTask(task) },
                        onClick = {
                            if (isSelectionMode) {
                                if (isSelected) selectedTasks.remove(task.id)
                                else selectedTasks.add(task.id)
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
            }
        }

        if (showHistoryConfirmDialog != null) {
            AlertDialog(
                onDismissRequest = { showHistoryConfirmDialog = null },
                title = { Text("Task Completed") },
                text = { Text("Is this task done? It will be moved to History.") },
                confirmButton = {
                    Button(onClick = {
                        viewModel.updateTaskStatus(showHistoryConfirmDialog!!, TaskStatus.COMPLETED)
                        showHistoryConfirmDialog = null
                    }, colors = ButtonDefaults.buttonColors(containerColor = SketchPrimary)) {
                        Text("YES")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showHistoryConfirmDialog = null }) {
                        Text("CANCEL")
                    }
                }
            )
        }

        if (showClearConfirmDialog != null) {
            AlertDialog(
                onDismissRequest = { showClearConfirmDialog = null },
                title = { Text("CONFIRM ACTION") },
                text = { Text(showClearConfirmDialog!!.first) },
                confirmButton = {
                    TextButton(onClick = {
                        showClearConfirmDialog!!.second()
                        showClearConfirmDialog = null
                    }) { Text("YES", color = Color.Red) }
                },
                dismissButton = {
                    TextButton(onClick = { showClearConfirmDialog = null }) { Text("NO") }
                }
            )
        }

        if (showDeleteRecurrenceDialog != null) {
            AlertDialog(
                onDismissRequest = { showDeleteRecurrenceDialog = null },
                title = { Text("REMOVE TASK", fontWeight = FontWeight.ExtraBold) },
                text = { Text("This is a recurring task. Do you want to stop the entire series or just remove this single occurrence?") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteTask(showDeleteRecurrenceDialog!!, deleteFuture = true)
                            showDeleteRecurrenceDialog = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        shape = RoundedCornerShape(8.dp)
                    ) { Text("STOP SERIES", color = Color.White) }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = {
                            viewModel.deleteTask(showDeleteRecurrenceDialog!!, deleteFuture = false)
                            showDeleteRecurrenceDialog = null
                        }) { Text("JUST THIS ONCE", color = SketchPrimary) }

                        TextButton(onClick = { showDeleteRecurrenceDialog = null }) {
                            Text("CANCEL", color = Color.Gray)
                        }
                    }
                }
            )
        }

        if (showManageCategoriesGlobal) {
            ManageCategoriesDialog(
                categories = categories,
                onDismiss = { showManageCategoriesGlobal = false },
                onDelete = { viewModel.deleteCategory(it) },
                onEdit = { old, new -> viewModel.updateCategory(old, new) }
            )
        }


        val categoriesForDialog by viewModel.categories.collectAsState()

        if (showDialog) {
            TaskDialog(
                task = taskToEdit,
                initialTitle = voiceTitle ?: "",
                initialPriority = voicePriority,
                initialDate = voiceDate,
                initialEndDate = voiceEndDate,
                initialStartTime = voiceStartTime,
                initialEndTime = voiceEndTime,
                initialCategory = voiceCategory,
                initialRecurrence = voiceRecurrence,
                onDismiss = { 
                    showDialog = false
                    onVoiceComplete()
                },
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
                    onVoiceComplete()
                },
                availableCategories = categoriesForDialog,
                onDeleteCategory = { viewModel.deleteCategory(it) }
            )
        }
    }
}

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
    // Removed unused state: var showStatusMenu by remember { mutableStateOf(false) }

    val isFutureDailyTask = remember(task.recurrenceType, task.targetDate, task.startTime, task.targetEndDate) {
        if (task.recurrenceType == RecurrenceType.DAILY && (task.targetDate != null || task.targetEndDate != null)) {
            val now = Calendar.getInstance()
            val finalTargetDate = task.targetEndDate ?: task.targetDate!!
            val targetCal = Calendar.getInstance().apply { timeInMillis = finalTargetDate }
            
            val isFutureDay = targetCal.get(Calendar.YEAR) > now.get(Calendar.YEAR) ||
                (targetCal.get(Calendar.YEAR) == now.get(Calendar.YEAR) && targetCal.get(Calendar.DAY_OF_YEAR) > now.get(Calendar.DAY_OF_YEAR))
            
            val isToday = targetCal.get(Calendar.YEAR) == now.get(Calendar.YEAR) && 
                targetCal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)

            if (isFutureDay) {
                true // Target day is in the future
            } else if (isToday) {
                // Today is the target day, check if start time has passed
                task.startTime?.let { startTimeMillis ->
                    val currentMillisFromDayStart = (now.get(Calendar.HOUR_OF_DAY) * 3600000L) +
                                                   (now.get(Calendar.MINUTE) * 60000L)
                    currentMillisFromDayStart < startTimeMillis
                } ?: false
            } else {
                false // Target day has already passed
            }
        } else {
            false
        }
    }

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

        val isFinalInstance = remember(task.recurrenceType, task.targetDate, task.targetEndDate) {
            if (task.recurrenceType == RecurrenceType.NONE || task.targetDate == null || task.targetEndDate == null) {
                false
            } else {
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = task.targetDate
                when (task.recurrenceType) {
                    RecurrenceType.DAILY -> calendar.add(Calendar.DAY_OF_YEAR, 1)
                    RecurrenceType.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                    RecurrenceType.MONTHLY -> calendar.add(Calendar.MONTH, 1)
                    RecurrenceType.YEARLY -> calendar.add(Calendar.YEAR, 1)
                    else -> {}
                }
                // If adding the next interval makes it go past the end date, it's the final instance
                calendar.timeInMillis > task.targetEndDate
            }
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
                    color = if (isSelected) SketchPrimary
                            else if (isFutureDailyTask) Color(0xFFFF9800)
                            else if (isNear && task.status != TaskStatus.COMPLETED) SketchError
                            else MaterialTheme.colorScheme.onBackground,
                    shape = RoundedCornerShape(16.dp)
                ),
            // Highlight recurring tasks with a subtle background color change if they are not active
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) SketchPrimary.copy(alpha = 0.2f)
                else if (isFutureDailyTask) MaterialTheme.colorScheme.surface.copy(alpha = 0.8f) // Adjusted for better night mode contrast
                else if (task.status == TaskStatus.PENDING) MaterialTheme.colorScheme.surface
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

                    // Task Checkbox (Only visible if active)
                    if (!isFutureDailyTask) {
                        IconButton(
                            onClick = { onToggleStatus(if (task.status == TaskStatus.COMPLETED) TaskStatus.PENDING else TaskStatus.COMPLETED) }
                        ) {
                            val icon = if (task.status == TaskStatus.COMPLETED) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank
                            Icon(
                                icon,
                                contentDescription = "Toggle status",
                                tint = if (task.status == TaskStatus.COMPLETED) SketchPrimary else if (isNear) SketchError else Color.Gray
                            )
                        }
                    } else {
                        // Spacer to keep layout consistent when checkbox is hidden
                        Spacer(modifier = Modifier.size(48.dp))
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
                            Spacer(modifier = Modifier.width(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(
                                        if (isFinalInstance) Color(0xFFFFE4B5).copy(alpha = 0.5f) else Color(0xFFFF9800).copy(alpha = 0.2f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    if (isFinalInstance) Icons.Default.Flag else Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = if (isFinalInstance) Color(0xFFDAA520) else Color(0xFFEF6C00),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    if (isFinalInstance) "Final" else task.recurrenceType.name.lowercase().replaceFirstChar { it.uppercase() },
                                    fontSize = 10.sp,
                                    color = if (isFinalInstance) Color(0xFFB8860B) else Color(0xFFE65100),
                                    fontWeight = FontWeight.Bold
                                )
                            }
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
                            text = task.subcategory,
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                            maxLines = 1,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }

                    Column( // Changed from Row to Column to stack date/time and category if needed for space
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (task.recurrenceType != RecurrenceType.NONE && task.targetEndDate != null) {
                                    "${dateFormatter.format(Date(task.targetDate ?: 0L))} - ${dateFormatter.format(Date(task.targetEndDate))}"
                                } else {
                                    dateFormatter.format(Date(task.targetDate ?: 0L))
                                },
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }

                        if (task.startTime != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (task.endTime != null) {
                                        "${timeFormatter.format(Date(task.startTime))} - ${timeFormatter.format(Date(task.endTime))}"
                                    } else {
                                        timeFormatter.format(Date(task.startTime))
                                    },
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .background(SketchPrimary.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(task.category, fontSize = 10.sp, color = SketchPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete task", tint = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun PriorityBadge(priority: Priority) {
    val color = when (priority) {
        Priority.HIGH -> Color.Red
        Priority.MEDIUM -> Color.Blue
        Priority.LOW -> Color.Green
    }
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
            .border(1.dp, color, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = priority.name,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun TaskDialog(
    task: Task? = null,
    initialTitle: String = "",
    initialPriority: Priority? = null,
    initialDate: Long? = null,
    initialEndDate: Long? = null,
    initialStartTime: Long? = null,
    initialEndTime: Long? = null,
    initialCategory: String? = null,
    initialRecurrence: RecurrenceType? = null,
    onDismiss: () -> Unit,
    onConfirm: (Task) -> Unit,
    onDeleteCategory: ((String) -> Unit)? = null,
    availableCategories: List<String> = listOf("Personal", "Work", "Meeting", "Others")
) {
    var title by remember { mutableStateOf(if (initialTitle.isNotEmpty()) initialTitle else task?.title ?: "") }
    var description by remember { mutableStateOf(task?.description ?: "") }
    var category by remember { mutableStateOf(initialCategory ?: task?.category ?: "Personal") }
    var customCategory by remember { mutableStateOf("") }
    var isAddingCustomCategory by remember { mutableStateOf(false) }
    var subcategory by remember { mutableStateOf(task?.subcategory ?: "") }
    var priority by remember { mutableStateOf(initialPriority ?: task?.priority ?: Priority.MEDIUM) }

    var categorySearchQuery by remember { mutableStateOf("") }

    var showFromDatePicker by remember { mutableStateOf(false) }
    var showToDatePicker by remember { mutableStateOf(false) }

    var targetDate by remember {
        mutableStateOf<Long?>(initialDate ?: task?.targetDate ?: Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis)
    }
    var targetEndDate by remember { mutableStateOf(initialEndDate ?: task?.targetEndDate) }
    var recurrenceType by remember { mutableStateOf(initialRecurrence ?: task?.recurrenceType ?: RecurrenceType.NONE) }

    var startTime by remember {
        mutableStateOf<Long?>(initialStartTime ?: task?.startTime ?: Calendar.getInstance().let {
            (it.get(Calendar.HOUR_OF_DAY) * 3600000L) + (it.get(Calendar.MINUTE) * 60000L)
        })
    }
    var endTime by remember { mutableStateOf(initialEndTime ?: task?.endTime) }

    var showTimePickerForStart by remember { mutableStateOf(false) }
    var showTimePickerForEnd by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.padding(12.dp).fillMaxWidth(),
        title = {
            Text(
                if (task == null) "NEW TASK" else "EDIT TASK",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title *", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    isError = title.isBlank(),
                    supportingText = if (title.isBlank()) { { Text("Task Title is required", fontSize = 10.sp) } } else null
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    var showRecurrenceInfo by remember { mutableStateOf(false) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Recurrence *", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(onClick = { showRecurrenceInfo = true }, modifier = Modifier.size(16.dp)) {
                            Icon(Icons.Default.Info, contentDescription = "Recurrence Info", tint = SketchPrimary, modifier = Modifier.size(14.dp))
                        }
                    }

                    if (showRecurrenceInfo) {
                        AlertDialog(
                            onDismissRequest = { showRecurrenceInfo = false },
                            title = { Text("About Recurrence", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                            text = {
                                Text("A recurring task automatically creates its next instance once the current one is completed. This ensures you never miss repeated duties.", fontSize = 12.sp)
                            },
                            confirmButton = { TextButton(onClick = { showRecurrenceInfo = false }) { Text("Got it") } }
                        )
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        // Only show NONE and DAILY recurrence types
                        listOf(RecurrenceType.NONE, RecurrenceType.DAILY).forEach { type ->
                            val isSelected = recurrenceType == type
                        FilterChip(
                                selected = isSelected,
                                onClick = {
                                    recurrenceType = type
                                    if (type == RecurrenceType.NONE) {
                                        targetEndDate = null
                                    } else {
                                        // When Daily, ensure a valid targetDate exists
                                        if (targetDate == null) {
                                            targetDate = Calendar.getInstance().apply {
                                                set(Calendar.HOUR_OF_DAY, 0)
                                                set(Calendar.MINUTE, 0)
                                                set(Calendar.SECOND, 0)
                                                set(Calendar.MILLISECOND, 0)
                                            }.timeInMillis
                                        }
                                    }
                                },
                                label = {
                                    Text(
                                        text = if (type == RecurrenceType.NONE) "One-Time"
                                               else type.name.lowercase().replaceFirstChar { it.uppercase() },
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SketchPrimary,
                                    selectedLabelColor = Color.White,
                                    labelColor = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.height(32.dp)
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val isCategoryValid = if (isAddingCustomCategory) customCategory.isNotBlank() else true
                    Text(
                        "Category *",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (!isCategoryValid) MaterialTheme.colorScheme.error else Color.Unspecified
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isAddingCustomCategory) {
                            OutlinedTextField(
                                value = customCategory,
                                onValueChange = { customCategory = it },
                                label = { Text("Custom Category Name *", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                                isError = customCategory.isBlank(),
                                trailingIcon = {
                                    IconButton(onClick = { isAddingCustomCategory = false; customCategory = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Cancel", modifier = Modifier.size(20.dp))
                                    }
                                },
                                singleLine = true
                            )
                        } else {
                            // Category Dropdown on the left (weight 1f)
                            var expanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedButton(
                                    onClick = { expanded = true },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)),
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) {
                                    Text(category, modifier = Modifier.weight(1f), textAlign = TextAlign.Start, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                    modifier = Modifier.widthIn(min = 150.dp, max = 280.dp).heightIn(max = 400.dp)
                                ) {
                                    // Always include the current category if it's not in the available list (e.g., from voice)
                                    val finalDisplayCategories = if (category.isNotBlank() && !availableCategories.contains(category)) {
                                        listOf(category) + availableCategories
                                    } else {
                                        availableCategories
                                    }.let { cats ->
                                        if (categorySearchQuery.isBlank()) cats
                                        else cats.filter { it.contains(categorySearchQuery, ignoreCase = true) }
                                    }

                                    finalDisplayCategories.forEach { cat ->
                                        DropdownMenuItem(
                                            text = { Text(cat) },
                                            onClick = {
                                                category = cat
                                                expanded = false
                                            },
                                            leadingIcon = {
                                                if (category == cat) Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                                            }
                                        )
                                    }
                                }
                            }

                            // Add Custom button on the right
                            IconButton(
                                onClick = { isAddingCustomCategory = true },
                                modifier = Modifier
                                    .size(48.dp)
                                    .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Custom", tint = SketchPrimary)
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Schedule", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { showFromDatePicker = true },
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (targetDate == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)),
                                contentPadding = PaddingValues(start = 4.dp, end = 24.dp)
                            ) {
                                Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                val dateText = targetDate?.let { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(it)) } ?: "Select Date"
                                Column {
                                    Text("First Instance *", fontSize = 7.sp, color = if (targetDate == null) MaterialTheme.colorScheme.error else Color.Gray)
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
                            val isEndDateSelectionEnabled = recurrenceType == RecurrenceType.DAILY
                            val currentTargetDate = targetDate
    val currentTargetEndDate = targetEndDate
    val isEndDateInvalid = recurrenceType == RecurrenceType.DAILY &&
                           currentTargetDate != null &&
                           currentTargetEndDate != null &&
                           currentTargetEndDate < currentTargetDate
                            OutlinedButton(
                                onClick = { if (isEndDateSelectionEnabled) showToDatePicker = true },
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (isEndDateInvalid) MaterialTheme.colorScheme.error else if (isEndDateSelectionEnabled) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f) else Color.LightGray.copy(alpha = 0.3f)),
                                enabled = isEndDateSelectionEnabled,
                                contentPadding = PaddingValues(start = 4.dp, end = 24.dp)
                            ) {
                                Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                val dateText = targetEndDate?.let { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(it)) } ?: "To"
                                Column {
                                    Text(if (recurrenceType == RecurrenceType.DAILY) "To *" else "To", fontSize = 7.sp, color = if (recurrenceType == RecurrenceType.DAILY && targetEndDate == null) MaterialTheme.colorScheme.error else Color.Gray)
                                    Text(dateText, fontSize = 11.sp, color = if (isEndDateSelectionEnabled) Color.Unspecified else Color.LightGray)
                                }
                            }
                            if (targetEndDate != null && isEndDateSelectionEnabled) {
                                IconButton(
                                    onClick = { targetEndDate = null },
                                    modifier = Modifier.align(Alignment.CenterEnd).size(24.dp).padding(end = 2.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(12.dp), tint = Color.Gray)
                                }
                            }
                            if (isEndDateInvalid) {
                                Text("End date must be after start date", color = MaterialTheme.colorScheme.error, fontSize = 8.sp, modifier = Modifier.padding(start = 4.dp))
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TimeButtonSmall(
                            label = "Start *",
                            time = startTime,
                            onClick = { showTimePickerForStart = true },
                            onClear = { startTime = null },
                            modifier = Modifier.weight(1f),
                            isError = startTime == null
                        )
                        TimeButtonSmall(
                            label = "End *",
                            time = endTime,
                            onClick = { showTimePickerForEnd = true },
                            onClear = { endTime = null },
                            modifier = Modifier.weight(1f),
                            isError = endTime == null
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
            val isCategoryValid = if (isAddingCustomCategory) customCategory.isNotBlank() else true
            var canSave = title.isNotBlank() && targetDate != null && startTime != null && endTime != null && isCategoryValid
            val currentTargetDate = targetDate
    val currentTargetEndDate = targetEndDate
    val isEndDateInvalid = recurrenceType == RecurrenceType.DAILY &&
                           currentTargetDate != null &&
                           currentTargetEndDate != null &&
                           currentTargetEndDate < currentTargetDate

            // Validation for Daily recurrence: End date is required and must not be before start date
            if (recurrenceType == RecurrenceType.DAILY && (targetEndDate == null || isEndDateInvalid)) {
                canSave = false
            }

            Button(
                onClick = {
                    if (canSave) {
                        val finalCategory = if (isAddingCustomCategory) customCategory.trim() else category
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
                    } else {
                        // If Daily recurrence selected and targetEndDate is null, show a toast
                        if (recurrenceType == RecurrenceType.DAILY && targetEndDate == null) {
                            // In a real app, you'd show a Toast or Snackbar here.
                            // For this environment, the disabled button and red border on "To" date serve as visual feedback.
                        }
                    }
                },
                enabled = canSave,
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
                modifier = Modifier.height(44.dp).fillMaxWidth().padding(horizontal = 12.dp)
            ) {
                Text("CANCEL", fontWeight = FontWeight.Bold, color = Color.Gray)
            }
        }
    )

    if (showFromDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = targetDate)
        DatePickerDialog(
            onDismissRequest = { showFromDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    targetDate = datePickerState.selectedDateMillis
                    showFromDatePicker = false
                }) { Text("OK") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showToDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = targetEndDate ?: targetDate)
        DatePickerDialog(
            onDismissRequest = { showToDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selected = datePickerState.selectedDateMillis
                    val currentTargetDate = targetDate
                    if (selected != null && currentTargetDate != null && selected < currentTargetDate) {
                        targetEndDate = currentTargetDate // Set end date to start date if invalid
                    } else {
                        targetEndDate = selected
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
    onDelete: (String) -> Unit,
    onEdit: (String, String) -> Unit
) {
    val defaults = listOf("Personal", "Work", "Meeting", "Others")
    val customCategories = categories.filter { cat ->
        defaults.none { it.equals(cat, ignoreCase = true) }
    }

    var categoryToDelete by remember { mutableStateOf<String?>(null) }
    var categoryToEdit by remember { mutableStateOf<String?>(null) }
    var editName by remember { mutableStateOf("") }

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
                                Text(cat, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Row {
                                    IconButton(onClick = { 
                                        categoryToEdit = cat 
                                        editName = cat
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = SketchPrimary)
                                    }
                                    IconButton(onClick = { categoryToDelete = cat }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                                    }
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

    if (categoryToDelete != null) {
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("Delete Category") },
            text = { Text("Are you sure you want to delete the category '${categoryToDelete}'? This will not delete the tasks, but they will no longer be associated with this category.") },
            confirmButton = {
                TextButton(onClick = {
                    categoryToDelete?.let { onDelete(it) }
                    categoryToDelete = null
                }) {
                    Text("DELETE", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("CANCEL")
                }
            }
        )
    }

    if (categoryToEdit != null) {
        AlertDialog(
            onDismissRequest = { categoryToEdit = null },
            title = { Text("Edit Category") },
            text = {
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("Category Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editName.isNotBlank() && categoryToEdit != null) {
                            onEdit(categoryToEdit!!, editName.trim())
                            categoryToEdit = null
                        }
                    },
                    enabled = editName.isNotBlank()
                ) {
                    Text("SAVE")
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToEdit = null }) {
                    Text("CANCEL")
                }
            }
        )
    }
}

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
                val finalHour = timePickerState.hour
                val finalMinute = timePickerState.minute
                val offset = (finalHour * 3600000L) + (finalMinute * 60000L)
                onTimeSelected(offset)
                onDismiss()
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TimePicker(state = timePickerState)
            }
        }
    )
}

@Composable
fun TimeButtonSmall(
    label: String,
    time: Long?,
    onClick: () -> Unit,
    onClear: () -> Unit,
    enabled: Boolean = true,
    isError: Boolean = false,
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
            border = BorderStroke(1.dp, if (isError) MaterialTheme.colorScheme.error else if (enabled) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f) else Color.LightGray.copy(alpha = 0.3f)),
            enabled = enabled
        ) {
            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(14.dp), tint = if (isError) MaterialTheme.colorScheme.error else LocalContentColor.current)
            Spacer(modifier = Modifier.width(4.dp))
            Column {
                Text(label, fontSize = 7.sp, color = if (isError) MaterialTheme.colorScheme.error else if (enabled) Color.Gray else Color.LightGray)
                Text(
                    time?.let {
                        formatter.format(Date(it))
                    } ?: "--:--",
                    fontSize = 11.sp,
                    color = if (isError) MaterialTheme.colorScheme.error else if (enabled) Color.Unspecified else Color.LightGray
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
