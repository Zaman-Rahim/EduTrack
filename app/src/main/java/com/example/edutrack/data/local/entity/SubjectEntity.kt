package com.example.edutrack.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * Repeat frequency modes for a scheduled subject.
 */
enum class SubjectRepeatMode {
    ONCE,
    EVERY_DAY,
    CUSTOM
}

/**
 * Represents a study subject managed by the student with daily goal & weekly repeat schedule.
 */
@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val colorHex: Long,
    val weeklyGoalHours: Double,
    val dailyGoalMinutes: Int = 25,
    val repeatMode: String = SubjectRepeatMode.EVERY_DAY.name,
    /** Bitmask of DayOfWeek (bit 0 = Monday .. bit 6 = Sunday). 127 = all 7 days. */
    val scheduledDaysMask: Int = 127,
    val startDateEpochDay: Long = LocalDate.now().toEpochDay(),
    val createdAt: Long = System.currentTimeMillis()
) {
    fun isScheduledOn(date: LocalDate = LocalDate.now()): Boolean {
        val epochDay = date.toEpochDay()
        if (epochDay < startDateEpochDay) return false
        return when (repeatMode) {
            SubjectRepeatMode.ONCE.name -> epochDay == startDateEpochDay
            SubjectRepeatMode.EVERY_DAY.name -> true
            else -> {
                val bitIndex = date.dayOfWeek.value - 1 // Monday=0 .. Sunday=6
                (scheduledDaysMask and (1 shl bitIndex)) != 0
            }
        }
    }

    fun scheduledDaysPerWeekCount(): Int {
        return when (repeatMode) {
            SubjectRepeatMode.ONCE.name -> 1
            SubjectRepeatMode.EVERY_DAY.name -> 7
            else -> Integer.bitCount(scheduledDaysMask and 127).coerceAtLeast(1)
        }
    }

    fun formatScheduleSummary(): String {
        return when (repeatMode) {
            SubjectRepeatMode.ONCE.name -> "Once"
            SubjectRepeatMode.EVERY_DAY.name -> "Every day"
            else -> {
                val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                val active = labels.filterIndexed { index, _ ->
                    (scheduledDaysMask and (1 shl index)) != 0
                }
                if (active.size == 7) "Every day"
                else if (active.isEmpty()) "No days selected"
                else active.joinToString(" · ")
            }
        }
    }
}
