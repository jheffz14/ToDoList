package com.example.todolistt

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.todolistt.data.local.TaskDatabase
import com.example.todolistt.data.repository.CategoryRepository
import com.example.todolistt.data.repository.TaskRepository
import com.example.todolistt.data.repository.ThemeRepository
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import com.example.todolistt.ui.screens.DashboardScreen
import com.example.todolistt.ui.screens.ArchiveScreen
import com.example.todolistt.ui.screens.TaskScreen
import com.example.todolistt.ui.theme.ToDoListtTheme
import com.example.todolistt.ui.viewmodel.TaskViewModel
import com.example.todolistt.ui.viewmodel.TaskViewModelFactory

class MainActivity : ComponentActivity() {
    private val database by lazy { TaskDatabase.getDatabase(this) }
    private val repository by lazy { TaskRepository(database.taskDao()) }
    private val categoryRepository by lazy { CategoryRepository(database.categoryDao()) }
    private val themeRepository by lazy { ThemeRepository(this) }
    
    private val viewModel: TaskViewModel by viewModels {
        TaskViewModelFactory(application, repository, categoryRepository, themeRepository)
    }

    private var initialTaskIdFromWidget = mutableStateOf<Int?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        handleIntent(intent)

        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            
            // This ensures status bar icons change color (Black vs White) when theme changes
            DisposableEffect(isDarkMode) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    ) { isDarkMode }
                )
                onDispose {}
            }

            ToDoListtTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf("tasks") }
                    val taskIdToOpen by initialTaskIdFromWidget
                    
                    when (currentScreen) {
                        "tasks" -> {
                            TaskScreen(
                                viewModel = viewModel,
                                onNavigateToDashboard = { currentScreen = "dashboard" },
                                onNavigateToArchive = { currentScreen = "archive" },
                                initialTaskId = taskIdToOpen
                            )
                            // Clear it after passing to TaskScreen
                            if (taskIdToOpen != null) {
                                LaunchedEffect(Unit) {
                                    initialTaskIdFromWidget.value = null
                                }
                            }
                        }
                        "dashboard" -> {
                            DashboardScreen(
                                viewModel = viewModel,
                                onBack = { currentScreen = "tasks" }
                            )
                        }
                        "archive" -> {
                            ArchiveScreen(
                                viewModel = viewModel,
                                onBack = { currentScreen = "tasks" }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val taskId = intent?.getIntExtra("TASK_ID", -1) ?: -1
        if (taskId != -1) {
            initialTaskIdFromWidget.value = taskId
        }
    }
}