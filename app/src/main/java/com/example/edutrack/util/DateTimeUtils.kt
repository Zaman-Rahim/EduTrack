package com.example.edutrack.util

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/**
 * Date, duration, week boundary, and study streak utilities using [LocalDate] epoch days.
 */
object DateTimeUtils {

    private val shortDateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())
    private val fullDateFormatter = DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy", Locale.getDefault())
    private val dayLabelFormatter = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())

    fun todayEpochDay(today: LocalDate = LocalDate.now()): Long = today.toEpochDay()

    /**
     * Returns the inclusive [startEpochDay, endEpochDay] for the current Monday–Sunday week.
     */
    fun currentWeekBounds(today: LocalDate = LocalDate.now()): Pair<Long, Long> {
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val sunday = monday.plusDays(6)
        return monday.toEpochDay() to sunday.toEpochDay()
    }

    /**
     * Returns the inclusive [startEpochDay, endEpochDay] for the current calendar month.
     */
    fun currentMonthBounds(today: LocalDate = LocalDate.now()): Pair<Long, Long> {
        val first = today.withDayOfMonth(1)
        val last = today.withDayOfMonth(today.lengthOfMonth())
        return first.toEpochDay() to last.toEpochDay()
    }

    fun formatShortDate(epochDay: Long): String =
        LocalDate.ofEpochDay(epochDay).format(shortDateFormatter)

    fun formatFullDate(epochDay: Long): String =
        LocalDate.ofEpochDay(epochDay).format(fullDateFormatter)

    fun formatDayLabel(epochDay: Long): String =
        LocalDate.ofEpochDay(epochDay).format(dayLabelFormatter)

    fun epochDayToUtcMillis(epochDay: Long): Long =
        LocalDate.ofEpochDay(epochDay)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()

    fun utcMillisToEpochDay(utcMillis: Long): Long =
        Instant.ofEpochMilli(utcMillis)
            .atZone(ZoneOffset.UTC)
            .toLocalDate()
            .toEpochDay()

    /**
     * Formats minutes cleanly (e.g., 90 -> "1h 30m", 45 -> "45m", 120 -> "2h").
     */
    fun formatDuration(totalMinutes: Int): String {
        if (totalMinutes <= 0) return "0m"
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        return when {
            hours > 0 && mins > 0 -> "${hours}h ${mins}m"
            hours > 0 -> "${hours}h"
            else -> "${mins}m"
        }
    }

    /**
     * Formats minutes as decimal hours with one decimal place (e.g., 90 -> "1.5 hrs").
     */
    fun formatMinutesAsHours(totalMinutes: Int): String {
        val hours = totalMinutes / 60.0
        return String.format(Locale.getDefault(), "%.1f hrs", hours)
    }

    /**
     * Computes (currentStreakDays, longestStreakDays) from a collection of session epoch days.
     * Current streak remains active if the student studied today or yesterday.
     */
    fun calculateStreaks(
        sessionEpochDays: Collection<Long>,
        todayEpochDay: Long = LocalDate.now().toEpochDay()
    ): Pair<Int, Int> {
        if (sessionEpochDays.isEmpty()) return 0 to 0

        val uniqueDays = sessionEpochDays.toSortedSet()
        var longest = 1
        var run = 1
        val sortedList = uniqueDays.toList()
        for (i in 1 until sortedList.size) {
            if (sortedList[i] == sortedList[i - 1] + 1L) {
                run++
                if (run > longest) longest = run
            } else {
                run = 1
            }
        }

        val lastStudyDay = sortedList.last()
        val current = if (lastStudyDay == todayEpochDay || lastStudyDay == todayEpochDay - 1L) {
            var streak = 1
            var cursor = lastStudyDay
            while (uniqueDays.contains(cursor - 1L)) {
                streak++
                cursor--
            }
            streak
        } else {
            0
        }

        return current to longest
    }
}
