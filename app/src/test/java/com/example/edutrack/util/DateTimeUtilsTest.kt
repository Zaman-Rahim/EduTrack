package com.example.edutrack.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DateTimeUtilsTest {

    @Test
    fun formatDuration_formatsMinutesAndHoursCorrectly() {
        assertEquals("0m", DateTimeUtils.formatDuration(0))
        assertEquals("0m", DateTimeUtils.formatDuration(-1))
        assertEquals("0m", DateTimeUtils.formatDuration(-30))
        assertEquals("1m", DateTimeUtils.formatDuration(1))
        assertEquals("45m", DateTimeUtils.formatDuration(45))
        assertEquals("1h", DateTimeUtils.formatDuration(60))
        assertEquals("2h", DateTimeUtils.formatDuration(120))
        assertEquals("1h 30m", DateTimeUtils.formatDuration(90))
        assertEquals("2h 5m", DateTimeUtils.formatDuration(125))
    }

    @Test
    fun formatHoursAsHoursMinutes_handlesZeroNegativeFiniteAndNonFiniteValues() {
        assertEquals("0m", DateTimeUtils.formatHoursAsHoursMinutes(0.0))
        assertEquals("0m", DateTimeUtils.formatHoursAsHoursMinutes(-1.5))
        assertEquals("30m", DateTimeUtils.formatHoursAsHoursMinutes(0.5))
        assertEquals("1h", DateTimeUtils.formatHoursAsHoursMinutes(1.0))
        assertEquals("1h 30m", DateTimeUtils.formatHoursAsHoursMinutes(1.5))
        assertEquals("1h 15m", DateTimeUtils.formatHoursAsHoursMinutes(1.25))
        assertEquals("0m", DateTimeUtils.formatHoursAsHoursMinutes(Double.NaN))
        assertEquals("0m", DateTimeUtils.formatHoursAsHoursMinutes(Double.POSITIVE_INFINITY))
        assertEquals("0m", DateTimeUtils.formatHoursAsHoursMinutes(Double.NEGATIVE_INFINITY))
    }

    @Test
    fun formatTimerCountdown_formatsUnderAndAtOrAboveOneHour() {
        assertEquals("00:00" to "MINUTES : SECONDS", DateTimeUtils.formatTimerCountdown(-5))
        assertEquals("00:00" to "MINUTES : SECONDS", DateTimeUtils.formatTimerCountdown(0))
        assertEquals("01:05" to "MINUTES : SECONDS", DateTimeUtils.formatTimerCountdown(65))
        assertEquals("59:59" to "MINUTES : SECONDS", DateTimeUtils.formatTimerCountdown(3599))

        assertEquals("1:00:00" to "HOURS : MINUTES : SECONDS", DateTimeUtils.formatTimerCountdown(3600))
        assertEquals("1:01:05" to "HOURS : MINUTES : SECONDS", DateTimeUtils.formatTimerCountdown(3665))
        assertEquals("2:02:02" to "HOURS : MINUTES : SECONDS", DateTimeUtils.formatTimerCountdown(7322))
    }

    @Test
    fun weekBoundsForOffset_handlesMondayAndSundayStartDays() {
        val thursday = LocalDate.of(2026, 11, 26)
        assertEquals(
            LocalDate.of(2026, 11, 23).toEpochDay() to LocalDate.of(2026, 11, 29).toEpochDay(),
            DateTimeUtils.weekBoundsForOffset(weekOffset = 0, weekStartsOn = "Monday", today = thursday)
        )
        assertEquals(
            LocalDate.of(2026, 11, 16).toEpochDay() to LocalDate.of(2026, 11, 22).toEpochDay(),
            DateTimeUtils.weekBoundsForOffset(weekOffset = -1, weekStartsOn = "Monday", today = thursday)
        )
        assertEquals(
            LocalDate.of(2026, 11, 30).toEpochDay() to LocalDate.of(2026, 12, 6).toEpochDay(),
            DateTimeUtils.weekBoundsForOffset(weekOffset = 1, weekStartsOn = "Monday", today = thursday)
        )

        assertEquals(
            LocalDate.of(2026, 11, 22).toEpochDay() to LocalDate.of(2026, 11, 28).toEpochDay(),
            DateTimeUtils.weekBoundsForOffset(weekOffset = 0, weekStartsOn = "Sunday", today = thursday)
        )
        assertEquals(
            LocalDate.of(2026, 11, 15).toEpochDay() to LocalDate.of(2026, 11, 21).toEpochDay(),
            DateTimeUtils.weekBoundsForOffset(weekOffset = -1, weekStartsOn = "Sunday", today = thursday)
        )
        assertEquals(
            LocalDate.of(2026, 11, 29).toEpochDay() to LocalDate.of(2026, 12, 5).toEpochDay(),
            DateTimeUtils.weekBoundsForOffset(weekOffset = 1, weekStartsOn = "Sunday", today = thursday)
        )

        // Boundary days when today itself is Sunday or Monday
        val sunday = LocalDate.of(2026, 11, 29)
        assertEquals(
            LocalDate.of(2026, 11, 23).toEpochDay() to LocalDate.of(2026, 11, 29).toEpochDay(),
            DateTimeUtils.weekBoundsForOffset(weekOffset = 0, weekStartsOn = "Monday", today = sunday)
        )
        assertEquals(
            LocalDate.of(2026, 11, 29).toEpochDay() to LocalDate.of(2026, 12, 5).toEpochDay(),
            DateTimeUtils.weekBoundsForOffset(weekOffset = 0, weekStartsOn = "Sunday", today = sunday)
        )

        val monday = LocalDate.of(2026, 11, 23)
        assertEquals(
            LocalDate.of(2026, 11, 23).toEpochDay() to LocalDate.of(2026, 11, 29).toEpochDay(),
            DateTimeUtils.weekBoundsForOffset(weekOffset = 0, weekStartsOn = "Monday", today = monday)
        )
        assertEquals(
            LocalDate.of(2026, 11, 22).toEpochDay() to LocalDate.of(2026, 11, 28).toEpochDay(),
            DateTimeUtils.weekBoundsForOffset(weekOffset = 0, weekStartsOn = "Sunday", today = monday)
        )
    }

    @Test
    fun calculateStreaks_handlesConsecutiveAndBrokenStreaks() {
        val today = LocalDate.of(2026, 11, 26).toEpochDay()
        val days = listOf(
            today - 10,
            today - 9,
            today - 8,
            today - 7, // 4-day historical streak
            today - 2,
            today - 1,
            today,
            today // duplicate session on today should not inflate streak
        )
        val (current, longest) = DateTimeUtils.calculateStreaks(days, todayEpochDay = today)
        assertEquals(3, current)
        assertEquals(4, longest)
    }

    @Test
    fun calculateStreaks_returnsZeroCurrentWhenYesterdayAndTodayMissing() {
        val today = LocalDate.of(2026, 11, 26).toEpochDay()
        val days = listOf(today - 4, today - 3)
        val (current, longest) = DateTimeUtils.calculateStreaks(days, todayEpochDay = today)
        assertEquals(0, current)
        assertEquals(2, longest)
    }

    @Test
    fun calculateStreaks_handlesEmptyInput() {
        val (current, longest) = DateTimeUtils.calculateStreaks(emptyList())
        assertEquals(0, current)
        assertEquals(0, longest)
    }
}
