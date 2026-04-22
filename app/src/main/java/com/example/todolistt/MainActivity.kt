package com.example.todolistt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.example.todolistt.ui.screens.DashboardScreen
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
        TaskViewModelFactory(repository, categoryRepository, themeRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            
            ToDoListtTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf("tasks") }
                    
                    if (currentScreen == "tasks") {
                        TaskScreen(
                            viewModel = viewModel,
                            onNavigateToDashboard = { currentScreen = "dashboard" }
                        )
                    } else {
                        DashboardScreen(
                            viewModel = viewModel,
                            onBack = { currentScreen = "tasks" }
                        )
                    }
                }
            }
        }
    }
}