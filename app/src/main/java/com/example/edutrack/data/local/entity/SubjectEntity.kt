package com.example.edutrack.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a study subject managed by the student.
 *
 * @property id Auto-generated primary key.
 * @property name Display name of the subject (e.g., "Mobile Apps Dev").
 * @property colorHex ARGB color value used as a visual tag across screens.
 * @property weeklyGoalHours Target weekly study duration in hours.
 * @property createdAt Epoch millis when the subject was created.
 */
@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val colorHex: Long,
    val weeklyGoalHours: Double,
    val createdAt: Long = System.currentTimeMillis()
)
