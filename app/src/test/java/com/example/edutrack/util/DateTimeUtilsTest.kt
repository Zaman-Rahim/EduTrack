package com.example.edutrack.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DateTimeUtilsTest {

    @Test
    fun formatDuration_formatsMinutesAndHoursCorrectly() {
        assertEquals("0m", DateTimeUtils.formatDuration(0))
        assertEquals("45m", DateTimeUtils.formatDuration(45))
        assertEquals("1h", DateTimeUtils.formatDuration(60))
        assertEquals("1h 30m", DateTimeUtils.formatDuration(90))
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
