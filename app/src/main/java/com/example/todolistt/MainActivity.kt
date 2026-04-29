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
import android.speech.RecognizerIntent
import androidx.activity.result.ActivityResultLauncher
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val database by lazy { TaskDatabase.getDatabase(this) }
    private val repository by lazy { TaskRepository(database.taskDao()) }
    private val categoryRepository by lazy { CategoryRepository(database.categoryDao()) }
    private val themeRepository by lazy { ThemeRepository(this) }
    
    private val viewModel: TaskViewModel by viewModels {
        TaskViewModelFactory(application, repository, categoryRepository, themeRepository)
    }

    private var initialTaskIdFromWidget = mutableStateOf<Int?>(null)
    private var showAddTaskDialog = mutableStateOf(false)
    private var voiceTaskTitle = mutableStateOf<String?>(null)
    private var voiceTaskPriority = mutableStateOf<com.example.todolistt.data.local.Priority?>(null)
    private var voiceTaskDate = mutableStateOf<Long?>(null)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            // Explain to the user that notifications are needed
        }
    }

    private val speechRecognizerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = results?.get(0)
            if (!spokenText.isNullOrBlank()) {
                parseVoiceInput(spokenText)
                showAddTaskDialog.value = true
            }
        }
    }

    private fun parseVoiceInput(text: String) {
        var cleanText = text
        
        // Priority Parsing
        val priority = when {
            text.contains("high priority", ignoreCase = true) || text.contains("important", ignoreCase = true) -> {
                cleanText = cleanText.replace("high priority", "", ignoreCase = true).replace("important", "", ignoreCase = true)
                com.example.todolistt.data.local.Priority.HIGH
            }
            text.contains("medium priority", ignoreCase = true) -> {
                cleanText = cleanText.replace("medium priority", "", ignoreCase = true)
                com.example.todolistt.data.local.Priority.MEDIUM
            }
            text.contains("low priority", ignoreCase = true) -> {
                cleanText = cleanText.replace("low priority", "", ignoreCase = true)
                com.example.todolistt.data.local.Priority.LOW
            }
            else -> null
        }

        // Date Parsing (Today/Tomorrow)
        val calendar = java.util.Calendar.getInstance()
        var date: Long? = null
        
        if (text.contains("today", ignoreCase = true)) {
            cleanText = cleanText.replace("today", "", ignoreCase = true)
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            date = calendar.timeInMillis
        } else if (text.contains("tomorrow", ignoreCase = true)) {
            cleanText = cleanText.replace("tomorrow", "", ignoreCase = true)
            calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            date = calendar.timeInMillis
        }

        voiceTaskTitle.value = cleanText.trim().replaceFirstChar { it.uppercase() }
        voiceTaskPriority.value = priority
        voiceTaskDate.value = date
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
                    var openAddTask by showAddTaskDialog
                    val voiceTitle by voiceTaskTitle
                    val voicePriority by voiceTaskPriority
                    val voiceDate by voiceTaskDate
                    
                    when (currentScreen) {
                        "tasks" -> {
                            TaskScreen(
                                viewModel = viewModel,
                                onNavigateToTasks = { currentScreen = "tasks" },
                                onNavigateToHistory = { currentScreen = "history" },
                                onNavigateToDashboard = { currentScreen = "dashboard" },
                                onNavigateToArchive = { currentScreen = "archive" },
                                initialTaskId = taskIdToOpen,
                                initialAddTask = openAddTask,
                                voiceTitle = voiceTitle,
                                voicePriority = voicePriority,
                                voiceDate = voiceDate,
                                onVoiceComplete = { 
                                    voiceTaskTitle.value = null
                                    voiceTaskPriority.value = null
                                    voiceTaskDate.value = null
                                },
                                onStartVoiceInput = { startVoiceInput() }
                            )
                            // Clear it after passing to TaskScreen
                            if (taskIdToOpen != null || openAddTask) {
                                LaunchedEffect(Unit) {
                                    initialTaskIdFromWidget.value = null
                                    showAddTaskDialog.value = false
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
        if (intent?.action == "ADD_TASK") {
            showAddTaskDialog.value = true
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

    private fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak the task title")
        }
        try {
            speechRecognizerLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Voice recognition not available", Toast.LENGTH_SHORT).show()
        }
    }
}
