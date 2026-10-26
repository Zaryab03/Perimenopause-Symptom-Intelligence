package com.perimenopauseintelligence.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.perimenopauseintelligence.app.AppContainer
import com.perimenopauseintelligence.app.domain.Insights
import com.perimenopauseintelligence.app.domain.InsightText
import com.perimenopauseintelligence.app.domain.PeriodSummary
import com.perimenopauseintelligence.app.domain.SymptomInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class InsightsUi(
    val current: PeriodSummary?,
    val previous: PeriodSummary?,
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
                symptoms = syms.associateBy { it.id },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InsightsUi(null, null, emptyMap()))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InsightsScreen(container: AppContainer) {
    val vm = viewModel { InsightsViewModel(container) }
    val days by vm.days.collectAsStateWithLifecycle()
    val compare by vm.compare.collectAsStateWithLifecycle()
    val ui by vm.ui.collectAsStateWithLifecycle()
    var showMethod by remember { mutableStateOf(false) }

    ScreenList("Insights") {
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(7 to "Last 7 days", 30 to "Last 30 days", 90 to "Last 90 days").forEach { (n, label) ->
                    FilterChip(selected = days == n, onClick = { vm.days.value = n }, label = { Text(label) })
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Compare with the previous $days days", Modifier.weight(1f))
                Switch(checked = compare, onCheckedChange = { vm.compare.value = it })
            }
        }
        val cur = ui.current
        if (cur == null) return@ScreenList
        item {
            InfoCard {
                Text("Completeness", style = MaterialTheme.typography.titleSmall)
                Text("You have entries on ${InsightText.days(cur.loggedDays)} of the last ${cur.totalDays}.")
                if (cur.skippedDays > 0) Text("${InsightText.days(cur.skippedDays)} skipped.")
                Text("${InsightText.days(cur.noEntryDays)} with no entry. These are unknown, not symptom-free.", style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (!cur.sufficient) {
            item {
                InfoCard {
                    Text("Not enough entries yet", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Summaries need at least ${Insights.MIN_LOGGED_DAYS} days with entries in the selected period, so that they " +
                            "don't give a misleading picture. Keep logging at your own pace, or choose a longer period.",
                    )
                }
            }
        } else {
            val prev = ui.previous
            val ids = (cur.stats.keys + (prev?.stats?.keys ?: emptySet())).filter { id -> (cur.stat(id).presentDays > 0) || (prev?.stat(id)?.presentDays ?: 0) > 0 }
            val sorted = ids.sortedByDescending { cur.stat(it).presentDays }
            if (sorted.isEmpty()) {
                item { InfoCard { Text("No symptoms were recorded as present in this period.") } }
            }
            items(sorted.size, key = { sorted[it] }) { i ->
                val id = sorted[i]
                val label = ui.symptoms[id]?.label ?: "Symptom"
                val stat = cur.stat(id)
                InfoCard {
                    Text(label, style = MaterialTheme.typography.titleSmall)
                    Text(InsightText.frequency(label, stat), style = MaterialTheme.typography.bodyMedium)
                    LinearProgressIndicator(
                        progress = { stat.rate.toFloat() },
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "$label recorded on ${stat.presentDays} of ${stat.loggedDays} days with entries" },
                    )
                    if (prev != null) {
                        val c = Insights.compare(cur, prev, id)
                        Text(
                            InsightText.comparison(label, c) ?: "Not enough data in one of the periods to compare.",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                androidx.compose.material3.TextButton(onClick = { showMethod = !showMethod }) { Text(if (showMethod) "Hide how this works" else "How is this calculated?") }
                if (showMethod) {
                    Text(
                        "• Only days where you recorded something count as \"days with entries\".\n" +
                            "• A symptom is counted when you marked it present. \"Not today\" is counted separately.\n" +
                            "• If you didn't mention a symptom on a day with entries, it is shown as \"not mentioned\", never as absent.\n" +
                            "• Average severity uses your own 1–3 ratings.\n" +
                            "• Comparisons need at least ${Insights.MIN_LOGGED_DAYS} days with entries in both periods. A difference is only described when " +
                            "frequency changes by ${(Insights.RATE_DIFFERENCE_THRESHOLD * 100).toInt()} percentage points or more, or average severity by " +
                            "${Insights.SEVERITY_DIFFERENCE_THRESHOLD} or more.\n" +
                            "• These are descriptions of what you recorded. They don't show causes and are not a diagnosis.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}
