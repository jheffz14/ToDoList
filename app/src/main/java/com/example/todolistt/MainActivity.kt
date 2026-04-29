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
    private var voiceTaskEndDate = mutableStateOf<Long?>(null)
    private var voiceTaskStartTime = mutableStateOf<Long?>(null)
    private var voiceTaskEndTime = mutableStateOf<Long?>(null)
    private var voiceTaskCategory = mutableStateOf<String?>(null)
    private var voiceTaskRecurrence = mutableStateOf<com.example.todolistt.data.local.RecurrenceType?>(null)

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
        
        // 1. Priority Parsing (Default to LOW)
        val priority = when {
            text.contains("high", ignoreCase = true) || text.contains("urgent", ignoreCase = true) || text.contains("important", ignoreCase = true) -> {
                cleanText = cleanText.replace("(?i)high priority|high|urgent|important".toRegex(), "")
                com.example.todolistt.data.local.Priority.HIGH
            }
            text.contains("medium", ignoreCase = true) -> {
                cleanText = cleanText.replace("(?i)medium priority|medium".toRegex(), "")
                com.example.todolistt.data.local.Priority.MEDIUM
            }
            text.contains("low", ignoreCase = true) -> {
                cleanText = cleanText.replace("(?i)low priority|low".toRegex(), "")
                com.example.todolistt.data.local.Priority.LOW
            }
            else -> com.example.todolistt.data.local.Priority.LOW
        }

        // 2. Recurrence Parsing
        var recurrence: com.example.todolistt.data.local.RecurrenceType? = null
        if (text.contains("daily", ignoreCase = true) || text.contains("every day", ignoreCase = true)) {
            recurrence = com.example.todolistt.data.local.RecurrenceType.DAILY
            cleanText = cleanText.replace("(?i)daily|every day".toRegex(), "")
        }

        // 3. Date Parsing (Today/Tomorrow/Specific Date)
        val calendar = java.util.Calendar.getInstance()
        var date: Long? = null
        var endDate: Long? = null
        
        // Tomorrow/Today/Now
        if (text.contains("tomorrow", ignoreCase = true)) {
            cleanText = cleanText.replace("tomorrow", "", ignoreCase = true)
            calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
            date = calendar.apply { set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0) }.timeInMillis
        } else if (text.contains("today", ignoreCase = true) || text.contains("now", ignoreCase = true)) {
            cleanText = cleanText.replace("(?i)today|now".toRegex(), "")
            date = calendar.apply { set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0) }.timeInMillis
        }

        // Specific Date detection (e.g., "May 3") - avoid "to May 3"
        val monthNames = "Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?"
        val specificDateRegex = "(?<!to\\s|until\\s)\\b($monthNames)\\s+(\\d{1,2})\\b".toRegex(RegexOption.IGNORE_CASE)
        val specificDateMatch = specificDateRegex.find(text)
        if (specificDateMatch != null) {
            val monthStr = specificDateMatch.groupValues[1]
            val day = specificDateMatch.groupValues[2].toInt()
            val monthIdx = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                .indexOfFirst { monthStr.startsWith(it, ignoreCase = true) }
            
            val dateCal = java.util.Calendar.getInstance()
            dateCal.set(java.util.Calendar.MONTH, monthIdx)
            dateCal.set(java.util.Calendar.DAY_OF_MONTH, day)
            date = dateCal.apply { set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0) }.timeInMillis
            cleanText = cleanText.replace(specificDateMatch.value, "", ignoreCase = true)
        }

        // End Date detection "to May 3" or "until May 3"
        val endDateRegex = "(?i)(?:to|until)\\s+($monthNames)\\s+(\\d{1,2})".toRegex()
        val dateMatch = endDateRegex.find(text)
        if (dateMatch != null) {
            val monthStr = dateMatch.groupValues[1]
            val day = dateMatch.groupValues[2].toInt()
            val monthIdx = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                .indexOfFirst { monthStr.startsWith(it, ignoreCase = true) }
            
            val endCal = java.util.Calendar.getInstance()
            endCal.set(java.util.Calendar.MONTH, monthIdx)
            endCal.set(java.util.Calendar.DAY_OF_MONTH, day)
            endDate = endCal.apply { set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0) }.timeInMillis
            cleanText = cleanText.replace(dateMatch.value, "", ignoreCase = true)
        }

        // 4. Category Parsing
        var category: String? = null
        val categoryWords = listOf("Personal", "Work", "Meeting", "Others", "Exercise", "Health", "Fitness", "Shopping", "Finance", "Education", "Travel", "Hobbies")
        for (word in categoryWords) {
            if (text.contains(word, ignoreCase = true)) {
                category = word
                cleanText = cleanText.replace(word, "", ignoreCase = true)
                cleanText = cleanText.replace("(?i)(?:for|in|category)\\s+".toRegex(), "")
                break
            }
        }

        // 5. Time Range Parsing (at 9 AM to 10 AM)
        var startTime: Long? = null
        var endTime: Long? = null
        
        fun parseSingleTime(h: Int, m: Int, amPm: String?): Long {
            var hour = h
            if (amPm?.uppercase() == "PM" && hour < 12) hour += 12
            if (amPm?.uppercase() == "AM" && hour == 12) hour = 0
            return (hour * 3600000L) + (m * 60000L)
        }

        // Expanded regex to catch "at 5 AM to 7 AM" or "at 5 to 7 PM"
        val timeRangeRegex = "(?i)at (\\d{1,2})(:(\\d{2}))?\\s*(AM|PM)?\\s*(?:to|until|-)\\s*(\\d{1,2})(:(\\d{2}))?\\s*(AM|PM)?".toRegex()
        val rangeMatch = timeRangeRegex.find(cleanText) // Use cleanText
        
        if (rangeMatch != null) {
            // Handle AM/PM logic for start time if not explicitly provided
            var startAmPm = rangeMatch.groupValues[4]
            val endAmPm = rangeMatch.groupValues[8]
            if (startAmPm.isEmpty() && endAmPm.isNotEmpty()) {
                startAmPm = endAmPm
            }

            startTime = parseSingleTime(rangeMatch.groupValues[1].toInt(), if (rangeMatch.groupValues[3].isNotEmpty()) rangeMatch.groupValues[3].toInt() else 0, startAmPm)
            endTime = parseSingleTime(rangeMatch.groupValues[5].toInt(), if (rangeMatch.groupValues[7].isNotEmpty()) rangeMatch.groupValues[7].toInt() else 0, endAmPm.ifEmpty { startAmPm })
            cleanText = cleanText.replace(rangeMatch.value, "", ignoreCase = true)
        } else {
            val singleTimeRegex = "(?i)at (\\d{1,2})(:(\\d{2}))?\\s*(AM|PM)?".toRegex()
            val singleMatch = singleTimeRegex.find(cleanText) // Use cleanText
            if (singleMatch != null) {
                startTime = parseSingleTime(singleMatch.groupValues[1].toInt(), if (singleMatch.groupValues[3].isNotEmpty()) singleMatch.groupValues[3].toInt() else 0, singleMatch.groupValues[4])
                cleanText = cleanText.replace(singleMatch.value, "", ignoreCase = true)
            }
        }

        voiceTaskTitle.value = cleanText.trim().replaceFirstChar { it.uppercase() }
        voiceTaskPriority.value = priority
        voiceTaskDate.value = date
        voiceTaskEndDate.value = endDate
        voiceTaskStartTime.value = startTime
        voiceTaskEndTime.value = endTime
        voiceTaskCategory.value = category
        voiceTaskRecurrence.value = recurrence
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
                    val voiceEndDate by voiceTaskEndDate
                    val voiceStartTime by voiceTaskStartTime
                    val voiceEndTime by voiceTaskEndTime
                    val voiceCategory by voiceTaskCategory
                    val voiceRecurrence by voiceTaskRecurrence
                    
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
                                voiceEndDate = voiceEndDate,
                                voiceStartTime = voiceStartTime,
                                voiceEndTime = voiceEndTime,
                                voiceCategory = voiceCategory,
                                voiceRecurrence = voiceRecurrence,
                                onVoiceComplete = { 
                                    voiceTaskTitle.value = null
                                    voiceTaskPriority.value = null
                                    voiceTaskDate.value = null
                                    voiceTaskEndDate.value = null
                                    voiceTaskStartTime.value = null
                                    voiceTaskEndTime.value = null
                                    voiceTaskCategory.value = null
                                    voiceTaskRecurrence.value = null
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
            putExtra(RecognizerIntent.EXTRA_PROMPT, "What is the task?")
            
            // Allow more time for the user to think (milliseconds)
            // Using string keys for broader compatibility
            putExtra("android.speech.extras.SPEECH_INPUT_MINIMUM_MILLIS", 3000L)
            putExtra("android.speech.extras.SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS", 2000L)
            putExtra("android.speech.extras.SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS", 2000L)
        }
        try {
            speechRecognizerLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Voice recognition not available", Toast.LENGTH_SHORT).show()
        }
    }
}
