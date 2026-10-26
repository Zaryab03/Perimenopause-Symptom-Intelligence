package com.perimenopauseintelligence.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.perimenopauseintelligence.app.AppContainer
import com.perimenopauseintelligence.app.data.DefaultSymptoms
import com.perimenopauseintelligence.app.domain.DayRecord
import com.perimenopauseintelligence.app.domain.InsightText
import com.perimenopauseintelligence.app.domain.Insights
import com.perimenopauseintelligence.app.domain.ObsState
import com.perimenopauseintelligence.app.domain.PeriodSummary
import com.perimenopauseintelligence.app.domain.SymptomInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class InsightsUi(
    val current: PeriodSummary?,
    val previous: PeriodSummary?,
    val records: List<DayRecord>,
    val start: LocalDate,
    val end: LocalDate,
    val symptoms: Map<String, SymptomInfo>,
)

@OptIn(ExperimentalCoroutinesApi::class)
class InsightsViewModel(private val c: AppContainer) : ViewModel() {
    val days = MutableStateFlow(30)
    val compare = MutableStateFlow(false)

    val ui = combine(days, compare) { d, cmp -> d to cmp }.flatMapLatest { (d, cmp) ->
        val end = LocalDate.now()
        val start = end.minusDays(d - 1L)
        val prevEnd = start.minusDays(1)
        val prevStart = prevEnd.minusDays(d - 1L)
        c.repo.observeRecords(if (cmp) prevStart else start, end).combine(c.repo.observeSymptoms()) { recs, syms ->
            InsightsUi(
                current = Insights.summarize(start, end, recs),
                previous = if (cmp) Insights.summarize(prevStart, prevEnd, recs) else null,
                records = recs.filter { !it.date.isBefore(start) },
                start = start, end = end,
                symptoms = syms.associateBy { it.id },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}

@Composable
fun InsightsScreen(container: AppContainer) {
    val vm = viewModel { InsightsViewModel(container) }
    val days by vm.days.collectAsStateWithLifecycle()
    val compare by vm.compare.collectAsStateWithLifecycle()
    val ui by vm.ui.collectAsStateWithLifecycle()
    var showMethod by remember { mutableStateOf(false) }

    ScreenList("Insights") {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(7 to "7 days", 30 to "30 days", 90 to "90 days").forEach { (n, label) ->
                    Pill(label, days == n, { vm.days.value = n }, Modifier.weight(1f))
                }
            }
        }
        val data = ui ?: return@ScreenList
        val cur = data.current ?: return@ScreenList

        // How much has been recorded
        item {
            AppCard(color = MaterialTheme.colorScheme.primaryContainer) {
                Text("Last ${cur.totalDays} days", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("${cur.loggedDays} of ${cur.totalDays} days recorded", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                LinearProgressIndicator(
                    progress = { cur.loggedDays.toFloat() / cur.totalDays },
                    modifier = Modifier.fillMaxWidth().height(8.dp).semantics { contentDescription = "${cur.loggedDays} of ${cur.totalDays} days recorded" },
                    trackColor = MaterialTheme.colorScheme.surfaceContainerLow,
                )
                Text(
                    buildString {
                        if (cur.skippedDays > 0) append("${InsightText.days(cur.skippedDays)} skipped. ")
                        append("${InsightText.days(cur.noEntryDays)} with no entry — those are unknown, not symptom-free.")
                    },
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        // Daily chart: always shown, it's just a record of what was logged
        item {
            AppCard {
                Text("Symptoms recorded per day", style = MaterialTheme.typography.titleSmall)
                val range = generateSequence(data.start) { it.plusDays(1) }.takeWhile { !it.isAfter(data.end) }.toList()
                val byDate = data.records.associateBy { it.date }
                BarChart(range.map { d -> byDate[d]?.observations?.count { it.state == ObsState.PRESENT } ?: 0 }, highlightLast = true)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(data.start.format(shortDate), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Today", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        val prev = data.previous
        val ids = (cur.stats.keys + (prev?.stats?.keys ?: emptySet())).filter { id ->
            cur.stat(id).presentDays > 0 || (prev?.stat(id)?.presentDays ?: 0) > 0
        }.sortedByDescending { cur.stat(it).presentDays }

        if (!cur.sufficient) {
            item {
                AppCard(color = MaterialTheme.colorScheme.tertiaryContainer) {
                    Text("Your first summary is almost there", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    LinearProgressIndicator(progress = { (cur.loggedDays.toFloat() / Insights.MIN_LOGGED_DAYS).coerceAtMost(1f) }, modifier = Modifier.fillMaxWidth().height(8.dp))
                    Text(
                        "${cur.loggedDays} of ${Insights.MIN_LOGGED_DAYS} days with entries. Once you reach ${Insights.MIN_LOGGED_DAYS}, you'll see how often each symptom was recorded. " +
                            "Fewer days could give a misleading picture, so we wait. Log at your own pace.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }
            if (ids.isNotEmpty()) {
                item { SectionHeader("What you've recorded so far") }
                items(ids.size, key = { "early_" + ids[it] }) { i ->
                    val s = cur.stat(ids[i]); val info = data.symptoms[ids[i]]
                    InfoCard { Text("${info?.label ?: "Symptom"}: recorded on ${InsightText.days(s.presentDays)}") }
                }
            }
        } else {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Compare with the previous ${cur.totalDays} days", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = compare, onCheckedChange = { vm.compare.value = it })
                }
            }
            item { SectionHeader("Most recorded symptoms") }
            if (ids.isEmpty()) item { InfoCard { Text("No symptoms were recorded as present in this period.") } }
            items(ids.size, key = { ids[it] }) { i ->
                val id = ids[i]
                val info = data.symptoms[id]
                val label = info?.label ?: "Symptom"
                val stat = cur.stat(id)
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        EmojiBadge(DefaultSymptoms.emojiFor(info?.category ?: ""), size = 36.dp)
                        Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        Text("${stat.presentDays} of ${stat.loggedDays} days", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                    Box(Modifier.fillMaxWidth().height(10.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.small)
                        .semantics { contentDescription = "$label recorded on ${stat.presentDays} of ${stat.loggedDays} days with entries" }) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(stat.rate.toFloat().coerceIn(0.02f, 1f)).background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small))
                    }
                    Text(InsightText.frequency(label, stat), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (prev != null) {
                        Text(
                            InsightText.comparison(label, Insights.compare(cur, prev, id)) ?: "Not enough data in one of the periods to compare.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
            val ctx = listOfNotNull(
                cur.sleepAverage?.let { "Sleep: average ${"%.1f".format(it.value)} of 5 over ${InsightText.days(it.count)}" },
                cur.moodAverage?.let { "Mood: average ${"%.1f".format(it.value)} of 5 over ${InsightText.days(it.count)}" },
                cur.energyAverage?.let { "Energy: average ${"%.1f".format(it.value)} of 5 over ${InsightText.days(it.count)}" },
            )
            if (ctx.isNotEmpty()) item { InfoCard { Text("Sleep, mood and energy", style = MaterialTheme.typography.titleSmall); ctx.forEach { Text(it) } } }
        }
        item {
            InfoCard {
                TextButton(onClick = { showMethod = !showMethod }) { Text(if (showMethod) "Hide how this works" else "How is this calculated?") }
                if (showMethod) {
                    Text(
                        "• Only days where you recorded something count as \"days recorded\".\n" +
                            "• A symptom is counted when you marked it present. \"Not today\" is counted separately.\n" +
                            "• If you didn't mention a symptom on a day with entries, it is shown as \"not mentioned\", never as absent.\n" +
                            "• Average severity uses your own 1–3 ratings.\n" +
                            "• Summaries need at least ${Insights.MIN_LOGGED_DAYS} days with entries. Comparisons need that in both periods, and only describe a difference when " +
                            "frequency changes by ${(Insights.RATE_DIFFERENCE_THRESHOLD * 100).toInt()} percentage points or more, or average severity by ${Insights.SEVERITY_DIFFERENCE_THRESHOLD} or more.\n" +
                            "• These describe what you recorded. They don't show causes and are not a diagnosis.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}
