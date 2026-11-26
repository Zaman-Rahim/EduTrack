package com.example.edutrack.util

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
}
