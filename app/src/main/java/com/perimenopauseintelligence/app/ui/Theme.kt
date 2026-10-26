package com.perimenopauseintelligence.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Light = lightColorScheme(
    primary = Color(0xFF2A6662),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3EBE8),
    onPrimaryContainer = Color(0xFF08302D),
    secondary = Color(0xFF8A4F6C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF7E0EB),
    onSecondaryContainer = Color(0xFF3A1229),
    tertiary = Color(0xFF8A5A1F),
    tertiaryContainer = Color(0xFFFBE7C8),
    onTertiaryContainer = Color(0xFF3A2400),
    background = Color(0xFFFAF8F6),
    onBackground = Color(0xFF1C1B1A),
    surface = Color(0xFFFAF8F6),
    onSurface = Color(0xFF1C1B1A),
    surfaceVariant = Color(0xFFEAE5E1),
    onSurfaceVariant = Color(0xFF4B4743),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFF4F0ED),
    surfaceContainerHigh = Color(0xFFEDE8E4),
    surfaceContainerHighest = Color(0xFFE6E0DB),
    outline = Color(0xFF6F6A66),
    outlineVariant = Color(0xFFD9D3CE),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF8FD1CB),
    onPrimary = Color(0xFF00201E),
    primaryContainer = Color(0xFF14504B),
    onPrimaryContainer = Color(0xFFD3EBE8),
    secondary = Color(0xFFE6B8D0),
    onSecondary = Color(0xFF3A1229),
    secondaryContainer = Color(0xFF5C3249),
    onSecondaryContainer = Color(0xFFF7DCEA),
    tertiary = Color(0xFFEFC27F),
    tertiaryContainer = Color(0xFF5A3F10),
    onTertiaryContainer = Color(0xFFFBE7C8),
    background = Color(0xFF151413),
    onBackground = Color(0xFFE8E3DF),
    surface = Color(0xFF151413),
    onSurface = Color(0xFFE8E3DF),
    surfaceVariant = Color(0xFF3A3633),
    onSurfaceVariant = Color(0xFFCBC5C0),
    surfaceContainerLowest = Color(0xFF100F0E),
    surfaceContainerLow = Color(0xFF1E1C1B),
    surfaceContainer = Color(0xFF242120),
    surfaceContainerHigh = Color(0xFF2E2B29),
    surfaceContainerHighest = Color(0xFF393532),
    outline = Color(0xFF948E89),
    outlineVariant = Color(0xFF4B4743),
)

private val AppTypography = Typography(
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
)

private val AppShapes = Shapes(
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
)

/** Severity is always shown with a text label too; colour is only a hint. */
data class SeverityColors(val container: Color, val content: Color)

@Composable
fun severityColors(severity: Int): SeverityColors {
    val dark = isSystemInDarkTheme()
    return when (severity) {
        1 -> if (dark) SeverityColors(Color(0xFF2D4A22), Color(0xFFCDE8BE)) else SeverityColors(Color(0xFFE0F0D2), Color(0xFF213815))
        2 -> if (dark) SeverityColors(Color(0xFF5A4410), Color(0xFFF5DDA0)) else SeverityColors(Color(0xFFFBE6B5), Color(0xFF473200))
        else -> if (dark) SeverityColors(Color(0xFF6B2A22), Color(0xFFFFD9D3)) else SeverityColors(Color(0xFFF8D2CA), Color(0xFF58180E))
    }
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
