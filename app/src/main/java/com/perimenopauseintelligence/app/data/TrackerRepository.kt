package com.perimenopauseintelligence.app.data

import androidx.room.withTransaction
import com.perimenopauseintelligence.app.domain.CycleFlag
import com.perimenopauseintelligence.app.domain.DayRecord
import com.perimenopauseintelligence.app.domain.EntryStatus
import com.perimenopauseintelligence.app.domain.ObsState
import com.perimenopauseintelligence.app.domain.ObservationRecord
import com.perimenopauseintelligence.app.domain.SymptomInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.UUID

class TrackerRepository(private val db: AppDatabase) {
    private val dao = db.dao()

    suspend fun seedDefaults() = dao.insertSymptomsIgnoringExisting(DefaultSymptoms.all)

    fun observeSymptoms(): Flow<List<SymptomInfo>> = dao.observeSymptoms().map { list -> list.map { it.toInfo() } }
    suspend fun symptoms(): List<SymptomInfo> = dao.symptoms().map { it.toInfo() }

    suspend fun addCustomSymptom(label: String, category: String) {
        val clean = label.trim()
        if (clean.isEmpty()) return
        dao.upsertSymptom(
            SymptomEntity("custom_" + UUID.randomUUID(), clean.take(60), category, isCustom = true, sortOrder = 1000),
        )
    }

    /** Shows exactly these symptoms in the daily log and hides the rest. Hiding never deletes recorded data. */
    suspend fun setVisibleSymptoms(ids: Set<String>) {
        seedDefaults()
        dao.setVisibleSymptoms(ids.toList())
    }

    suspend fun setSymptomArchived(id: String, archived: Boolean) = dao.setSymptomArchived(id, archived)

    fun observeDay(date: LocalDate): Flow<DayRecord?> =
        combine(dao.observeEntry(date.toString()), dao.observeObservations(date.toString())) { e, o ->
            e?.toRecord(o)
        }

    fun observeRecords(from: LocalDate, to: LocalDate): Flow<List<DayRecord>> =
        combine(
            dao.observeEntriesBetween(from.toString(), to.toString()),
            dao.observeObservationsBetween(from.toString(), to.toString()),
        ) { entries, obs ->
            val byDate = obs.groupBy { it.date }
            entries.map { it.toRecord(byDate[it.date].orEmpty()) }
        }

    suspend fun records(from: LocalDate, to: LocalDate): List<DayRecord> {
        val byDate = dao.observationsBetween(from.toString(), to.toString()).groupBy { it.date }
        return dao.entriesBetween(from.toString(), to.toString()).map { it.toRecord(byDate[it.date].orEmpty()) }
    }

    suspend fun firstEntryDate(): LocalDate? = dao.firstEntryDate()?.let(LocalDate::parse)

    /** state == null clears the observation (back to "not recorded"). */
    suspend fun setObservation(date: LocalDate, symptomId: String, state: ObsState?, severity: Int?) {
        val d = date.toString()
        db.withTransaction {
            if (state == null) {
                dao.deleteObservation(d, symptomId)
            } else {
                ensureLoggedEntry(d)
                dao.upsertObservation(ObservationEntity(d, symptomId, state.name, if (state == ObsState.PRESENT) severity else null))
            }
            deleteIfEmpty(d)
        }
    }

    suspend fun updateEntry(date: LocalDate, change: (EntryEntity) -> EntryEntity) {
        val d = date.toString()
        db.withTransaction {
            val current = ensureLoggedEntry(d)
            dao.upsertEntry(change(current).copy(updatedAt = System.currentTimeMillis()))
            deleteIfEmpty(d)
        }
    }

    suspend fun skipDay(date: LocalDate) {
        val d = date.toString()
        db.withTransaction {
            if (dao.entry(d) == null) dao.upsertEntry(EntryEntity(d, EntryStatus.SKIPPED.name, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun deleteDay(date: LocalDate) = dao.deleteEntry(date.toString())

    fun observeQuestions(): Flow<List<QuestionEntity>> = dao.observeQuestions()
    suspend fun questions(): List<String> = dao.questions().map { it.text }
    suspend fun addQuestion(text: String) {
        if (text.isNotBlank()) dao.insertQuestion(QuestionEntity(text = text.trim().take(500)))
    }
    suspend fun deleteQuestion(id: Long) = dao.deleteQuestion(id)

    /** Removes every health record, custom symptom and question. The default catalogue is restored. */
    suspend fun deleteEverything() {
        db.withTransaction {
            dao.deleteAllEntries()
            dao.deleteAllQuestions()
            dao.deleteCustomSymptoms()
            dao.unarchiveAllSymptoms()
        }
    }

    private suspend fun ensureLoggedEntry(d: String): EntryEntity {
        val existing = dao.entry(d)
        val entry = when {
            existing == null -> EntryEntity(d, EntryStatus.LOGGED.name, updatedAt = System.currentTimeMillis())
            existing.status != EntryStatus.LOGGED.name -> existing.copy(status = EntryStatus.LOGGED.name)
            else -> existing
        }
        if (entry != existing) dao.upsertEntry(entry)
        return entry
    }

    private suspend fun deleteIfEmpty(d: String) {
        val e = dao.entry(d) ?: return
        if (e.status == EntryStatus.LOGGED.name && e.toRecord(dao.observations(d)).isEmpty) dao.deleteEntry(d)
    }
}

private fun SymptomEntity.toInfo() = SymptomInfo(id, label, category, isCustom, archived)

private fun EntryEntity.toRecord(obs: List<ObservationEntity>) = DayRecord(
    date = LocalDate.parse(date),
    status = EntryStatus.valueOf(status),
    notes = notes,
    sleep = sleep,
    mood = mood,
    energy = energy,
    cycle = cycle?.let { runCatching { CycleFlag.valueOf(it) }.getOrNull() },
    observations = obs.map { ObservationRecord(it.symptomId, ObsState.valueOf(it.state), it.severity) },
)
