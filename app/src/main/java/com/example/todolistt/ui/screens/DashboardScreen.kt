package com.example.todolistt.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todolistt.ui.theme.SketchBlack
import com.example.todolistt.ui.theme.SketchPaper
import com.example.todolistt.ui.theme.SketchPrimary
import com.example.todolistt.ui.viewmodel.TaskViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: TaskViewModel, onBack: () -> Unit) {
    val stats by viewModel.stats.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("DASHBOARD", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Summary Cards
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatCard(label = "Total", value = stats.totalTasks.toString(), modifier = Modifier.weight(1f))
                StatCard(label = "Done", value = stats.completedTasks.toString(), modifier = Modifier.weight(1f))
            }

            SketchChart(
                title = "DAILY PROGRESS",
                data = listOf(0.2f, 0.5f, 0.3f, 0.8f, 0.6f, 0.9f, stats.completionRate),
                modifier = Modifier.height(200.dp)
            )

            SketchChart(
                title = "MONTHLY TREND",
                data = listOf(0.4f, 0.6f, 0.5f, 0.7f, 0.9f, 0.8f, 0.75f, 0.85f, 0.6f, 0.7f, 0.8f, 0.9f),
                modifier = Modifier.height(200.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.border(2.dp, MaterialTheme.colorScheme.onBackground, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = SketchPrimary)
        }
    }
}

@Composable
fun SketchChart(title: String, data: List<Float>, modifier: Modifier = Modifier) {
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    Column(modifier = modifier) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                .border(2.dp, MaterialTheme.colorScheme.onBackground, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            val width = size.width
            val height = size.height
            val spacing = if (data.size > 1) width / (data.size - 1) else 0f

            // Draw Grid Lines (Sketchy)
            for (i in 0..4) {
                val y = height - (height / 4 * i)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )
            }

            // Draw Path
            val path = Path().apply {
                data.forEachIndexed { index, value ->
                    val x = index * spacing
                    val y = height - (value * height)
                    if (index == 0) moveTo(x, y) else lineTo(x, y)
                }
            }

            drawPath(
                path = path,
                color = SketchPrimary,
                style = Stroke(width = 4.dp.toPx())
            )

            // Draw Points
            data.forEachIndexed { index, value ->
                val x = index * spacing
                val y = height - (value * height)
                drawCircle(
                    color = SketchPrimary,
                    radius = 4.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }
    }
}
