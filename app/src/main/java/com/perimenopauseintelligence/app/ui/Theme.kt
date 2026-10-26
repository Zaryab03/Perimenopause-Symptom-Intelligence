package com.perimenopauseintelligence.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Color(0xFF2F6F6B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCFE8E5),
    onPrimaryContainer = Color(0xFF0B2F2C),
    secondary = Color(0xFF7A4B66),
    secondaryContainer = Color(0xFFF2DCE8),
    onSecondaryContainer = Color(0xFF301024),
    background = Color(0xFFFAF8F6),
    surface = Color(0xFFFAF8F6),
    surfaceVariant = Color(0xFFE9E4E0),
    onSurface = Color(0xFF1C1B1A),
    onSurfaceVariant = Color(0xFF4B4743),
    outline = Color(0xFF6F6A66),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF8FD1CB),
    onPrimary = Color(0xFF00201E),
    primaryContainer = Color(0xFF0F4B47),
    onPrimaryContainer = Color(0xFFCFE8E5),
    secondary = Color(0xFFE6B8D0),
    secondaryContainer = Color(0xFF5C3249),
    onSecondaryContainer = Color(0xFFF7DCEA),
    background = Color(0xFF151413),
    surface = Color(0xFF151413),
    surfaceVariant = Color(0xFF3A3633),
    onSurface = Color(0xFFE8E3DF),
    onSurfaceVariant = Color(0xFFCBC5C0),
    outline = Color(0xFF948E89),
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
