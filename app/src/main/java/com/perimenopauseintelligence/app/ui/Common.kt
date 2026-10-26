package com.perimenopauseintelligence.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

val longDate: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.getDefault())
val shortDate: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())

@Composable
fun ScreenList(
    title: String,
    onBack: (() -> Unit)? = null,
    state: LazyListState = rememberLazyListState(),
    content: LazyListScope.() -> Unit,
) {
    Column(Modifier.statusBarsPadding()) {
        if (title.isNotEmpty()) Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            } else {
                androidx.compose.foundation.layout.Spacer(Modifier.padding(start = 8.dp))
            }
            Text(title, style = MaterialTheme.typography.headlineSmall)
        }
        LazyColumn(
            state = state,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
fun InfoCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    AppCard(modifier = modifier, color = MaterialTheme.colorScheme.surfaceContainer) { content() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerButtonDialog(initial: LocalDate, maxDate: LocalDate = LocalDate.now(), onPicked: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initial.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let {
                    val d = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    onPicked(if (d.isAfter(maxDate)) maxDate else d)
                }
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) { DatePicker(state = state) }
}
