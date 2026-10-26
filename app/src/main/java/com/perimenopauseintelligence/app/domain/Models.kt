package com.perimenopauseintelligence.app.domain

import java.time.LocalDate

/** A symptom is either explicitly present, explicitly absent ("not today"), or simply not recorded (no row). */
enum class ObsState { PRESENT, ABSENT }

/** LOGGED = the user recorded something. SKIPPED = the user chose to skip. No entry at all = missing. */
enum class EntryStatus { LOGGED, SKIPPED }

enum class CycleFlag(val label: String) {
    PERIOD("Period"),
    SPOTTING("Spotting"),
    NO_BLEEDING("No bleeding"),
    NOT_APPLICABLE("Doesn't apply to me"),
}

object Severity {
    const val MIN = 1
    const val MAX = 3
    fun label(value: Int): String = when (value) {
        1 -> "Mild"
        2 -> "Moderate"
        3 -> "Severe"
        else -> "Unknown"
    }
}

object Ratings {
    const val MIN = 1
    const val MAX = 5
}

data class SymptomInfo(
    val id: String,
    val label: String,
    val category: String,
    val isCustom: Boolean = false,
    val archived: Boolean = false,
)

data class ObservationRecord(
    val symptomId: String,
    val state: ObsState,
    val severity: Int?,
)

data class DayRecord(
    val date: LocalDate,
    val status: EntryStatus,
    val notes: String = "",
    val sleep: Int? = null,
    val mood: Int? = null,
    val energy: Int? = null,
    val cycle: CycleFlag? = null,
    val observations: List<ObservationRecord> = emptyList(),
) {
    val isEmpty: Boolean
        get() = notes.isBlank() && sleep == null && mood == null && energy == null &&
            cycle == null && observations.isEmpty()
}
