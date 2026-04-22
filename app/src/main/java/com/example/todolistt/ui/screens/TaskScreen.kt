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
    var showDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = SketchPaper,
        topBar = {
            TopAppBar(
                title = { 
                    Text("MY TASKS", 
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp
                        )
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SketchPaper)
            )
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
                modifier = Modifier.border(2.dp, SketchBlack, RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Category Filter
            LazyRow(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { 
                            selectedCategory = null
                            viewModel.setCategoryFilter(null)
                        },
                        label = { Text("All") },
                        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selectedCategory == null, borderColor = SketchBlack, selectedBorderColor = SketchBlack, borderWidth = 2.dp, selectedBorderWidth = 2.dp)
                    )
                }
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { 
                            selectedCategory = category
                            viewModel.setCategoryFilter(category)
                        },
                        label = { Text(category) },
                        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selectedCategory == category, borderColor = SketchBlack, selectedBorderColor = SketchBlack, borderWidth = 2.dp, selectedBorderWidth = 2.dp)
                    )
                }
            }

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
                }
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
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(2.dp, SketchBlack, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
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
                    tint = if (task.status == TaskStatus.COMPLETED) SketchPrimary else SketchBlack
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            textDecoration = if (task.status == TaskStatus.COMPLETED) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    PriorityBadge(task.priority)
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
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color.Gray)
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
    onConfirm: (Task) -> Unit
) {
    var title by remember { mutableStateOf(task?.title ?: "") }
    var description by remember { mutableStateOf(task?.description ?: "") }
    var category by remember { mutableStateOf(task?.category ?: "Personal") }
    var customCategory by remember { mutableStateOf("") }
    var isAddingCustomCategory by remember { mutableStateOf(false) }
    var subcategory by remember { mutableStateOf(task?.subcategory ?: "") }
    var priority by remember { mutableStateOf(task?.priority ?: Priority.MEDIUM) }
    
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = task?.targetDate ?: System.currentTimeMillis())

    var startTime by remember { mutableStateOf(task?.startTime) }
    var endTime by remember { mutableStateOf(task?.endTime) }
    var showTimePickerForStart by remember { mutableStateOf(false) }
    var showTimePickerForEnd by remember { mutableStateOf(false) }

    val categories = listOf("Personal", "Work", "Wishlist", "Meeting", "Session", "Others")

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

                Text("Category", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat && !isAddingCustomCategory,
                            onClick = { 
                                category = cat
                                isAddingCustomCategory = false
                            },
                            label = { Text(cat) },
                            border = FilterChipDefaults.filterChipBorder(enabled = true, selected = category == cat, borderColor = SketchBlack, selectedBorderColor = SketchBlack, borderWidth = 1.dp, selectedBorderWidth = 2.dp)
                        )
                    }
                    FilterChip(
                        selected = isAddingCustomCategory,
                        onClick = { isAddingCustomCategory = true },
                        label = { Text("+ Custom") },
                        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = isAddingCustomCategory, borderColor = SketchBlack, selectedBorderColor = SketchBlack, borderWidth = 1.dp, selectedBorderWidth = 2.dp)
                    )
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
                        border = BorderStroke(2.dp, SketchBlack)
                    ) {
                        Icon(Icons.Default.Event, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(datePickerState.selectedDateMillis?.let { 
                            SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(it))
                        } ?: "Date")
                    }

                    Column(modifier = Modifier.weight(1.5f)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TimeButton(label = "Start", time = startTime) { showTimePickerForStart = true }
                            TimeButton(label = "End", time = endTime) { showTimePickerForEnd = true }
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
                            border = FilterChipDefaults.filterChipBorder(enabled = true, selected = priority == p, borderColor = SketchBlack, selectedBorderColor = SketchBlack, borderWidth = 1.dp, selectedBorderWidth = 2.dp)
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
                modifier = Modifier.border(2.dp, SketchBlack, RoundedCornerShape(12.dp))
            ) {
                Text("SAVE", fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", color = Color.Gray) }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SketchTimePickerDialog(onDismiss: () -> Unit, onTimeSelected: (Long) -> Unit) {
    val currentTime = Calendar.getInstance()
    val timePickerState = rememberTimePickerState(
        initialHour = currentTime.get(Calendar.HOUR_OF_DAY),
        initialMinute = currentTime.get(Calendar.MINUTE)
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
fun TimeButton(label: String, time: Long?, onClick: () -> Unit) {
    val formatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.width(90.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, SketchBlack)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 10.sp, color = Color.Gray)
            Text(time?.let { formatter.format(Date(it)) } ?: "--:--", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}
