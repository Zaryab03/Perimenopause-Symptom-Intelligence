package com.perimenopauseintelligence.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.perimenopauseintelligence.app.AppContainer
import com.perimenopauseintelligence.app.domain.CycleFlag
import com.perimenopauseintelligence.app.domain.DayRecord
import com.perimenopauseintelligence.app.domain.EntryStatus
import com.perimenopauseintelligence.app.domain.ObsState
import com.perimenopauseintelligence.app.domain.Ratings
import com.perimenopauseintelligence.app.domain.Severity
import com.perimenopauseintelligence.app.domain.SymptomInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DayUi(val loaded: Boolean, val record: DayRecord?)

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(private val c: AppContainer) : ViewModel() {
    val date: StateFlow<LocalDate> = c.selectedDate
    val symptoms = c.repo.observeSymptoms().map { l -> l.filter { !it.archived } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val day: StateFlow<DayUi> = c.selectedDate.flatMapLatest { d -> c.repo.observeDay(d).map { DayUi(true, it) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DayUi(false, null))

    fun go(days: Long) {
        val next = c.selectedDate.value.plusDays(days)
        if (!next.isAfter(LocalDate.now())) c.selectedDate.value = next
    }
    fun today() { c.selectedDate.value = LocalDate.now() }
    fun setObservation(id: String, state: ObsState?, severity: Int?) =
        viewModelScope.launch { c.repo.setObservation(date.value, id, state, severity) }
    fun setSleep(v: Int?) = viewModelScope.launch { c.repo.updateEntry(date.value) { it.copy(sleep = v) } }
    fun setMood(v: Int?) = viewModelScope.launch { c.repo.updateEntry(date.value) { it.copy(mood = v) } }
    fun setEnergy(v: Int?) = viewModelScope.launch { c.repo.updateEntry(date.value) { it.copy(energy = v) } }
    fun setCycle(v: CycleFlag?) = viewModelScope.launch { c.repo.updateEntry(date.value) { it.copy(cycle = v?.name) } }
    fun setNotes(forDate: LocalDate, text: String) = viewModelScope.launch { c.repo.updateEntry(forDate) { it.copy(notes = text) } }
    fun skip() = viewModelScope.launch { c.repo.skipDay(date.value) }
    fun clearDay() = viewModelScope.launch { c.repo.deleteDay(date.value) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TodayScreen(container: AppContainer) {
    val vm = viewModel { TodayViewModel(container) }
    val date by vm.date.collectAsStateWithLifecycle()
    val symptoms by vm.symptoms.collectAsStateWithLifecycle()
    val dayUi by vm.day.collectAsStateWithLifecycle()
    val record = dayUi.record
    var confirmClear by remember { mutableStateOf(false) }
    val isToday = date == LocalDate.now()

    ScreenList(if (isToday) "Today" else "Daily log") {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { vm.go(-1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous day") }
                Text(
                    date.format(longDate),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                IconButton(onClick = { vm.go(1) }, enabled = !isToday) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next day")
                }
            }
            if (!isToday) TextButton(onClick = { vm.today() }) { Text("Back to today") }
        }
        item {
            InfoCard {
                Text(
                    "Record only what you want to. Anything you leave blank is simply \"not recorded\" — " +
                        "it is never treated as \"no symptoms\".",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        item {
            when {
                !dayUi.loaded -> {}
                record == null -> OutlinedButton(onClick = { vm.skip() }) { Text("Skip this day") }
                record.status == EntryStatus.SKIPPED -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("You skipped this day. That's okay.", Modifier.weight(1f))
                    TextButton(onClick = { vm.clearDay() }) { Text("Undo") }
                }
            }
        }

        val byCategory = symptoms.groupBy { it.category }
        byCategory.forEach { (category, list) ->
            item(key = "cat_$category") {
                Text(category, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp).semantics { heading() })
            }
            items(list.size, key = { list[it].id }) { i ->
                val s = list[i]
                SymptomRow(s, record?.observations?.firstOrNull { it.symptomId == s.id }?.let { it.state to it.severity }, vm::setObservation)
            }
        }

        item {
            Text("How was your day? (optional)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp).semantics { heading() })
        }
        item { RatingRow("Sleep", "1 = poor, 5 = good", record?.sleep, vm::setSleep) }
        item { RatingRow("Mood", "1 = low, 5 = good", record?.mood, vm::setMood) }
        item { RatingRow("Energy", "1 = low, 5 = high", record?.energy, vm::setEnergy) }
        item {
            Text("Menstrual bleeding (optional)", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CycleFlag.entries.forEach { f ->
                    FilterChip(selected = record?.cycle == f, onClick = { vm.setCycle(if (record?.cycle == f) null else f) }, label = { Text(f.label) })
                }
            }
        }
        item {
            if (dayUi.loaded) NotesField(date, record?.notes.orEmpty()) { d, t -> vm.setNotes(d, t) }
        }
        if (record != null && record.status == EntryStatus.LOGGED) {
            item { TextButton(onClick = { confirmClear = true }) { Text("Clear everything for this day") } }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear this day?") },
            text = { Text("This deletes everything you recorded for ${date.format(shortDate)}. It can't be undone.") },
            confirmButton = { TextButton(onClick = { vm.clearDay(); confirmClear = false }) { Text("Clear") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Keep") } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SymptomRow(s: SymptomInfo, current: Pair<ObsState, Int?>?, onSet: (String, ObsState?, Int?) -> Unit) {
    androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(s.label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val absent = current?.first == ObsState.ABSENT
            FilterChip(
                selected = absent,
                onClick = { onSet(s.id, if (absent) null else ObsState.ABSENT, null) },
                label = { Text("Not today") },
            )
            for (sev in Severity.MIN..Severity.MAX) {
                val selected = current?.first == ObsState.PRESENT && current.second == sev
                FilterChip(
                    selected = selected,
                    onClick = { onSet(s.id, if (selected) null else ObsState.PRESENT, if (selected) null else sev) },
                    label = { Text(Severity.label(sev)) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RatingRow(label: String, hint: String, value: Int?, onSet: (Int?) -> Unit) {
    androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("$label  ·  $hint", style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (n in Ratings.MIN..Ratings.MAX) {
                FilterChip(selected = value == n, onClick = { onSet(if (value == n) null else n) }, label = { Text("$n") })
            }
        }
    }
}

@Composable
private fun NotesField(date: LocalDate, saved: String, onSave: (LocalDate, String) -> Unit) {
    var text by remember(date) { mutableStateOf(saved) }
    val latest by rememberUpdatedState(text)
    val savedNow by rememberUpdatedState(saved)
    val save by rememberUpdatedState(onSave)
    LaunchedEffect(text) {
        if (text != savedNow) {
            kotlinx.coroutines.delay(500)
            save(date, text)
        }
    }
    DisposableEffect(date) { onDispose { if (latest != savedNow) save(date, latest) } }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it.take(5000) },
        label = { Text("Notes (optional)") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 3,
    )
}
