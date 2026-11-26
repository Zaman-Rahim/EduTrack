package com.example.edutrack.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ReminderRepeatType(val label: String) {
    NONE("Does not repeat"),
    DAILY("Every day"),
    WEEKDAYS("Weekdays (Mon–Fri)"),
    WEEKLY("Every week")
}

enum class ReminderPriority(val label: String) {
    LOW("Low"),
    NORMAL("Normal"),
    HIGH("High")
}

/**
 * Represents a personal task or reminder independent of study subjects.
 */
@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val notes: String = "",
    val dueDateEpochDay: Long,
    /** Minutes from midnight (0..1439), e.g., 540 = 9:00 AM. */
    val dueTimeMinutes: Int,
    val repeatType: String = ReminderRepeatType.NONE.name,
    val priority: String = ReminderPriority.NORMAL.name,
    val notifyEnabled: Boolean = true,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
