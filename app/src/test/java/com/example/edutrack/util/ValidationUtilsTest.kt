package com.example.edutrack.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidationUtilsTest {

    @Test
    fun validateStudySession_rejectsEmptyAndInvalidInputs() {
        val result = ValidationUtils.validateStudySession(
            selectedSubjectId = null,
            durationText = "abc",
            sessionDateEpochDay = null,
            notes = "  "
        )
        assertFalse(result.isValid)
        assertNotNull(result.subjectError)
        assertNotNull(result.durationError)
        assertNotNull(result.dateError)
        assertNotNull(result.notesError)
    }

    @Test
    fun validateStudySession_rejectsZeroAndExcessiveDuration() {
        val zeroResult = ValidationUtils.validateStudySession(
            selectedSubjectId = 1L,
            durationText = "0",
            sessionDateEpochDay = 20000L,
            notes = "Reviewed Room DAO"
        )
        assertFalse(zeroResult.isValid)
        assertNotNull(zeroResult.durationError)

        val tooLongResult = ValidationUtils.validateStudySession(
            selectedSubjectId = 1L,
            durationText = "1500",
            sessionDateEpochDay = 20000L,
            notes = "Reviewed Room DAO"
        )
        assertFalse(tooLongResult.isValid)
        assertNotNull(tooLongResult.durationError)
    }

    @Test
    fun validateStudySession_acceptsValidInput() {
        val result = ValidationUtils.validateStudySession(
            selectedSubjectId = 1L,
            durationText = "75",
            sessionDateEpochDay = 20000L,
            notes = "Practised Compose Navigation and StateFlow"
        )
        assertTrue(result.isValid)
        assertNull(result.subjectError)
        assertNull(result.durationError)
        assertNull(result.dateError)
        assertNull(result.notesError)
    }

    @Test
    fun validateSubject_rejectsDuplicateAndInvalidGoal() {
        val duplicate = ValidationUtils.validateSubject(
            name = "Mobile Apps Dev",
            weeklyGoalHoursText = "5.0",
            isDuplicateName = true
        )
        assertFalse(duplicate.isValid)
        assertNotNull(duplicate.nameError)

        val badGoal = ValidationUtils.validateSubject(
            name = "Networking",
            weeklyGoalHoursText = "-2",
            isDuplicateName = false
        )
        assertFalse(badGoal.isValid)
        assertNotNull(badGoal.goalError)
    }

    @Test
    fun validateSubject_acceptsValidSubject() {
        val result = ValidationUtils.validateSubject(
            name = "Operating Systems",
            weeklyGoalHoursText = "4.5",
            isDuplicateName = false
        )
        assertTrue(result.isValid)
    }

    @Test
    fun validateSubject_rejectsNonFiniteNumbers() {
        val nanResult = ValidationUtils.validateSubject(
            name = "Networking",
            weeklyGoalHoursText = "NaN",
            isDuplicateName = false
        )
        assertFalse(nanResult.isValid)
        assertNotNull(nanResult.goalError)

        val infResult = ValidationUtils.validateSubject(
            name = "Networking",
            weeklyGoalHoursText = "Infinity",
            isDuplicateName = false
        )
        assertFalse(infResult.isValid)
        assertNotNull(infResult.goalError)
    }

    @Test
    fun validateStudySession_rejectsFutureDateAndAllowsToday() {
        val today = 20050L
        val futureResult = ValidationUtils.validateStudySession(
            selectedSubjectId = 1L,
            durationText = "45",
            sessionDateEpochDay = today + 1L,
            notes = "Planned tomorrow review",
            todayEpochDay = today
        )
        assertFalse(futureResult.isValid)
        assertEquals("Study date cannot be in the future", futureResult.dateError)

        val todayResult = ValidationUtils.validateStudySession(
            selectedSubjectId = 1L,
            durationText = "45",
            sessionDateEpochDay = today,
            notes = "Today review session",
            todayEpochDay = today
        )
        assertTrue(todayResult.isValid)
        assertNull(todayResult.dateError)
    }

    @Test
    fun validateStudySession_rejectsIntegerAndLongDurationOverflow() {
        val intOverflowResult = ValidationUtils.validateStudySession(
            selectedSubjectId = 1L,
            durationText = "99999999999",
            sessionDateEpochDay = 20050L,
            notes = "Overflow test",
            todayEpochDay = 20050L
        )
        assertFalse(intOverflowResult.isValid)
        assertEquals(
            "Duration cannot exceed 1440 minutes (24 hours)",
            intOverflowResult.durationError
        )

        val longOverflowResult = ValidationUtils.validateStudySession(
            selectedSubjectId = 1L,
            durationText = "99999999999999999999999",
            sessionDateEpochDay = 20050L,
            notes = "Long overflow test",
            todayEpochDay = 20050L
        )
        assertFalse(longOverflowResult.isValid)
        assertEquals(
            "Duration cannot exceed 1440 minutes (24 hours)",
            longOverflowResult.durationError
        )
    }

    @Test
    fun validateSubjectSchedule_rejectsShortNameZeroGoalExcessiveGoalAndEmptyCustomMask() {
        val shortName = ValidationUtils.validateSubjectSchedule(
            name = " A ",
            dailyHoursText = "0",
            dailyMinutesText = "30",
            isCustomMode = false,
            scheduledDaysMask = 127
        )
        assertFalse(shortName.isValid)
        assertNotNull(shortName.nameError)
        assertNull(shortName.goalError)

        val zeroGoal = ValidationUtils.validateSubjectSchedule(
            name = "Physics",
            dailyHoursText = "0",
            dailyMinutesText = "0",
            isCustomMode = false,
            scheduledDaysMask = 127
        )
        assertFalse(zeroGoal.isValid)
        assertNull(zeroGoal.nameError)
        assertNotNull(zeroGoal.goalError)

        val overMaxMinutes = ValidationUtils.validateSubjectSchedule(
            name = "Physics",
            dailyHoursText = "24",
            dailyMinutesText = "1",
            isCustomMode = false,
            scheduledDaysMask = 127
        )
        assertFalse(overMaxMinutes.isValid)
        assertNotNull(overMaxMinutes.goalError)

        val overMaxRawMinutes = ValidationUtils.validateSubjectSchedule(
            name = "Physics",
            dailyHoursText = "0",
            dailyMinutesText = "1441",
            isCustomMode = false,
            scheduledDaysMask = 127
        )
        assertFalse(overMaxRawMinutes.isValid)
        assertNotNull(overMaxRawMinutes.goalError)

        val emptyCustomMask = ValidationUtils.validateSubjectSchedule(
            name = "Physics",
            dailyHoursText = "1",
            dailyMinutesText = "0",
            isCustomMode = true,
            scheduledDaysMask = 0
        )
        assertFalse(emptyCustomMask.isValid)
        assertNull(emptyCustomMask.nameError)
        assertNull(emptyCustomMask.goalError)
        assertNotNull(emptyCustomMask.scheduleError)
    }

    @Test
    fun validateSubjectSchedule_acceptsValidSchedules() {
        val maxGoalEveryDay = ValidationUtils.validateSubjectSchedule(
            name = "Calculus",
            dailyHoursText = "24",
            dailyMinutesText = "0",
            isCustomMode = false,
            scheduledDaysMask = 0
        )
        assertTrue(maxGoalEveryDay.isValid)

        val customDays = ValidationUtils.validateSubjectSchedule(
            name = "Calculus",
            dailyHoursText = "1",
            dailyMinutesText = "30",
            isCustomMode = true,
            scheduledDaysMask = 0b0010101
        )
        assertTrue(customDays.isValid)
    }
}
