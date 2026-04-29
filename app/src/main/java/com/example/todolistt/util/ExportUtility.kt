package com.example.todolistt.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.example.todolistt.data.local.Task
import com.example.todolistt.data.local.TaskStatus
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

object ExportUtility {

    fun exportTasksToCsv(context: Context, tasks: List<Task>, uri: android.net.Uri) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                val header = "Title,Description,Category,Priority,Status,Created At,Target Date\n"
                out.write(header.toByteArray())
                
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                
                tasks.forEach { task ->
                    val row = "\"${task.title}\",\"${task.description}\",\"${task.category}\",\"${task.priority}\",\"${task.status}\",\"${dateFormat.format(Date(task.createdAt))}\",\"${task.targetDate?.let { dateFormat.format(Date(it)) } ?: ""}\"\n"
                    out.write(row.toByteArray())
                }
            }
            Toast.makeText(context, "CSV exported successfully", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to export CSV: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun exportTasksToPdf(context: Context, tasks: List<Task>, uri: android.net.Uri) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        val titlePaint = Paint().apply {
            textSize = 18f
            isFakeBoldText = true
        }
        val textPaint = Paint().apply {
            textSize = 12f
        }

        var y = 40f
        canvas.drawText("Task Report", 20f, y, titlePaint)
        y += 30f
        
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        tasks.forEach { task ->
            if (y > 800) {
                pdfDocument.finishPage(page)
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                y = 40f
            }

            val status = if (task.status == TaskStatus.COMPLETED) "[x]" else "[ ]"
            canvas.drawText("$status ${task.title}", 20f, y, textPaint)
            y += 15f
            canvas.drawText("Category: ${task.category} | Priority: ${task.priority}", 40f, y, textPaint)
            y += 15f
            task.targetDate?.let {
                canvas.drawText("Due: ${dateFormat.format(Date(it))}", 40f, y, textPaint)
                y += 15f
            }
            y += 10f
        }

        pdfDocument.finishPage(page)
        
        try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                pdfDocument.writeTo(out)
            }
            Toast.makeText(context, "PDF exported successfully", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to export PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
        }
    }

    fun exportTasksToCsv(context: Context, tasks: List<Task>) {
        val fileName = "tasks_report_${System.currentTimeMillis()}.csv"
        
        try {
            val outputStream: OutputStream?
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                outputStream = uri?.let { context.contentResolver.openOutputStream(it) }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val file = File(downloadsDir, fileName)
                outputStream = FileOutputStream(file)
            }

            outputStream?.use { out ->
                val header = "Title,Description,Category,Priority,Status,Created At,Target Date\n"
                out.write(header.toByteArray())
                
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                
                tasks.forEach { task ->
                    val row = "\"${task.title}\",\"${task.description}\",\"${task.category}\",\"${task.priority}\",\"${task.status}\",\"${dateFormat.format(Date(task.createdAt))}\",\"${task.targetDate?.let { dateFormat.format(Date(it)) } ?: ""}\"\n"
                    out.write(row.toByteArray())
                }
            }
            Toast.makeText(context, "CSV exported to Downloads folder", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to export CSV: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun exportTasksToPdf(context: Context, tasks: List<Task>) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        val titlePaint = Paint().apply {
            textSize = 18f
            isFakeBoldText = true
        }
        val textPaint = Paint().apply {
            textSize = 12f
        }

        var y = 40f
        canvas.drawText("Task Report", 20f, y, titlePaint)
        y += 30f
        
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        tasks.forEach { task ->
            if (y > 800) {
                pdfDocument.finishPage(page)
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                y = 40f
            }

            val status = if (task.status == TaskStatus.COMPLETED) "[x]" else "[ ]"
            canvas.drawText("$status ${task.title}", 20f, y, textPaint)
            y += 15f
            canvas.drawText("Category: ${task.category} | Priority: ${task.priority}", 40f, y, textPaint)
            y += 15f
            task.targetDate?.let {
                canvas.drawText("Due: ${dateFormat.format(Date(it))}", 40f, y, textPaint)
                y += 15f
            }
            y += 10f
        }

        pdfDocument.finishPage(page)

        val fileName = "tasks_report_${System.currentTimeMillis()}.pdf"
        
        try {
            val outputStream: OutputStream?
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                outputStream = uri?.let { context.contentResolver.openOutputStream(it) }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val file = File(downloadsDir, fileName)
                outputStream = FileOutputStream(file)
            }

            outputStream?.use { out ->
                pdfDocument.writeTo(out)
            }
            Toast.makeText(context, "PDF exported to Downloads folder", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to export PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
        }
    }
}
