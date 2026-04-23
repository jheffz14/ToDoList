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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
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
import com.example.todolistt.ui.theme.SketchPrimary
import com.example.todolistt.ui.viewmodel.TaskViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: TaskViewModel, onBack: () -> Unit) {
    val stats by viewModel.stats.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()
    
    var timeFilter by remember { mutableStateOf("All") }
    var showTimeFilterMenu by remember { mutableStateOf(false) }
    var showManageCategoriesGlobal by remember { mutableStateOf(false) }

    val monthName = remember(selectedMonth) {
        val cal = Calendar.getInstance().apply { set(Calendar.MONTH, selectedMonth) }
        SimpleDateFormat("MMMM", Locale.getDefault()).format(cal.time)
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
                    onClick = onBack,
                    icon = { Icon(Icons.AutoMirrored.Outlined.List, contentDescription = "Tasks") },
                    label = { Text("Tasks") }
                )
                NavigationBarItem(
                    selected = true,
                    onClick = { /* Stay here */ },
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
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "DASHBOARD",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )

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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = SketchPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("$monthName $selectedYear", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
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
                    IconButton(onClick = {
                        val today = Calendar.getInstance()
                        viewModel.setDateFilter(
                            today.get(Calendar.MONTH),
                            today.get(Calendar.YEAR)
                        )
                    }) {
                        Icon(Icons.Default.Today, contentDescription = "Today", tint = SketchPrimary)
                    }
                }
            }

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

            // Category Donut Chart Section
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
                                Text(timeFilter, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            DropdownMenu(expanded = showTimeFilterMenu, onDismissRequest = { showTimeFilterMenu = false }) {
                                listOf("In 7 days", "In 30 days", "All").forEach { filter ->
                                    DropdownMenuItem(
                                        text = { Text(filter) },
                                        onClick = {
                                            timeFilter = filter
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
                        Text("Daily task complete", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("All", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    BarChart(
                        data = stats.dailyCompletion,
                        modifier = Modifier.height(150.dp).fillMaxWidth()
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
