package com.example.edutrack.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents an individual logged study session linked to a [SubjectEntity].
 * Deleting a parent subject automatically cascades and removes its sessions.
 *
 * @property id Auto-generated primary key.
 * @property subjectId Foreign key referencing [SubjectEntity.id].
 * @property durationMinutes Duration of the study session in minutes.
 * @property sessionDateEpochDay Calendar date stored as LocalDate.toEpochDay().
 * @property notes Summary of topics covered or study reflections.
 * @property createdAt Epoch millis when the record was created.
 */
@Entity(
    tableName = "study_sessions",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["subjectId"]), Index(value = ["sessionDateEpochDay"])]
)
data class StudySessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val subjectId: Long,
    val durationMinutes: Int,
    val sessionDateEpochDay: Long,
    val notes: String,
    val createdAt: Long = System.currentTimeMillis()
)
