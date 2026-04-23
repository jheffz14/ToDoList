package com.example.todolistt.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.example.todolistt.data.local.Task
import com.example.todolistt.receiver.TaskReminderReceiver

object ReminderManager {
    fun scheduleReminder(context: Context, task: Task) {
        val reminderTime = getReminderTime(task)
        if (reminderTime < System.currentTimeMillis()) return

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

    private fun getReminderTime(task: Task): Long {
        return task.startTime?.let { startTime ->
             task.targetDate!! + startTime
        } ?: task.targetDate!!
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
