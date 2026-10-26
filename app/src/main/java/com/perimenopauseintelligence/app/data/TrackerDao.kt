package com.perimenopauseintelligence.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackerDao {
    @Query("SELECT * FROM symptoms ORDER BY sortOrder, label")
    fun observeSymptoms(): Flow<List<SymptomEntity>>

    @Query("SELECT * FROM symptoms ORDER BY sortOrder, label")
    suspend fun symptoms(): List<SymptomEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSymptomsIgnoringExisting(items: List<SymptomEntity>)

    @Upsert
    suspend fun upsertSymptom(item: SymptomEntity)

    @Query("UPDATE symptoms SET archived = :archived WHERE id = :id")
    suspend fun setSymptomArchived(id: String, archived: Boolean)

    @Query("SELECT * FROM entries WHERE date = :date")
    fun observeEntry(date: String): Flow<EntryEntity?>

    @Query("SELECT * FROM entries WHERE date = :date")
    suspend fun entry(date: String): EntryEntity?

    @Query("SELECT * FROM observations WHERE date = :date")
    fun observeObservations(date: String): Flow<List<ObservationEntity>>

    @Query("SELECT * FROM observations WHERE date = :date")
    suspend fun observations(date: String): List<ObservationEntity>

    @Upsert
    suspend fun upsertEntry(item: EntryEntity)

    @Upsert
    suspend fun upsertObservation(item: ObservationEntity)

    @Query("DELETE FROM observations WHERE date = :date AND symptomId = :symptomId")
    suspend fun deleteObservation(date: String, symptomId: String)

    @Query("DELETE FROM entries WHERE date = :date")
    suspend fun deleteEntry(date: String)

    @Query("SELECT * FROM entries WHERE date BETWEEN :from AND :to ORDER BY date")
    fun observeEntriesBetween(from: String, to: String): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries WHERE date BETWEEN :from AND :to ORDER BY date")
    suspend fun entriesBetween(from: String, to: String): List<EntryEntity>

    @Query("SELECT * FROM observations WHERE date BETWEEN :from AND :to")
    fun observeObservationsBetween(from: String, to: String): Flow<List<ObservationEntity>>

    @Query("SELECT * FROM observations WHERE date BETWEEN :from AND :to")
    suspend fun observationsBetween(from: String, to: String): List<ObservationEntity>

    @Query("SELECT MIN(date) FROM entries")
    suspend fun firstEntryDate(): String?

    @Query("SELECT * FROM questions ORDER BY id")
    fun observeQuestions(): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions ORDER BY id")
    suspend fun questions(): List<QuestionEntity>

    @Insert
    suspend fun insertQuestion(item: QuestionEntity)

    @Query("DELETE FROM questions WHERE id = :id")
    suspend fun deleteQuestion(id: Long)

    @Query("DELETE FROM entries")
    suspend fun deleteAllEntries()

    @Query("DELETE FROM questions")
    suspend fun deleteAllQuestions()

    @Query("DELETE FROM symptoms WHERE isCustom = 1")
    suspend fun deleteCustomSymptoms()

    @Query("UPDATE symptoms SET archived = CASE WHEN id IN (:visibleIds) THEN 0 ELSE 1 END")
    suspend fun setVisibleSymptoms(visibleIds: List<String>)

    @Query("UPDATE symptoms SET archived = 0")
    suspend fun unarchiveAllSymptoms()
}
