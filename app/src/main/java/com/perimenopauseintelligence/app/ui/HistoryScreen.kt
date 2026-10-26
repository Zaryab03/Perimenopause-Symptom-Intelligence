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
import androidx.compose.material3.Card
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
import kotlinx.coroutines.flow.combine
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

    val shown: List<DayRecord> = if (searching) {
        val q = query.trim().lowercase()
        allRecords.filter { r -> matches(r, q, labels) }.sortedByDescending { it.date }
    } else {
        monthRecords.sortedByDescending { it.date }
    }

    ScreenList("History") {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { vm.query.value = it },
                label = { Text("Search symptoms or notes") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (!searching) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { vm.month.value = month.minusMonths(1) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
                    }
                    Text(
                        month.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale)),
                        style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { vm.month.value = month.plusMonths(1) }, enabled = month < YearMonth.now()) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
                    }
                }
            }
            item { CalendarGrid(month, monthRecords.associateBy { it.date }, locale) { vm.open(it); onOpenDay() } }
            item {
                Text(
                    "● day with entries     ○ skipped day     no mark: no entry (unknown, not symptom-free)",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            val loggedCount = shown.count { it.status == EntryStatus.LOGGED }
            Text(
                if (searching) "$loggedCount matching days across your whole history" else "Entries this month",
                style = MaterialTheme.typography.titleSmall,
            )
        }
        if (shown.none { it.status == EntryStatus.LOGGED }) {
            item {
                InfoCard {
                    Text(
                        if (searching) "Nothing matches that search." else "No entries for this month yet. Days you log will appear here.",
                    )
                }
            }
        }
        items(shown.filter { it.status == EntryStatus.LOGGED }.size, key = { shown.filter { it.status == EntryStatus.LOGGED }[it].date.toString() }) { i ->
            val r = shown.filter { it.status == EntryStatus.LOGGED }[i]
            TimelineCard(r, labels) { vm.open(r.date); onOpenDay() }
        }
    }
}

private fun matches(r: DayRecord, q: String, labels: Map<String, SymptomInfo>): Boolean =
    r.status == EntryStatus.LOGGED && (
        r.notes.lowercase().contains(q) ||
            r.observations.any { it.state == ObsState.PRESENT && (labels[it.symptomId]?.label ?: "").lowercase().contains(q) }
        )

@Composable
private fun TimelineCard(r: DayRecord, labels: Map<String, SymptomInfo>, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(r.date.format(longDate), style = MaterialTheme.typography.titleSmall)
            val present = r.observations.filter { it.state == ObsState.PRESENT }
            if (present.isNotEmpty()) {
                Text(present.joinToString(", ") { o ->
                    val n = labels[o.symptomId]?.label ?: "Symptom"
                    if (o.severity != null) "$n (${Severity.label(o.severity).lowercase()})" else n
                })
            }
            val absent = r.observations.count { it.state == ObsState.ABSENT }
            if (absent > 0) Text("Marked \"not today\": $absent", style = MaterialTheme.typography.bodySmall)
            if (r.notes.isNotBlank()) Text(r.notes.trim().take(160), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (present.isEmpty() && absent == 0 && r.notes.isBlank()) Text("Context recorded", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun CalendarGrid(month: YearMonth, records: Map<LocalDate, DayRecord>, locale: Locale, onDay: (LocalDate) -> Unit) {
    val first = month.atDay(1)
    val lead = (first.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
    val days = month.lengthOfMonth()
    val rows = (lead + days + 6) / 7
    val today = LocalDate.now()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth()) {
            for (i in 0 until 7) {
                Text(
                    DayOfWeek.of(i + 1).getDisplayName(TextStyle.NARROW, locale),
                    modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium,
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
                        val desc = date.format(longDate) + when {
                            rec?.status == EntryStatus.LOGGED -> ", entries recorded"
                            rec?.status == EntryStatus.SKIPPED -> ", skipped"
                            else -> ", no entry"
                        }
                        Box(
                            Modifier.weight(1f).aspectRatio(1f)
                                .border(
                                    BorderStroke(if (date == today) 2.dp else 1.dp, if (date == today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                    MaterialTheme.shapes.small,
                                )
                                .clickable(enabled = !future) { onDay(date) }
                                .semantics { contentDescription = desc },
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$dayNum", style = MaterialTheme.typography.bodyMedium,
                                    color = if (future) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface)
                                when (rec?.status) {
                                    EntryStatus.LOGGED -> Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                                    EntryStatus.SKIPPED -> Box(Modifier.size(8.dp).border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape))
                                    null -> Box(Modifier.size(8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
