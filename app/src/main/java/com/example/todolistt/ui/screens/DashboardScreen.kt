package com.example.todolistt.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.example.todolistt.data.local.Priority
import com.example.todolistt.ui.theme.SketchPrimary
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.platform.LocalContext
import com.example.todolistt.util.ExportUtility
import com.example.todolistt.ui.viewmodel.TaskViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: TaskViewModel,
    onNavigateToTasks: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onBack: () -> Unit
) {
    val stats by viewModel.stats.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()
    val isRangeFilter by viewModel.isRangeFilter.collectAsState()
    val endMonth by viewModel.endMonth.collectAsState()
    val endYear by viewModel.endYear.collectAsState()
    val categoryTimeFilter by viewModel.categoryTimeFilter.collectAsState()
    
    var showTimeFilterMenu by remember { mutableStateOf(false) }
    var showManageCategoriesGlobal by remember { mutableStateOf(false) }
    var showMonthMenu by remember { mutableStateOf(false) }
    var showRangeDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var showExportMenu by remember { mutableStateOf(false) }

    val dateLabel = remember(selectedMonth, selectedYear, isRangeFilter, endMonth, endYear) {
        if (isRangeFilter) {
            val startCal = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.MONTH, selectedMonth)
                set(Calendar.YEAR, selectedYear)
            }
            val endCal = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.MONTH, endMonth)
                set(Calendar.YEAR, endYear)
            }
            val df = SimpleDateFormat("MMM yyyy", Locale.getDefault())
            "${df.format(startCal.time)} - ${df.format(endCal.time)}"
        } else {
            val cal = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.MONTH, selectedMonth)
                set(Calendar.YEAR, selectedYear)
            }
            SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToTasks,
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
                    selected = true,
                    onClick = { /* Stay here */ },
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
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DASHBOARD",
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )

                Box {
                    IconButton(onClick = { showExportMenu = true }) {
                        Icon(Icons.Default.Share, contentDescription = "Export Analytics", tint = SketchPrimary)
                    }
                    DropdownMenu(
                        expanded = showExportMenu,
                        onDismissRequest = { showExportMenu = false },
                        modifier = Modifier.widthIn(max = 250.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Export Analytics (CSV)") },
                            onClick = {
                                viewModel.tasks.value.let { tasks ->
                                    ExportUtility.exportTasksToCsv(context, tasks)
                                }
                                showExportMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Export Analytics (PDF)") },
                            onClick = {
                                viewModel.tasks.value.let { tasks ->
                                    ExportUtility.exportTasksToPdf(context, tasks)
                                }
                                showExportMenu = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Month/Year Filter UI
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showMonthMenu = true }
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = SketchPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(dateLabel, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)

                        DropdownMenu(
                            expanded = showMonthMenu,
                            onDismissRequest = { showMonthMenu = false },
                            modifier = Modifier.widthIn(min = 150.dp, max = 280.dp).heightIn(max = 400.dp)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Select Date Range...", color = SketchPrimary, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    showMonthMenu = false
                                    showRangeDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.Today, contentDescription = null, tint = SketchPrimary) }
                            )
                            HorizontalDivider()
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
                                        if (!isRangeFilter && selectedMonth == m && selectedYear == y) {
                                            Icon(Icons.Default.Check, contentDescription = null)
                                        }
                                    }
                                )
                            }
                        }
                    }
                    Row {
                        IconButton(onClick = {
                            var m = selectedMonth
                            var y = selectedYear
                            if (m == 0) {
                                m = 11
                                y--
                            } else {
                                m--
                            }
                            viewModel.setDateFilter(m, y)
                        }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Prev", tint = MaterialTheme.colorScheme.onSurface)
                        }
                        IconButton(onClick = {
                            var m = selectedMonth
                            var y = selectedYear
                            if (m == 11) {
                                m = 0
                                y++
                            } else {
                                m++
                            }
                            viewModel.setDateFilter(m, y)
                        }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Top Stat Cards
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCardImproved(
                    label = "Completed Tasks",
                    value = stats.completedTasks.toString(),
                    modifier = Modifier.weight(1f)
                )
                StatCardImproved(
                    label = "Pending Tasks",
                    value = stats.pendingTasks.toString(),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Category Details
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Category Status", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(12.dp))
                    stats.categoryDistribution.forEach { (cat, count) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(cat, color = MaterialTheme.colorScheme.onSurface)
                            Text("$count Pending", fontWeight = FontWeight.Bold, color = SketchPrimary)
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    }
                    if (stats.categoryDistribution.isEmpty()) {
                        Text("No pending tasks", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Recurrence Details
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Recurrence Overview", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(12.dp))
                    stats.recurrenceDistribution.forEach { (type, count) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(type.name.lowercase().replaceFirstChar { it.uppercase() }, color = MaterialTheme.colorScheme.onSurface)
                            Text("$count Tasks", fontWeight = FontWeight.Bold, color = SketchPrimary)
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    }
                    if (stats.recurrenceDistribution.isEmpty()) {
                        Text("No recurrence tasks", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Completion Rate
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Overall Completion", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { stats.completionRate },
                            modifier = Modifier.size(100.dp),
                            strokeWidth = 10.dp,
                            color = SketchPrimary,
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )
                        Text(
                            text = "${(stats.completionRate * 100).toInt()}%",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Total: ${stats.totalTasks} | Completed: ${stats.completedTasks}", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Category Donut Chart Section (Simplified)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Open Tasks in Categories", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                        Box {
                            TextButton(onClick = { showTimeFilterMenu = true }) {
                                Text(categoryTimeFilter, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            DropdownMenu(
                                expanded = showTimeFilterMenu,
                                onDismissRequest = { showTimeFilterMenu = false },
                                modifier = Modifier.widthIn(max = 200.dp)
                            ) {
                                listOf("In 7 days", "In 30 days", "All").forEach { filter ->
                                    DropdownMenuItem(
                                        text = { Text(filter) },
                                        onClick = {
                                            viewModel.setCategoryTimeFilter(filter)
                                            showTimeFilterMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        DonutChart(
                            data = stats.categoryDistribution,
                            modifier = Modifier.size(150.dp)
                        )
                        
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val colors = listOf(Color(0xFF4C6EF5), Color(0xFF228BE6), Color(0xFFD0EBFF))
                            stats.categoryDistribution.entries.take(3).forEachIndexed { index, entry ->
                                CategoryLegend(
                                    label = entry.key,
                                    value = entry.value.toString(),
                                    color = colors[index % colors.size]
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Daily Task Bar Chart Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Task completion", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text(if (isRangeFilter) "Custom Range" else "Last 7 days", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    BarChart(
                        data = stats.dailyCompletion,
                        modifier = Modifier.height(150.dp).fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Weekly Comparison Chart
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Weekly Comparison", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(16.dp))
                    WeeklyComparisonChart(
                        thisWeek = stats.weeklyComparison["This Week"] ?: emptyList(),
                        lastWeek = stats.weeklyComparison["Last Week"] ?: emptyList(),
                        modifier = Modifier.height(150.dp).fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        LegendItem("This Week", Color(0xFF4C6EF5))
                        LegendItem("Last Week", Color(0xFF4C6EF5).copy(alpha = 0.3f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Priority Heatmap
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Priority Distribution", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(16.dp))
                    PriorityHeatmap(
                        data = stats.priorityDistribution,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Time of Day Insights
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val peakHour = stats.hourlyCompletion.maxByOrNull { it.value }?.key ?: -1
                    val peakText = if (peakHour != -1) {
                        val period = if (peakHour < 12) "AM" else "PM"
                        val displayHour = if (peakHour % 12 == 0) 12 else peakHour % 12
                        "Your peak productivity is around $displayHour $period"
                    } else "Complete more tasks to see insights"
                    
                    Text("Peak Productivity", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(peakText, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    HourlyActivityChart(
                        data = stats.hourlyCompletion,
                        modifier = Modifier.height(100.dp).fillMaxWidth()
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (showManageCategoriesGlobal) {
            ManageCategoriesDialog(
                categories = categories,
                onDismiss = { showManageCategoriesGlobal = false },
                onDelete = { viewModel.deleteCategory(it) }
            )
        }

        if (showRangeDialog) {
            DateRangePickerDialog(
                onDismiss = { showRangeDialog = false },
                onRangeSelected = { startM, startY, endM, endY ->
                    viewModel.setDateRangeFilter(startM, startY, endM, endY)
                    showRangeDialog = false
                }
            )
        }
    }
}

@Composable
fun DateRangePickerDialog(
    onDismiss: () -> Unit,
    onRangeSelected: (Int, Int, Int, Int) -> Unit
) {
    var startMonth by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.MONTH)) }
    var startYear by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var endMonth by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.MONTH)) }
    var endYear by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.YEAR)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Date Range") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Start Month", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MonthSpinner(
                        selectedMonth = startMonth,
                        onMonthSelected = { startMonth = it },
                        modifier = Modifier.weight(1f)
                    )
                    YearSpinner(
                        selectedYear = startYear,
                        onYearSelected = { startYear = it },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Text("End Month", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MonthSpinner(
                        selectedMonth = endMonth,
                        onMonthSelected = { endMonth = it },
                        modifier = Modifier.weight(1f)
                    )
                    YearSpinner(
                        selectedYear = endYear,
                        onYearSelected = { endYear = it },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            val startVal = startYear * 12 + startMonth
            val endVal = endYear * 12 + endMonth
            val isValid = endVal >= startVal
            
            Button(
                onClick = { onRangeSelected(startMonth, startYear, endMonth, endYear) },
                enabled = isValid
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun MonthSpinner(selectedMonth: Int, onMonthSelected: (Int) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val months = (0..11).map { m ->
        val cal = Calendar.getInstance().apply { set(Calendar.MONTH, m) }
        SimpleDateFormat("MMMM", Locale.getDefault()).format(cal.time)
    }

    Box(modifier = modifier) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(months[selectedMonth])
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(max = 200.dp).heightIn(max = 400.dp)
        ) {
            months.forEachIndexed { index, month ->
                DropdownMenuItem(
                    text = { Text(month) },
                    onClick = {
                        onMonthSelected(index)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun YearSpinner(selectedYear: Int, onYearSelected: (Int) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val years = (currentYear - 2..currentYear + 5).toList()

    Box(modifier = modifier) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selectedYear.toString())
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(max = 200.dp).heightIn(max = 400.dp)
        ) {
            years.forEach { year ->
                DropdownMenuItem(
                    text = { Text(year.toString()) },
                    onClick = {
                        onYearSelected(year)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun StatCardImproved(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun DonutChart(data: Map<String, Int>, modifier: Modifier = Modifier) {
    val colors = listOf(Color(0xFF4C6EF5), Color(0xFF228BE6), Color(0xFFD0EBFF), Color(0xFFA5D8FF))
    val total = data.values.sum().toFloat()
    
    Canvas(modifier = modifier) {
        val strokeWidth = 30.dp.toPx()
        val innerRadius = (size.minDimension - strokeWidth) / 2
        var startAngle = -90f
        
        if (total == 0f) {
            drawCircle(
                color = Color.LightGray.copy(alpha = 0.3f),
                radius = innerRadius,
                style = Stroke(width = strokeWidth)
            )
        } else {
            data.values.forEachIndexed { index, value ->
                val sweepAngle = (value / total) * 360f
                drawArc(
                    color = colors[index % colors.size],
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                )
                startAngle += sweepAngle
            }
        }
    }
}

@Composable
fun CategoryLegend(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(12.dp).background(color, RoundedCornerShape(2.dp)))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.width(4.dp))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun WeeklyComparisonChart(thisWeek: List<Int>, lastWeek: List<Int>, modifier: Modifier = Modifier) {
    val maxVal = (thisWeek + lastWeek).maxOrNull()?.coerceAtLeast(1) ?: 1
    
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val stepX = width / 6
        
        val thisWeekPath = Path().apply {
            thisWeek.forEachIndexed { i, value ->
                val x = i * stepX
                val y = height - (value.toFloat() / maxVal * height)
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
        
        val lastWeekPath = Path().apply {
            lastWeek.forEachIndexed { i, value ->
                val x = i * stepX
                val y = height - (value.toFloat() / maxVal * height)
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
        
        drawPath(
            path = lastWeekPath,
            color = Color(0xFF4C6EF5).copy(alpha = 0.3f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
        drawPath(
            path = thisWeekPath,
            color = Color(0xFF4C6EF5),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
fun PriorityHeatmap(data: Map<String, Map<Priority, Int>>, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.width(80.dp))
            Priority.entries.forEach { p ->
                Text(
                    p.name.lowercase().replaceFirstChar { it.uppercase() },
                    modifier = Modifier.weight(1f), 
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        data.forEach { (category, priorities) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    category, 
                    modifier = Modifier.width(80.dp), 
                    fontSize = 12.sp, 
                    maxLines = 1, 
                    overflow = TextOverflow.Ellipsis
                )
                Priority.entries.forEach { p ->
                    val count = priorities[p] ?: 0
                    val alpha = if (count > 0) (0.2f + (count.toFloat() / 5).coerceAtMost(0.8f)) else 0.05f
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(30.dp)
                            .padding(2.dp)
                            .background(Color(0xFF4C6EF5).copy(alpha = alpha), RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (count > 0) Text(count.toString(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun HourlyActivityChart(data: Map<Int, Int>, modifier: Modifier = Modifier) {
    val maxVal = data.values.maxOrNull()?.coerceAtLeast(1) ?: 1
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
        (0..23).forEach { hour ->
            val count = data[hour] ?: 0
            val barHeight = (count.toFloat() / maxVal).coerceAtLeast(0.05f)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(barHeight)
                    .background(
                        if (count > 0) Color(0xFF4C6EF5) else Color.LightGray.copy(alpha = 0.2f),
                        RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)
                    )
            )
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(12.dp).background(color, RoundedCornerShape(2.dp)))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun BarChart(data: List<Pair<String, Int>>, modifier: Modifier = Modifier) {
    val maxVal = (data.maxByOrNull { it.second }?.second ?: 1).coerceAtLeast(1)
    var selectedBarIndex by remember { mutableStateOf<Int?>(null) }
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEachIndexed { index, (day, count) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { selectedBarIndex = index }
            ) {
                if (selectedBarIndex == index) {
                    Text(
                        text = count.toString(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SketchPrimary
                    )
                }
                val barHeight = (count.toFloat() / maxVal)
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .fillMaxHeight(barHeight.coerceAtLeast(0.05f))
                        .background(
                            if (selectedBarIndex == index) SketchPrimary 
                            else if (count > 0) Color(0xFF4C6EF5) 
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                            RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                        )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    day, 
                    fontSize = 10.sp, 
                    color = if (selectedBarIndex == index) SketchPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (selectedBarIndex == index) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
