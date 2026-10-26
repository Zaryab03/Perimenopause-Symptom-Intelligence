package com.perimenopauseintelligence.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.perimenopauseintelligence.app.AppContainer
import com.perimenopauseintelligence.app.domain.DayRecord
import com.perimenopauseintelligence.app.domain.EntryStatus
import com.perimenopauseintelligence.app.domain.ObsState
import com.perimenopauseintelligence.app.domain.Severity
import com.perimenopauseintelligence.app.domain.SymptomInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(private val c: AppContainer) : ViewModel() {
    val month = MutableStateFlow(YearMonth.now())
    val query = MutableStateFlow("")
    val symptoms = c.repo.observeSymptoms().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val monthRecords = month.flatMapLatest { m -> c.repo.observeRecords(m.atDay(1), m.atEndOfMonth()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allRecords = query.flatMapLatest { q ->
        if (q.isBlank()) kotlinx.coroutines.flow.flowOf(emptyList())
        else c.repo.observeRecords(LocalDate.of(2000, 1, 1), LocalDate.now())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun open(date: LocalDate) { c.selectedDate.value = date }
}

@Composable
fun HistoryScreen(container: AppContainer, onOpenDay: () -> Unit) {
    val locale = androidx.compose.ui.platform.LocalLocale.current.platformLocale
    val vm = viewModel { HistoryViewModel(container) }
    val month by vm.month.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val symptoms by vm.symptoms.collectAsStateWithLifecycle()
    val monthRecords by vm.monthRecords.collectAsStateWithLifecycle()
    val allRecords by vm.allRecords.collectAsStateWithLifecycle()
    val labels = symptoms.associateBy { it.id }
    val searching = query.isNotBlank()
    var selected by remember { mutableStateOf<LocalDate?>(null) }

    val shown: List<DayRecord> = (if (searching) {
        val q = query.trim().lowercase()
        allRecords.filter { r -> matches(r, q, labels) }
    } else monthRecords).filter { it.status == EntryStatus.LOGGED }.sortedByDescending { it.date }

    ScreenList("History") {
        item {
            OutlinedTextField(
                value = query, onValueChange = { vm.query.value = it },
                label = { Text("Search symptoms or notes") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
            )
        }
        if (!searching) {
            item {
                AppCard {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { vm.month.value = month.minusMonths(1); selected = null }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
                        }
                        Text(
                            month.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale)),
                            style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { vm.month.value = month.plusMonths(1); selected = null }, enabled = month < YearMonth.now()) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
                        }
                    }
                    CalendarGrid(month, monthRecords.associateBy { it.date }, locale, selected) { selected = it }
                    Text(
                        "Darker days have more symptoms recorded. A ring means you skipped. No mark means no entry, which is unknown, not symptom-free.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            selected?.let { day ->
                item {
                    val rec = monthRecords.firstOrNull { it.date == day }
                    AppCard(color = MaterialTheme.colorScheme.primaryContainer) {
                        Text(day.format(longDate), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        if (rec == null) Text("No entry for this day.", color = MaterialTheme.colorScheme.onPrimaryContainer)
                        else if (rec.status == EntryStatus.SKIPPED) Text("You skipped this day.", color = MaterialTheme.colorScheme.onPrimaryContainer)
                        else DaySummary(rec, labels, MaterialTheme.colorScheme.onPrimaryContainer)
                        Button(onClick = { vm.open(day); onOpenDay() }) { Text(if (rec == null) "Add an entry" else "View or edit") }
                    }
                }
            }
        }
        item {
            Text(
                if (searching) "${shown.size} matching ${if (shown.size == 1) "day" else "days"} across your whole history" else "Entries this month",
                style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp),
            )
        }
        if (shown.isEmpty()) {
            item {
                InfoCard {
                    Text(if (searching) "Nothing matches that search." else "No entries this month yet. Days you log will appear here.")
                }
            }
        }
        items(shown.size, key = { shown[it].date.toString() }) { i ->
            val r = shown[i]
            AppCard(onClick = { vm.open(r.date); onOpenDay() }) {
                Text(r.date.format(longDate), style = MaterialTheme.typography.titleSmall)
                DaySummary(r, labels, MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

private fun matches(r: DayRecord, q: String, labels: Map<String, SymptomInfo>): Boolean =
    r.status == EntryStatus.LOGGED && (
        r.notes.lowercase().contains(q) ||
            r.observations.any { it.state == ObsState.PRESENT && (labels[it.symptomId]?.label ?: "").lowercase().contains(q) }
        )

@Composable
private fun DaySummary(r: DayRecord, labels: Map<String, SymptomInfo>, color: Color) {
    val present = r.observations.filter { it.state == ObsState.PRESENT }
    if (present.isNotEmpty()) {
        Text(present.joinToString(", ") { o ->
            val n = labels[o.symptomId]?.label ?: "Symptom"
            if (o.severity != null) "$n (${Severity.label(o.severity).lowercase()})" else n
        }, color = color)
    }
    val absent = r.observations.count { it.state == ObsState.ABSENT }
    if (absent > 0) Text("Marked \"not today\": $absent", style = MaterialTheme.typography.bodySmall, color = color)
    r.mood?.let { Text("Mood $it of 5", style = MaterialTheme.typography.bodySmall, color = color) }
    if (r.notes.isNotBlank()) Text("“" + r.notes.trim().take(160) + "”", style = MaterialTheme.typography.bodySmall, color = color)
    if (present.isEmpty() && absent == 0 && r.notes.isBlank() && r.mood == null) Text("Details recorded", style = MaterialTheme.typography.bodySmall, color = color)
}

@Composable
private fun CalendarGrid(month: YearMonth, records: Map<LocalDate, DayRecord>, locale: Locale, selected: LocalDate?, onDay: (LocalDate) -> Unit) {
    val first = month.atDay(1)
    val lead = (first.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
    val days = month.lengthOfMonth()
    val rows = (lead + days + 6) / 7
    val today = LocalDate.now()
    val primary = MaterialTheme.colorScheme.primary
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth()) {
            for (i in 0 until 7) {
                Text(
                    DayOfWeek.of(i + 1).getDisplayName(TextStyle.NARROW, locale),
                    modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (col in 0 until 7) {
                    val dayNum = r * 7 + col - lead + 1
                    if (dayNum < 1 || dayNum > days) {
                        Box(Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val date = month.atDay(dayNum)
                        val rec = records[date]
                        val future = date.isAfter(today)
                        val n = rec?.observations?.count { it.state == ObsState.PRESENT } ?: 0
                        val fill = when {
                            rec?.status != EntryStatus.LOGGED -> Color.Transparent
                            n == 0 -> primary.copy(alpha = 0.14f)
                            n <= 2 -> primary.copy(alpha = 0.35f)
                            n <= 4 -> primary.copy(alpha = 0.6f)
                            else -> primary.copy(alpha = 0.85f)
                        }
                        val textColor = if (n > 2 && rec?.status == EntryStatus.LOGGED) MaterialTheme.colorScheme.onPrimary
                        else if (future) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                        val desc = date.format(longDate) + when {
                            rec?.status == EntryStatus.LOGGED -> ", $n symptoms recorded"
                            rec?.status == EntryStatus.SKIPPED -> ", skipped"
                            else -> ", no entry"
                        }
                        Box(
                            Modifier.weight(1f).aspectRatio(1f)
                                .background(fill, CircleShape)
                                .border(
                                    BorderStroke(
                                        if (date == selected) 3.dp else if (date == today || rec?.status == EntryStatus.SKIPPED) 2.dp else 0.dp,
                                        if (date == selected) MaterialTheme.colorScheme.secondary else primary,
                                    ),
                                    CircleShape,
                                )
                                .clickable(enabled = !future) { onDay(date) }
                                .semantics { contentDescription = desc },
                            contentAlignment = Alignment.Center,
                        ) { Text("$dayNum", style = MaterialTheme.typography.bodyMedium, color = textColor) }
                    }
                }
            }
        }
    }
}
