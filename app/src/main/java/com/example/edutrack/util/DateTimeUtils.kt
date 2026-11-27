package com.example.edutrack.util

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Date, duration, week boundary, and study streak utilities using [LocalDate] epoch days.
 */
object DateTimeUtils {

    private val shortDateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())
    private val fullDateFormatter = DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy", Locale.getDefault())
    private val dayLabelFormatter = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())

    fun todayEpochDay(today: LocalDate = LocalDate.now()): Long = today.toEpochDay()

    /**
     * Resolves the user preference string ("Sunday" or "Monday") to a [DayOfWeek].
     */
    fun firstDayOfWeek(weekStartsOn: String): DayOfWeek =
        if (weekStartsOn.equals("Sunday", ignoreCase = true)) DayOfWeek.SUNDAY else DayOfWeek.MONDAY

    /**
     * Returns the inclusive [startEpochDay, endEpochDay] for the current week
     * starting on [weekStartsOn] ("Monday" or "Sunday").
     */
    fun currentWeekBounds(
        weekStartsOn: String = "Monday",
        today: LocalDate = LocalDate.now()
    ): Pair<Long, Long> = weekBoundsForOffset(
        weekOffset = 0,
        weekStartsOn = weekStartsOn,
        today = today
    )

    /**
     * Returns the inclusive [startEpochDay, endEpochDay] for the week offset by
     * [weekOffset] weeks relative to [today], starting on [weekStartsOn].
     */
    fun weekBoundsForOffset(
        weekOffset: Int = 0,
        weekStartsOn: String = "Monday",
        today: LocalDate = LocalDate.now()
    ): Pair<Long, Long> {
        val startDay = firstDayOfWeek(weekStartsOn)
        val weekStart = today
            .with(TemporalAdjusters.previousOrSame(startDay))
            .plusWeeks(weekOffset.toLong())
        val weekEnd = weekStart.plusDays(6)
        return weekStart.toEpochDay() to weekEnd.toEpochDay()
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
     * Formats decimal hours as rounded hours and minutes (e.g., 1.5 -> "1h 30m").
     */
    fun formatHoursAsHoursMinutes(hours: Double): String {
        if (!hours.isFinite()) return "0m"
        return formatDuration((hours * 60.0).roundToInt().coerceAtLeast(0))
    }

    /**
     * Formats countdown seconds into (displayTime, unitLabel):
     * ("MM:SS", "MINUTES : SECONDS") under 1 hour, and
     * ("H:MM:SS", "HOURS : MINUTES : SECONDS") at 1 hour or more.
     */
    fun formatTimerCountdown(totalSeconds: Int): Pair<String, String> {
        val safeSeconds = totalSeconds.coerceAtLeast(0)
        return if (safeSeconds < 3600) {
            val mins = safeSeconds / 60
            val secs = safeSeconds % 60
            String.format(Locale.getDefault(), "%02d:%02d", mins, secs) to "MINUTES : SECONDS"
        } else {
            val hours = safeSeconds / 3600
            val mins = (safeSeconds % 3600) / 60
            val secs = safeSeconds % 60
            String.format(Locale.getDefault(), "%d:%02d:%02d", hours, mins, secs) to "HOURS : MINUTES : SECONDS"
        }
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
