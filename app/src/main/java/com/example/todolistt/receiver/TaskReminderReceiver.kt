package com.example.todolistt.receiver

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.todolistt.MainActivity
import com.example.todolistt.R

class TaskReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra("TASK_ID", 0)
        val taskTitle = intent.getStringExtra("TASK_TITLE") ?: "Task Reminder"
        val snoozeCount = intent.getIntExtra("SNOOZE_COUNT", 0)
        val action = intent.action

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (action == "com.example.todolistt.DISMISS") {
            notificationManager.cancel(taskId)
            return
        }

        if (action == "com.example.todolistt.SNOOZE") {
            notificationManager.cancel(taskId)
            if (snoozeCount < 3) {
                scheduleSnooze(context, taskId, taskTitle, snoozeCount + 1)
            }
            return
        }
        
        val channelId = "task_reminders_high"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, 
                "Task Reminders (High Importance)", 
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for upcoming tasks with sound and popup"
                enableLights(true)
                enableVibration(true)
                // To use custom sound:
                // val soundUri = Uri.parse("android.resource://${context.packageName}/raw/alarm_sound")
                // setSound(soundUri, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build())
            }
            notificationManager.createNotificationChannel(channel)
        }

        val activityIntent = Intent(context, MainActivity::class.java).apply {
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
            action = "com.example.todolistt.SNOOZE"
            putExtra("TASK_ID", taskId)
            putExtra("TASK_TITLE", taskTitle)
            putExtra("SNOOZE_COUNT", snoozeCount)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context, taskId + 1000, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Dismiss Action
        val dismissIntent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = "com.example.todolistt.DISMISS"
            putExtra("TASK_ID", taskId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context, taskId + 2000, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm) // Change to your app logo later
            .setContentTitle("ToDoList - Task Reminder")
            .setContentText(taskTitle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setFullScreenIntent(pendingIntent, true) // Makes it pop up
            .setContentIntent(pendingIntent)
            .addAction(0, "Snooze", snoozePendingIntent)
            .addAction(0, "Close", dismissPendingIntent)
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
        val triggerTime = System.currentTimeMillis() + (5 * 60 * 1000) // 5 minutes
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                return
            }
        }
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
    }
}
