package com.perimenopauseintelligence.app.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class SymptomStat(
    val symptomId: String,
    val presentDays: Int,
    val absentDays: Int,
    val loggedDays: Int,
    val severityCount: Int,
    val severitySum: Int,
) {
    /** Logged days on which this symptom was neither marked present nor "not today". */
    val unmentionedDays: Int get() = loggedDays - presentDays - absentDays
    val averageSeverity: Double? get() = if (severityCount == 0) null else severitySum.toDouble() / severityCount
    val rate: Double get() = if (loggedDays == 0) 0.0 else presentDays.toDouble() / loggedDays
}

data class PeriodSummary(
    val start: LocalDate,
    val end: LocalDate,
    val totalDays: Int,
    val loggedDays: Int,
    val skippedDays: Int,
    val stats: Map<String, SymptomStat>,
    val sleepAverage: Average?,
    val moodAverage: Average?,
    val energyAverage: Average?,
) {
    val noEntryDays: Int get() = totalDays - loggedDays - skippedDays
    val sufficient: Boolean get() = loggedDays >= Insights.MIN_LOGGED_DAYS

    fun stat(symptomId: String): SymptomStat =
        stats[symptomId] ?: SymptomStat(symptomId, 0, 0, loggedDays, 0, 0)
}

data class Average(val value: Double, val count: Int)

enum class Direction { MORE, LESS, SIMILAR }

/** A null direction means "not enough data to compare", never "no difference". */
data class Comparison(val frequency: Direction?, val severity: Direction?)

/**
 * Deterministic, transparent calculations. Nothing here estimates, predicts or infers a cause.
 */
object Insights {
    const val MIN_LOGGED_DAYS = 5
    const val MIN_SEVERITY_OBSERVATIONS = 3
    const val RATE_DIFFERENCE_THRESHOLD = 0.15
    const val SEVERITY_DIFFERENCE_THRESHOLD = 0.5

    fun summarize(start: LocalDate, end: LocalDate, records: List<DayRecord>): PeriodSummary {
        require(!end.isBefore(start)) { "end must not be before start" }
        val inRange = records.filter { !it.date.isBefore(start) && !it.date.isAfter(end) }
        val logged = inRange.filter { it.status == EntryStatus.LOGGED }
        val skipped = inRange.count { it.status == EntryStatus.SKIPPED }

        val present = HashMap<String, Int>()
        val absent = HashMap<String, Int>()
        val sevCount = HashMap<String, Int>()
        val sevSum = HashMap<String, Int>()
        for (day in logged) {
            for (o in day.observations) {
                when (o.state) {
                    ObsState.PRESENT -> {
                        present.merge(o.symptomId, 1, Int::plus)
                        if (o.severity != null) {
                            sevCount.merge(o.symptomId, 1, Int::plus)
                            sevSum.merge(o.symptomId, o.severity, Int::plus)
                        }
                    }
                    ObsState.ABSENT -> absent.merge(o.symptomId, 1, Int::plus)
                }
            }
        }
        val ids = present.keys + absent.keys
        val stats = ids.associateWith { id ->
            SymptomStat(id, present[id] ?: 0, absent[id] ?: 0, logged.size, sevCount[id] ?: 0, sevSum[id] ?: 0)
        }
        return PeriodSummary(
            start = start,
            end = end,
            totalDays = ChronoUnit.DAYS.between(start, end).toInt() + 1,
            loggedDays = logged.size,
            skippedDays = skipped,
            stats = stats,
            sleepAverage = average(logged.mapNotNull { it.sleep }),
            moodAverage = average(logged.mapNotNull { it.mood }),
            energyAverage = average(logged.mapNotNull { it.energy }),
        )
    }

    private fun average(values: List<Int>): Average? =
        if (values.isEmpty()) null else Average(values.sum().toDouble() / values.size, values.size)

    fun compare(current: PeriodSummary, previous: PeriodSummary, symptomId: String): Comparison {
        if (!current.sufficient || !previous.sufficient) return Comparison(null, null)
        val a = current.stat(symptomId)
        val b = previous.stat(symptomId)
        val frequency = direction(a.rate - b.rate, RATE_DIFFERENCE_THRESHOLD)
        val severity =
            if (a.severityCount >= MIN_SEVERITY_OBSERVATIONS && b.severityCount >= MIN_SEVERITY_OBSERVATIONS) {
                direction(a.averageSeverity!! - b.averageSeverity!!, SEVERITY_DIFFERENCE_THRESHOLD)
            } else {
                null
            }
        return Comparison(frequency, severity)
    }

    private fun direction(diff: Double, threshold: Double): Direction = when {
        diff >= threshold -> Direction.MORE
        diff <= -threshold -> Direction.LESS
        else -> Direction.SIMILAR
    }
}

/** Wording for insights. Observations only: no causation, diagnosis or prediction. */
object InsightText {
    fun frequency(label: String, s: SymptomStat): String {
        val parts = mutableListOf("$label was recorded on ${days(s.presentDays)} out of ${days(s.loggedDays)} with entries.")
        if (s.absentDays > 0) parts += "You marked \"not today\" on ${days(s.absentDays)}."
        if (s.unmentionedDays > 0) parts += "It wasn't mentioned on ${days(s.unmentionedDays)}."
        s.averageSeverity?.let { parts += "Average recorded severity: ${"%.1f".format(it)} of ${Severity.MAX}." }
        return parts.joinToString(" ")
    }

    fun comparison(label: String, c: Comparison): String? {
        val f = c.frequency ?: return null
        val freq = when (f) {
            Direction.MORE -> "$label was recorded more often in this period than in the previous one."
            Direction.LESS -> "$label was recorded less often in this period than in the previous one."
            Direction.SIMILAR -> "$label was recorded about as often as in the previous period."
        }
        val sev = when (c.severity) {
            Direction.MORE -> " Recorded severity was higher."
            Direction.LESS -> " Recorded severity was lower."
            Direction.SIMILAR -> " Recorded severity was similar."
            null -> ""
        }
        return freq + sev
    }

    fun days(n: Int): String = if (n == 1) "1 day" else "$n days"
}
