package com.perimenopauseintelligence.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "symptoms")
data class SymptomEntity(
    @PrimaryKey val id: String,
    val label: String,
    val category: String,
    val isCustom: Boolean,
    val archived: Boolean = false,
    val sortOrder: Int = 0,
)

/** Keyed by the user's local calendar date (ISO yyyy-MM-dd) so a timezone change never moves an entry. */
@Entity(tableName = "entries")
data class EntryEntity(
    @PrimaryKey val date: String,
    val status: String,
    val notes: String = "",
    val sleep: Int? = null,
    val mood: Int? = null,
    val energy: Int? = null,
    val cycle: String? = null,
    val updatedAt: Long,
)

@Entity(
    tableName = "observations",
    primaryKeys = ["date", "symptomId"],
    foreignKeys = [
        ForeignKey(EntryEntity::class, ["date"], ["date"], onDelete = ForeignKey.CASCADE),
        ForeignKey(SymptomEntity::class, ["id"], ["symptomId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("symptomId")],
)
data class ObservationEntity(
    val date: String,
    val symptomId: String,
    val state: String,
    val severity: Int?,
)

@Entity(tableName = "questions")
data class QuestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
)
