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
import com.example.todolistt.ui.screens.HistoryScreen
import com.example.todolistt.ui.screens.TaskScreen
import com.example.todolistt.ui.theme.ToDoListtTheme
import com.example.todolistt.ui.viewmodel.TaskViewModel
import com.example.todolistt.ui.viewmodel.TaskViewModelFactory
import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.widget.Toast

class MainActivity : ComponentActivity() {
    private val database by lazy { TaskDatabase.getDatabase(this) }
    private val repository by lazy { TaskRepository(database.taskDao()) }
    private val categoryRepository by lazy { CategoryRepository(database.categoryDao()) }
    private val themeRepository by lazy { ThemeRepository(this) }
    
    private val viewModel: TaskViewModel by viewModels {
        TaskViewModelFactory(application, repository, categoryRepository, themeRepository)
    }

    private var initialTaskIdFromWidget = mutableStateOf<Int?>(null)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            // Explain to the user that notifications are needed
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        checkPermissions()
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
                                onNavigateToTasks = { currentScreen = "tasks" },
                                onNavigateToHistory = { currentScreen = "history" },
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
                        "history" -> {
                            HistoryScreen(
                                viewModel = viewModel,
                                onNavigateToTasks = { currentScreen = "tasks" },
                                onNavigateToDashboard = { currentScreen = "dashboard" }
                            )
                        }
                        "dashboard" -> {
                            DashboardScreen(
                                viewModel = viewModel,
                                onNavigateToTasks = { currentScreen = "tasks" },
                                onNavigateToHistory = { currentScreen = "history" },
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
        setIntent(intent) // Important: update the intent so handleIntent can see the new TASK_ID
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val taskId = intent?.getIntExtra("TASK_ID", -1) ?: -1
        if (taskId != -1) {
            initialTaskIdFromWidget.value = taskId
        }
    }

    private fun checkPermissions() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        // 1. Request Notification Permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // 2. Check Channel Importance (Force Heads-up)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = notificationManager.getNotificationChannel("task_reminders_v2")
            if (channel != null && channel.importance < android.app.NotificationManager.IMPORTANCE_HIGH) {
                // The user downgraded the channel importance. Force them to fix it.
                val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                    putExtra(Settings.EXTRA_CHANNEL_ID, "task_reminders_v2")
                }
                startActivity(intent)
                Toast.makeText(this, "Please enable 'High Importance' for Pop-up notifications", Toast.LENGTH_LONG).show()
            }
        }

        // 3. Check Exact Alarm Permission (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent().apply {
                    action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }
    }
}
