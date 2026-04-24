package com.example.todolistt.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.example.todolistt.data.local.Task
import com.example.todolistt.receiver.TaskReminderReceiver
import java.util.Calendar
import java.util.Date

object ReminderManager {
    fun scheduleReminder(context: Context, task: Task) {
        val reminderTime = getReminderTime(task)
        val currentTime = System.currentTimeMillis()
        
        // Log for debugging (in real app use Timber or Log.d)
        android.util.Log.d("ReminderManager", "Scheduling task ${task.title} for ${Date(reminderTime)}")
        
        // Allow a 1-minute grace period for "Now" tasks to prevent skipping due to millisecond differences
        if (reminderTime < currentTime - 60000) {
            android.util.Log.d("ReminderManager", "Time ${Date(reminderTime)} is more than 1 minute in the past, skipping.")
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                scheduleInexact(context, task, alarmManager, reminderTime)
                return
            }
        }
        scheduleExact(context, task, alarmManager, reminderTime)
    }

    private fun scheduleExact(context: Context, task: Task, alarmManager: AlarmManager, reminderTime: Long) {
        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            putExtra("TASK_ID", task.id)
            putExtra("TASK_TITLE", task.title)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            reminderTime,
            pendingIntent
        )
    }

    private fun scheduleInexact(context: Context, task: Task, alarmManager: AlarmManager, reminderTime: Long) {
        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            putExtra("TASK_ID", task.id)
            putExtra("TASK_TITLE", task.title)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            reminderTime,
            pendingIntent
        )
    }

    fun getReminderTime(task: Task): Long {
        val baseDate = task.targetDate ?: return System.currentTimeMillis() // Fallback
        val startTime = task.startTime ?: 0L
        
        // Ensure the baseDate is at exactly 00:00:00:000 of the target day
        val cal = Calendar.getInstance()
        cal.timeInMillis = baseDate
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        
        return cal.timeInMillis + startTime
    }

    fun cancelReminder(context: Context, task: Task) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, TaskReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
