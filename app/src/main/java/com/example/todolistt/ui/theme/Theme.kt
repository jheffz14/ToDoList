package com.example.todolistt.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SketchLightColorScheme = lightColorScheme(
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

private val SketchDarkColorScheme = darkColorScheme(
    primary = SketchPrimary,
    secondary = SketchSecondary,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFFE1E1E1),
    onSurface = Color(0xFFE1E1E1),
    error = SketchError,
    outline = Color(0xFF333333)
)

@Composable
fun ToDoListtTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) SketchDarkColorScheme else SketchLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
