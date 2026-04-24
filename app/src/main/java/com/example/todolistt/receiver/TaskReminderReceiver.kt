package com.example.todolistt.receiver

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.todolistt.MainActivity
import com.example.todolistt.data.local.TaskDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaskReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra("TASK_ID", -1)
        val taskTitle = intent.getStringExtra("TASK_TITLE") ?: "Task Reminder"
        val snoozeCount = intent.getIntExtra("SNOOZE_COUNT", 0)
        val action = intent.action

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (action == Intent.ACTION_BOOT_COMPLETED) {
            // Reschedule alarms if needed, but don't show a notification
            return
        }

        if (action == "com.example.todolistt.DISMISS") {
            notificationManager.cancel(taskId)
            return
        }

        if (action == "com.example.todolistt.SNOOZE") {
            notificationManager.cancel(taskId)
            if (snoozeCount < 5) {
                scheduleSnooze(context, taskId, taskTitle, snoozeCount + 1)
            }
            return
        }

        if (taskId == -1) return

        // Verify task still exists and isn't completed before showing notification
        val database = TaskDatabase.getDatabase(context)
        val goAsync = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val task = database.taskDao().getTaskById(taskId)
                if (task != null && !task.isCompleted) {
                    showNotification(context, taskId, task.title, snoozeCount)
                }
            } finally {
                goAsync.finish()
            }
        }
    }

    private fun showNotification(context: Context, taskId: Int, taskTitle: String, snoozeCount: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "task_reminders_v2"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, 
                "Task Reminders (Priority)", 
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High importance notifications that pop up"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
                setBypassDnd(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }

        val activityIntent = Intent(context, MainActivity::class.java).apply {
            action = "OPEN_TASK"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("TASK_ID", taskId)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 
            taskId, 
            activityIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze Action
        val snoozeIntent = Intent(context, TaskReminderReceiver::class.java).apply {
            setAction("com.example.todolistt.SNOOZE")
            putExtra("TASK_ID", taskId)
            putExtra("TASK_TITLE", taskTitle)
            putExtra("SNOOZE_COUNT", snoozeCount)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context, taskId + 1000, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Dismiss Action
        val dismissIntent = Intent(context, TaskReminderReceiver::class.java).apply {
            setAction("com.example.todolistt.DISMISS")
            putExtra("TASK_ID", taskId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context, taskId + 2000, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Task Reminder: $taskTitle")
            .setContentText("Tap to view details")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setOngoing(true)
            .setFullScreenIntent(pendingIntent, true)
            .setContentIntent(pendingIntent)
            .addAction(0, "Snooze (10m)", snoozePendingIntent)
            .addAction(0, "Dismiss", dismissPendingIntent)
            .setDeleteIntent(dismissPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        notificationManager.notify(taskId, notification)
    }

    private fun scheduleSnooze(context: Context, taskId: Int, title: String, count: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            putExtra("TASK_ID", taskId)
            putExtra("TASK_TITLE", title)
            putExtra("SNOOZE_COUNT", count)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, taskId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerTime = System.currentTimeMillis() + (10 * 60 * 1000) // 10 minutes snooze
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                return
            }
        }
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
    }
}
