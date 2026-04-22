package com.example.todolistt.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
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
    var timeFilter by remember { mutableStateOf("All") }
    var showTimeFilterMenu by remember { mutableStateOf(false) }

    // Date range for filtering
    var currentYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var currentMonth by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH)) }
    val monthName = remember(currentMonth) {
        val cal = Calendar.getInstance().apply { set(Calendar.MONTH, currentMonth) }
        SimpleDateFormat("MMMM", Locale.getDefault()).format(cal.time)
    }

    Scaffold(
        containerColor = Color(0xFFF1F3F5), // Light grey background like in the image
        topBar = {
            TopAppBar(
                title = { Text("DASHBOARD", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Month/Year Filter UI
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
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
                        Text("$monthName $currentYear", fontWeight = FontWeight.Bold)
                    }
                    Row {
                        IconButton(onClick = {
                            if (currentMonth == 0) {
                                currentMonth = 11
                                currentYear--
                            } else {
                                currentMonth--
                            }
                            viewModel.setDateFilter(currentMonth, currentYear)
                        }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Prev")
                        }
                        IconButton(onClick = {
                            if (currentMonth == 11) {
                                currentMonth = 0
                                currentYear++
                            } else {
                                currentMonth++
                            }
                            viewModel.setDateFilter(currentMonth, currentYear)
                        }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next")
                        }
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
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Open Tasks in Categories", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Box {
                            TextButton(onClick = { showTimeFilterMenu = true }) {
                                Text(timeFilter, color = Color.Gray)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray)
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
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Daily task complete", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("All", color = Color.Gray, fontSize = 12.sp)
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
    }
}

@Composable
fun StatCardImproved(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
            Text(label, fontSize = 12.sp, color = Color.Gray)
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
        Text(label, fontSize = 14.sp, color = Color.DarkGray)
        Spacer(modifier = Modifier.width(4.dp))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun BarChart(data: List<Pair<String, Int>>, modifier: Modifier = Modifier) {
    val maxVal = (data.maxByOrNull { it.second }?.second ?: 1).coerceAtLeast(1)
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEach { (day, count) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val barHeight = (count.toFloat() / maxVal)
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .fillMaxHeight(barHeight.coerceAtLeast(0.05f))
                        .background(
                            if (count > 0) Color(0xFF4C6EF5) else Color(0xFFD0EBFF),
                            RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                        )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(day, fontSize = 10.sp, color = Color.Gray)
            }
        }
    }
}

