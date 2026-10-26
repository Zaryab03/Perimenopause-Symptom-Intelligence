package com.perimenopauseintelligence.app.domain

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

sealed interface ReportBlock {
    data class Title(val text: String) : ReportBlock
    data class Heading(val text: String) : ReportBlock
    data class Paragraph(val text: String) : ReportBlock
    data class Bullet(val text: String) : ReportBlock
    data class Caption(val text: String) : ReportBlock
}

data class ReportSpec(
    val title: String,
    val start: LocalDate,
    val end: LocalDate,
    /** null = every symptom. */
    val symptomIds: Set<String>? = null,
    val includeNotes: Boolean = true,
    val includeDailyLog: Boolean = true,
    val questions: List<String> = emptyList(),
)

/** Builds the report content as plain blocks so it can be tested without Android. */
object ReportBuilder {
    private val dayFormat = DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.ENGLISH)

    fun build(
        spec: ReportSpec,
        symptoms: Map<String, SymptomInfo>,
        records: List<DayRecord>,
        generatedOn: LocalDate,
    ): List<ReportBlock> {
        val out = mutableListOf<ReportBlock>()
        val summary = Insights.summarize(spec.start, spec.end, records)
        val inRange = records
            .filter { !it.date.isBefore(spec.start) && !it.date.isAfter(spec.end) }
            .sortedBy { it.date }

        out += ReportBlock.Title(spec.title)
        out += ReportBlock.Caption("Period: ${fmt(spec.start)} to ${fmt(spec.end)}  |  Created: ${fmt(generatedOn)}")
        out += ReportBlock.Paragraph(
            "This report contains information the user recorded themselves. It is not a diagnosis or a " +
                "clinical assessment, and it does not show causes. It is intended as a starting point for a " +
                "conversation with a healthcare professional.",
        )

        out += ReportBlock.Heading("How complete is this record?")
        out += ReportBlock.Bullet("Days in period: ${summary.totalDays}")
        out += ReportBlock.Bullet("Days with an entry: ${summary.loggedDays}")
        out += ReportBlock.Bullet("Days skipped on purpose: ${summary.skippedDays}")
        out += ReportBlock.Bullet("Days with no entry: ${summary.noEntryDays} (these are unknown, not symptom-free)")
        if (!summary.sufficient) {
            out += ReportBlock.Paragraph(
                "There are fewer than ${Insights.MIN_LOGGED_DAYS} days with entries, so no summary of how " +
                    "often symptoms occurred is shown. The daily log below lists what was recorded.",
            )
        }

        if (summary.sufficient) {
            out += ReportBlock.Heading("Symptom summary")
            val selected = symptoms.values
                .filter { spec.symptomIds == null || it.id in spec.symptomIds }
                .map { it to summary.stat(it.id) }
                .filter { (_, s) -> s.presentDays > 0 || s.absentDays > 0 || spec.symptomIds != null }
                .sortedWith(compareByDescending<Pair<SymptomInfo, SymptomStat>> { it.second.presentDays }.thenBy { it.first.label })
            if (selected.isEmpty()) {
                out += ReportBlock.Paragraph("No symptoms were recorded in this period.")
            }
            for ((info, stat) in selected) out += ReportBlock.Bullet(InsightText.frequency(info.label, stat))
            out += ReportBlock.Caption(
                "Severity is the user's own rating: 1 = mild, 2 = moderate, 3 = severe. " +
                    "\"Not mentioned\" means nothing was recorded for that symptom on a day with entries.",
            )

            val context = listOfNotNull(
                summary.sleepAverage?.let { "Sleep (1 = poor, 5 = good): average ${"%.1f".format(it.value)} over ${it.count} days" },
                summary.moodAverage?.let { "Mood (1 = low, 5 = good): average ${"%.1f".format(it.value)} over ${it.count} days" },
                summary.energyAverage?.let { "Energy (1 = low, 5 = high): average ${"%.1f".format(it.value)} over ${it.count} days" },
            )
            if (context.isNotEmpty()) {
                out += ReportBlock.Heading("Sleep, mood and energy")
                context.forEach { out += ReportBlock.Bullet(it) }
            }
        }

        val periodDays = inRange.filter { it.cycle == CycleFlag.PERIOD }.size
        val spottingDays = inRange.filter { it.cycle == CycleFlag.SPOTTING }.size
        if (periodDays + spottingDays > 0) {
            out += ReportBlock.Heading("Menstrual bleeding")
            out += ReportBlock.Bullet("Days recorded as period: $periodDays")
            out += ReportBlock.Bullet("Days recorded as spotting: $spottingDays")
        }

        if (spec.questions.isNotEmpty()) {
            out += ReportBlock.Heading("Questions for my clinician")
            spec.questions.forEach { out += ReportBlock.Bullet(it) }
        }

        if (spec.includeDailyLog) {
            out += ReportBlock.Heading("Daily log")
            val visible = inRange.filter { it.status == EntryStatus.LOGGED }
            if (visible.isEmpty()) out += ReportBlock.Paragraph("No entries in this period.")
            for (d in visible) {
                val present = d.observations.filter { it.state == ObsState.PRESENT }.joinToString(", ") { o ->
                    val name = symptoms[o.symptomId]?.label ?: o.symptomId
                    if (o.severity != null) "$name (${Severity.label(o.severity).lowercase()})" else name
                }
                val absent = d.observations.filter { it.state == ObsState.ABSENT }
                    .joinToString(", ") { symptoms[it.symptomId]?.label ?: it.symptomId }
                val bits = mutableListOf<String>()
                if (present.isNotEmpty()) bits += "Recorded: $present"
                if (absent.isNotEmpty()) bits += "Marked not today: $absent"
                d.sleep?.let { bits += "Sleep $it/5" }
                d.mood?.let { bits += "Mood $it/5" }
                d.energy?.let { bits += "Energy $it/5" }
                d.cycle?.let { bits += it.label }
                if (spec.includeNotes && d.notes.isNotBlank()) bits += "Notes: ${d.notes.trim()}"
                out += ReportBlock.Bullet("${fmt(d.date)}: ${bits.joinToString(". ").ifEmpty { "Entry with no details" }}")
            }
        }
        return out
    }

    private fun fmt(d: LocalDate): String = dayFormat.format(d)
}
