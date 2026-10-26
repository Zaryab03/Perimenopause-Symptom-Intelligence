package com.perimenopauseintelligence.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val inner: @Composable () -> Unit = {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = color,
            tonalElevation = 1.dp, shadowElevation = 1.dp, content = inner)
    } else {
        Surface(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = color,
            tonalElevation = 1.dp, shadowElevation = 1.dp, content = inner)
    }
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(top = 12.dp, bottom = 2.dp).semantics { heading() },
    )
}

/** A large, high-contrast choice button. Selected state is shown by fill, border-free and with a check-free label. */
@Composable
fun Pill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedColor: Color = MaterialTheme.colorScheme.primary,
    selectedContent: Color = MaterialTheme.colorScheme.onPrimary,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp).semantics { role = Role.RadioButton; this.selected = selected },
        shape = RoundedCornerShape(14.dp),
        color = if (selected) selectedColor else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (selected) selectedContent else MaterialTheme.colorScheme.onSurface,
    ) {
        Box(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
            Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center, maxLines = 2)
        }
    }
}

@Composable
fun EmojiBadge(emoji: String, size: Dp = 40.dp, color: Color = MaterialTheme.colorScheme.primaryContainer) {
    Box(Modifier.size(size).background(color, CircleShape), contentAlignment = Alignment.Center) {
        Text(emoji, style = MaterialTheme.typography.titleMedium)
    }
}

/** The app mark: a six-petal bloom. Drawn in code so it matches the launcher icon exactly. */
@Composable
fun LogoMark(size: Dp, petal: Color = Color.White, center: Color = Color(0xFFF2B880)) {
    Canvas(Modifier.size(size)) {
        val c = Offset(this.size.width / 2, this.size.height / 2)
        val unit = this.size.width / 108f
        for (a in -90 until 270 step 60) {
            val rad = Math.toRadians(a.toDouble())
            drawCircle(petal.copy(alpha = 0.9f), radius = 11f * unit, center = Offset(c.x + (19f * unit * cos(rad)).toFloat(), c.y + (19f * unit * sin(rad)).toFloat()))
        }
        drawCircle(center, radius = 10f * unit, center = c)
    }
}

@Composable
fun BrandHeader(modifier: Modifier = Modifier, logoSize: Dp = 96.dp) {
    Box(
        modifier.size(logoSize + 24.dp)
            .background(Brush.linearGradient(listOf(Color(0xFF3F8F89), Color(0xFF1F5A56))), RoundedCornerShape(32.dp)),
        contentAlignment = Alignment.Center,
    ) { LogoMark(logoSize) }
}

@Composable
fun BarChart(values: List<Int>, modifier: Modifier = Modifier, highlightLast: Boolean = false, height: Dp = 96.dp) {
    val bar = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceContainerHigh
    val max = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    Canvas(modifier.fillMaxWidth().height(height)) {
        if (values.isEmpty()) return@Canvas
        val gap = (size.width / values.size) * 0.28f
        val w = size.width / values.size - gap
        values.forEachIndexed { i, v ->
            val x = i * (w + gap) + gap / 2
            drawRoundRect(track, Offset(x, 0f), androidx.compose.ui.geometry.Size(w, size.height), androidx.compose.ui.geometry.CornerRadius(w / 3))
            if (v > 0) {
                val h = size.height * v / max
                drawRoundRect(
                    if (highlightLast && i == values.lastIndex) bar else bar.copy(alpha = 0.75f),
                    Offset(x, size.height - h), androidx.compose.ui.geometry.Size(w, h), androidx.compose.ui.geometry.CornerRadius(w / 3),
                )
            }
        }
    }
}
