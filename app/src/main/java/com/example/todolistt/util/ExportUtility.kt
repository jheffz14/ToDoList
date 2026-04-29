package com.example.todolistt.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.todolistt.data.local.Task
import com.example.todolistt.data.local.TaskStatus
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ExportUtility {

    fun exportTasksToCsv(context: Context, tasks: List<Task>) {
        val fileName = "tasks_report_${System.currentTimeMillis()}.csv"
        val file = File(context.cacheDir, fileName)
        
        try {
            FileOutputStream(file).use { out ->
                val header = "Title,Description,Category,Priority,Status,Created At,Target Date\n"
                out.write(header.toByteArray())
                
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                
                tasks.forEach { task ->
                    val row = "\"${task.title}\",\"${task.description}\",\"${task.category}\",\"${task.priority}\",\"${task.status}\",\"${dateFormat.format(Date(task.createdAt))}\",\"${task.targetDate?.let { dateFormat.format(Date(it)) } ?: ""}\"\n"
                    out.write(row.toByteArray())
                }
            }
            shareFile(context, file, "text/csv")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun exportTasksToPdf(context: Context, tasks: List<Task>) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        val paint = Paint()
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

        tasks.forEachIndexed { index, task ->
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
        val file = File(context.cacheDir, fileName)
        
        try {
            pdfDocument.writeTo(FileOutputStream(file))
            shareFile(context, file, "application/pdf")
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            pdfDocument.close()
        }
    }

    private fun shareFile(context: Context, file: File, mimeType: String) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Report"))
    }
}
