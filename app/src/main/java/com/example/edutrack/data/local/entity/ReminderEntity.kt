package com.example.edutrack.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ReminderRepeatType(val label: String) {
    NONE("Does not repeat"),
    DAILY("Every day"),
    WEEKDAYS("Weekdays (Mon–Fri)"),
    WEEKLY("Every week"),
    MONTHLY("Every month"),
    CUSTOM("Custom weekdays")
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
    /** 7-bit bitmask (bit 0 = Monday .. bit 6 = Sunday) used when repeatType == CUSTOM. */
    val repeatDaysMask: Int = 0b0010101,
    val priority: String = ReminderPriority.NORMAL.name,
    val notifyEnabled: Boolean = true,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun formatRepeatSummary(): String {
        val type = runCatching { ReminderRepeatType.valueOf(repeatType) }
            .getOrDefault(ReminderRepeatType.NONE)
        if (type != ReminderRepeatType.CUSTOM) return type.label
        val shortDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val selected = shortDays.filterIndexed { index, _ -> (repeatDaysMask and (1 shl index)) != 0 }
        return if (selected.isEmpty()) "Does not repeat" else selected.joinToString(" · ")
    }
}
