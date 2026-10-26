package com.perimenopauseintelligence.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.perimenopauseintelligence.app.AppContainer
import com.perimenopauseintelligence.app.data.DefaultSymptoms
import com.perimenopauseintelligence.app.data.PeriodStatus
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
import java.time.LocalTime
import java.time.format.TextStyle

data class DayUi(val loaded: Boolean, val record: DayRecord?)

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(private val c: AppContainer) : ViewModel() {
    val date: StateFlow<LocalDate> = c.selectedDate
    val symptoms = c.repo.observeSymptoms().map { l -> l.filter { !it.archived } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allSymptoms = c.repo.observeSymptoms().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val day: StateFlow<DayUi> = c.selectedDate.flatMapLatest { d -> c.repo.observeDay(d).map { DayUi(true, it) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DayUi(false, null))
    val week = c.repo.observeRecords(LocalDate.now().minusDays(6), LocalDate.now())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
    fun setVisible(ids: Set<String>) = viewModelScope.launch { c.repo.setVisibleSymptoms(ids) }
}

private val moods = listOf(
    1 to ("😞" to "Very low"), 2 to ("🙁" to "Low"), 3 to ("😐" to "Okay"),
    4 to ("🙂" to "Good"), 5 to ("😊" to "Great"),
)

private fun greeting(): String {
    val h = LocalTime.now().hour
    return when { h < 12 -> "Good morning"; h < 18 -> "Good afternoon"; else -> "Good evening" }
}

private fun recordedCount(r: DayRecord?): Int =
    if (r == null || r.status != EntryStatus.LOGGED) 0 else
        r.observations.size + listOf(r.sleep, r.mood, r.energy, r.cycle).count { it != null } + (if (r.notes.isNotBlank()) 1 else 0)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TodayScreen(container: AppContainer) {
    val locale = androidx.compose.ui.platform.LocalLocale.current.platformLocale
    val vm = viewModel { TodayViewModel(container) }
    val date by vm.date.collectAsStateWithLifecycle()
    val symptoms by vm.symptoms.collectAsStateWithLifecycle()
    val allSymptoms by vm.allSymptoms.collectAsStateWithLifecycle()
    val dayUi by vm.day.collectAsStateWithLifecycle()
    val week by vm.week.collectAsStateWithLifecycle()
    val name by container.settings.userName.collectAsStateWithLifecycle()
    val period by container.settings.periodStatus.collectAsStateWithLifecycle()
    val record = dayUi.record
    var confirmClear by remember { mutableStateOf(false) }
    var showChooser by remember { mutableStateOf(false) }
    var expandedId by rememberSaveable { mutableStateOf<String?>(null) }
    var moreOpen by rememberSaveable { mutableStateOf(false) }
    val isToday = date == LocalDate.now()
    val count = recordedCount(record)

    ScreenList(title = "") {
        // Greeting and date navigation
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    if (name.isBlank()) greeting() else "${greeting()}, $name",
                    style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() },
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { vm.go(-1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous day") }
                    Text(
                        (if (isToday) "Today · " else "") + date.format(longDate),
                        style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                    )
                    IconButton(onClick = { vm.go(1) }, enabled = !isToday) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next day") }
                }
                if (!isToday) TextButton(onClick = { vm.today() }) { Text("Back to today") }
            }
        }

        // Mood check-in hero
        item {
            AppCard(color = MaterialTheme.colorScheme.primaryContainer) {
                Text(if (isToday) "How are you feeling today?" else "How were you feeling?", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    moods.forEach { (value, face) ->
                        val selected = record?.mood == value
                        Column(
                            Modifier.weight(1f).clickable { vm.setMood(if (selected) null else value) }
                                .semantics { contentDescription = "Mood: ${face.second}" + if (selected) ", selected" else "" }
                                .padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Surface(
                                shape = CircleShape, modifier = Modifier.size(54.dp),
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow,
                            ) { Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Text(face.first, style = MaterialTheme.typography.headlineSmall) } }
                            Text(face.second, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer, maxLines = 1)
                        }
                    }
                }
            }
        }

        // Live summary: confirms that logging did something
        item {
            AppCard(color = if (count > 0) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainer) {
                when {
                    !dayUi.loaded -> Text(" ")
                    record?.status == EntryStatus.SKIPPED -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("You skipped this day. That's okay.", Modifier.weight(1f))
                        TextButton(onClick = { vm.clearDay() }) { Text("Undo") }
                    }
                    count > 0 -> {
                        Text("✓  ${if (count == 1) "1 thing" else "$count things"} recorded", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        Text("Saved on this device. You can change it any time.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                    else -> {
                        Text("Nothing recorded yet", style = MaterialTheme.typography.titleSmall)
                        Text("Tap a symptom below if you've noticed it. Skipping a day is fine too.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item { SectionHeader("Your symptoms") }
        if (symptoms.isEmpty()) {
            item { InfoCard { Text("You're not tracking any symptoms yet. Choose some below.") } }
        }
        val byCategory = symptoms.groupBy { it.category }
        byCategory.forEach { (category, list) ->
            item(key = "cat_$category") {
                Text("${DefaultSymptoms.emojiFor(category)}  $category", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }
            items(list.size, key = { list[it].id }) { i ->
                val s = list[i]
                val cur = record?.observations?.firstOrNull { it.symptomId == s.id }?.let { it.state to it.severity }
                SymptomRow(
                    s, cur, expanded = expandedId == s.id,
                    onToggle = { expandedId = if (expandedId == s.id) null else s.id },
                    onSet = { st, sev -> vm.setObservation(s.id, st, sev); expandedId = null },
                )
            }
        }
        item {
            OutlinedButton(onClick = { showChooser = true }, modifier = Modifier.fillMaxWidth()) { Text("+  Add or hide symptoms") }
        }

        // Everything else, collapsed so the screen is not overwhelming
        item {
            AppCard(onClick = { moreOpen = !moreOpen }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Sleep, energy, bleeding & notes", style = MaterialTheme.typography.titleSmall)
                        Text("Optional extras", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(if (moreOpen) "Hide" else "Show", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                }
                AnimatedVisibility(moreOpen) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(top = 8.dp)) {
                        RatingRow("How did you sleep?", "1 = poorly, 5 = very well", record?.sleep, vm::setSleep)
                        RatingRow("Energy level", "1 = very low, 5 = high", record?.energy, vm::setEnergy)
                        if (period != PeriodStatus.NONE) {
                            Text("Menstrual bleeding", style = MaterialTheme.typography.titleSmall)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                CycleFlag.entries.forEach { f -> Pill(f.label, record?.cycle == f, { vm.setCycle(if (record?.cycle == f) null else f) }) }
                            }
                        }
                        if (dayUi.loaded) NotesField(date, record?.notes.orEmpty()) { d, t -> vm.setNotes(d, t) }
                    }
                }
            }
        }

        // This week at a glance
        item {
            AppCard {
                Text("Your last 7 days", style = MaterialTheme.typography.titleSmall)
                val days = (6 downTo 0).map { LocalDate.now().minusDays(it.toLong()) }
                val counts = days.map { d -> week.firstOrNull { it.date == d }?.observations?.count { it.state == ObsState.PRESENT } ?: 0 }
                val logged = days.count { d -> week.any { it.date == d && it.status == EntryStatus.LOGGED } }
                BarChart(counts, highlightLast = true)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    days.forEach { Text(it.dayOfWeek.getDisplayName(TextStyle.NARROW, locale), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                Text(
                    if (logged == 0) "Bars show how many symptoms you recorded each day. They'll appear as you log."
                    else "You've recorded $logged of the last 7 days. Bars show symptoms recorded per day. Days without a bar have no entry, which isn't the same as no symptoms.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (record == null && dayUi.loaded) {
            item { TextButton(onClick = { vm.skip() }, modifier = Modifier.fillMaxWidth()) { Text("Skip this day") } }
        }
        if (record != null && record.status == EntryStatus.LOGGED) {
            item { TextButton(onClick = { confirmClear = true }, modifier = Modifier.fillMaxWidth()) { Text("Clear everything for this day") } }
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
    if (showChooser) {
        var draft by remember { mutableStateOf(symptoms.map { it.id }.toSet()) }
        AlertDialog(
            onDismissRequest = { showChooser = false },
            title = { Text("What do you want to track?") },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) { SymptomChooser(allSymptoms, draft) { draft = it } } },
            confirmButton = { TextButton(onClick = { vm.setVisible(draft); showChooser = false }) { Text("Done") } },
            dismissButton = { TextButton(onClick = { showChooser = false }) { Text("Cancel") } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SymptomRow(
    s: SymptomInfo, current: Pair<ObsState, Int?>?, expanded: Boolean,
    onToggle: () -> Unit, onSet: (ObsState?, Int?) -> Unit,
) {
    val absent = current?.first == ObsState.ABSENT
    val sev = if (current?.first == ObsState.PRESENT) current.second else null
    val present = current?.first == ObsState.PRESENT
    AppCard(onClick = onToggle, color = if (current != null) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            EmojiBadge(DefaultSymptoms.emojiFor(s.category), size = 36.dp)
            Text(s.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            when {
                present && sev != null -> {
                    val c = severityColors(sev)
                    Surface(shape = CircleShape, color = c.container, contentColor = c.content) {
                        Text(Severity.label(sev), Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelLarge)
                    }
                }
                absent -> Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    Text("Not today", Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelLarge)
                }
                else -> Text(if (expanded) "Close" else "Tap to record", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        AnimatedVisibility(expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill("Not today", absent, { onSet(if (absent) null else ObsState.ABSENT, null) }, Modifier.weight(1f))
                    for (v in Severity.MIN..Severity.MAX) {
                        val selected = present && sev == v
                        val c = severityColors(v)
                        Pill(Severity.label(v), selected, { onSet(if (selected) null else ObsState.PRESENT, if (selected) null else v) }, Modifier.weight(1f), c.container, c.content)
                    }
                }
                if (current != null) TextButton(onClick = { onSet(null, null) }) { Text("Clear") }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RatingRow(label: String, hint: String, value: Int?, onSet: (Int?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (n in Ratings.MIN..Ratings.MAX) Pill("$n", value == n, { onSet(if (value == n) null else n) }, Modifier.weight(1f))
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
