package com.example.todolistt.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.todolistt.R
import com.example.todolistt.data.local.Task
import com.example.todolistt.data.local.TaskDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Calendar

class TaskWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return TaskWidgetFactory(applicationContext)
    }
}

class TaskWidgetFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {
    private var tasks: List<Task> = emptyList()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        val database = TaskDatabase.getDatabase(context)
        
        runBlocking {
            // Show all pending, non-archived tasks in the widget to ensure data is visible
            // We can add more specific filtering later if needed
            tasks = database.taskDao().getAllTasks().first()
                .filter { !it.isCompleted && !it.isArchived }
                .sortedWith(compareBy({ it.priority }, { it.targetDate ?: Long.MAX_VALUE }))
        }
    }

    override fun onDestroy() {}

    override fun getCount(): Int = tasks.size

    override fun getViewAt(position: Int): RemoteViews {
        val task = tasks[position]
        val views = RemoteViews(context.packageName, R.layout.widget_item)
        
        views.setTextViewText(R.id.widget_item_title, task.title)
        
        // Color coding based on priority
        val priorityColor = when (task.priority) {
            com.example.todolistt.data.local.Priority.HIGH -> 0xFFFF0000.toInt() // Red
            com.example.todolistt.data.local.Priority.MEDIUM -> 0xFFFFA500.toInt() // Orange
            com.example.todolistt.data.local.Priority.LOW -> 0xFF008000.toInt() // Green
        }
        // Using FrameLayout for priority indicator as it's more reliable in RemoteViews
        views.setInt(R.id.widget_item_priority, "setBackgroundColor", priorityColor)

        // Create a fill-in intent for the item click. 
        // We pass the task ID which can be used to open the specific task.
        val fillInIntent = Intent().apply {
            putExtra("TASK_ID", task.id)
            // Setting a data URI helps distinguish intents for different items
            data = android.net.Uri.parse("task://${task.id}")
        }
        views.setOnClickFillInIntent(R.id.widget_item_container, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = tasks[position].id.toLong()

    override fun hasStableIds(): Boolean = true
}
