package com.example.todolistt.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val SketchColorScheme = lightColorScheme(
    primary = SketchPrimary,
    secondary = SketchSecondary,
    background = SketchPaper,
    surface = SketchWhite,
    onPrimary = SketchWhite,
    onSecondary = SketchWhite,
    onBackground = SketchBlack,
    onSurface = SketchBlack,
    error = SketchError
)

@Composable
fun ToDoListTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SketchColorScheme,
        typography = Typography,
        content = content
    )
}
