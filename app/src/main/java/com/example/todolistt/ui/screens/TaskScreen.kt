package com.example.todolistt.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todolistt.data.local.Priority
import com.example.todolistt.data.local.Task
import com.example.todolistt.data.local.TaskStatus
import com.example.todolistt.ui.theme.SketchBlack
import com.example.todolistt.ui.theme.SketchError
import com.example.todolistt.ui.theme.SketchPaper
import com.example.todolistt.ui.theme.SketchPrimary
import com.example.todolistt.ui.viewmodel.TaskViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(viewModel: TaskViewModel) {
    val tasks by viewModel.tasks.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    var showManageCategoriesGlobal by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.background).padding(top = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MY TASKS",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp
                        )
                    )
                    Row {
                        IconButton(onClick = { showManageCategoriesGlobal = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                        IconButton(onClick = { viewModel.toggleTheme() }) {
                            Icon(
                                if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle Theme"
                            )
                        }
                    }
                }

                // Global Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    placeholder = { Text("Search tasks or categories...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onBackground
                    )
                )

                LazyRow(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { viewModel.setCategoryFilter(null) },
                            label = { Text("All") },
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true, 
                                selected = selectedCategory == null, 
                                borderColor = MaterialTheme.colorScheme.onBackground, 
                                selectedBorderColor = MaterialTheme.colorScheme.onBackground, 
                                borderWidth = 2.dp, 
                                selectedBorderWidth = 2.dp
                            )
                        )
                    }
                    items(categories) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { viewModel.setCategoryFilter(category) },
                            label = { Text(category) },
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true, 
                                selected = selectedCategory == category, 
                                borderColor = MaterialTheme.colorScheme.onBackground, 
                                selectedBorderColor = MaterialTheme.colorScheme.onBackground, 
                                borderWidth = 2.dp, 
                                selectedBorderWidth = 2.dp
                            )
                        )
                    }
                }
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
                items(tasks) { task ->
                    SketchTaskItem(
                        task = task,
                        onToggleCompletion = { viewModel.toggleTaskCompletion(task) },
                        onDelete = { viewModel.deleteTask(task) },
                        onClick = {
                            taskToEdit = task
                            showDialog = true
                        }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
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
                            updatedTask.targetTime,
                            updatedTask.startTime,
                            updatedTask.endTime
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

@Composable
fun SketchTaskItem(
    task: Task,
    onToggleCompletion: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

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
            .clickable { onClick() }
            .border(
                width = 2.dp,
                color = if (isNear && task.status != TaskStatus.COMPLETED) SketchError else MaterialTheme.colorScheme.onBackground,
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isNear && task.status != TaskStatus.COMPLETED) SketchError.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface
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
            IconButton(onClick = onToggleCompletion) {
                Icon(
                    if (task.status == TaskStatus.COMPLETED) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Toggle completion",
                    tint = if (task.status == TaskStatus.COMPLETED) SketchPrimary else if (isNear && task.status != TaskStatus.COMPLETED) SketchError else MaterialTheme.colorScheme.onBackground
                )
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
                        Text(
                            text = " ${dateFormatter.format(Date(date))}",
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskDialog(
    task: Task? = null,
    onDismiss: () -> Unit,
    onConfirm: (Task) -> Unit,
    onDeleteCategory: ((String) -> Unit)? = null,
    availableCategories: List<String> = listOf("Personal", "Work", "Meeting")
) {
    var title by remember { mutableStateOf(task?.title ?: "") }
    var description by remember { mutableStateOf(task?.description ?: "") }
    var category by remember { mutableStateOf(task?.category ?: "Personal") }
    var customCategory by remember { mutableStateOf("") }
    var isAddingCustomCategory by remember { mutableStateOf(false) }
    var subcategory by remember { mutableStateOf(task?.subcategory ?: "") }
    var priority by remember { mutableStateOf(task?.priority ?: Priority.MEDIUM) }
    
    var categorySearchQuery by remember { mutableStateOf("") }
    
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = task?.targetDate ?: System.currentTimeMillis())

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
        title = { Text(if (task == null) "NEW TASK" else "EDIT TASK", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Category", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    var showManageCategories by remember { mutableStateOf(false) }
                    
                    IconButton(onClick = { showManageCategories = true }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Settings, contentDescription = "Manage Categories", modifier = Modifier.size(16.dp))
                    }
                    
                    if (showManageCategories) {
                        ManageCategoriesDialog(
                            categories = availableCategories,
                            onDismiss = { showManageCategories = false },
                            onDelete = { 
                                onDeleteCategory?.invoke(it)
                                if (category == it) category = "Others"
                            }
                        )
                    }
                }
                
                // Search Bar for Categories
                OutlinedTextField(
                    value = categorySearchQuery,
                    onValueChange = { categorySearchQuery = it },
                    placeholder = { Text("Search categories...", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Category Carousel
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(filteredCategories) { cat ->
                        FilterChip(
                            selected = category == cat && !isAddingCustomCategory,
                            onClick = { 
                                category = cat
                                isAddingCustomCategory = false
                            },
                            label = { Text(cat) },
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true, 
                                selected = category == cat && !isAddingCustomCategory, 
                                borderColor = MaterialTheme.colorScheme.onBackground, 
                                selectedBorderColor = MaterialTheme.colorScheme.onBackground, 
                                borderWidth = 1.dp, 
                                selectedBorderWidth = 2.dp
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = isAddingCustomCategory,
                            onClick = { isAddingCustomCategory = true },
                            label = { Text("+ Custom") },
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true, 
                                selected = isAddingCustomCategory, 
                                borderColor = MaterialTheme.colorScheme.onBackground, 
                                selectedBorderColor = MaterialTheme.colorScheme.onBackground, 
                                borderWidth = 1.dp, 
                                selectedBorderWidth = 2.dp
                            )
                        )
                    }
                }

                if (isAddingCustomCategory) {
                    OutlinedTextField(
                        value = customCategory,
                        onValueChange = { customCategory = it },
                        label = { Text("Custom Category Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.onBackground)
                    ) {
                        Icon(Icons.Default.Event, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(datePickerState.selectedDateMillis?.let { 
                            SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(it))
                        } ?: "Date")
                    }

                    Column(modifier = Modifier.weight(1.5f)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            TimeButton(
                                label = "Start", 
                                time = startTime, 
                                onClick = { showTimePickerForStart = true },
                                onClear = { startTime = null }
                            )
                            TimeButton(
                                label = "End", 
                                time = endTime, 
                                onClick = { showTimePickerForEnd = true },
                                onClear = { endTime = null }
                            )
                        }
                    }
                }

                Text("Priority", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Priority.values().forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p.name) },
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true, 
                                selected = priority == p, 
                                borderColor = MaterialTheme.colorScheme.onBackground, 
                                selectedBorderColor = MaterialTheme.colorScheme.onBackground, 
                                borderWidth = 1.dp, 
                                selectedBorderWidth = 2.dp
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = subcategory,
                    onValueChange = { subcategory = it },
                    label = { Text("Subcategory (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
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
                            targetDate = datePickerState.selectedDateMillis,
                            startTime = startTime,
                            endTime = endTime
                        )
                        onConfirm(updated)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SketchPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.border(2.dp, MaterialTheme.colorScheme.onBackground, RoundedCornerShape(12.dp))
            ) {
                Text("SAVE", fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { showDatePicker = false }) { Text("OK") } }
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
    val defaults = listOf("Personal", "Work", "Meeting")
    val customCategories = categories.filter { !defaults.contains(it) }

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
                }
                onTimeSelected(cal.timeInMillis)
                onDismiss()
            }) { Text("OK") }
        },
        text = { TimePicker(state = timePickerState) }
    )
}

@Composable
fun TimeButton(label: String, time: Long?, onClick: () -> Unit, onClear: () -> Unit) {
    val formatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    Box {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.width(90.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(time?.let { formatter.format(Date(it)) } ?: "--:--", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        if (time != null) {
            IconButton(
                onClick = onClear,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 10.dp, y = (-10).dp)
                    .size(24.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Clear",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
