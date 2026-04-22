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
import com.example.todolistt.data.local.TaskDatabase
import com.example.todolistt.data.repository.TaskRepository
import com.example.todolistt.ui.screens.TaskScreen
import com.example.todolistt.ui.theme.ToDoListtTheme
import com.example.todolistt.ui.viewmodel.TaskViewModel
import com.example.todolistt.ui.viewmodel.TaskViewModelFactory

class MainActivity : ComponentActivity() {
    private val database by lazy { TaskDatabase.getDatabase(this) }
    private val repository by lazy { TaskRepository(database.taskDao()) }
    private val viewModel: TaskViewModel by viewModels {
        TaskViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ToDoListtTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TaskScreen(viewModel = viewModel)
                }
            }
        }
    }
}