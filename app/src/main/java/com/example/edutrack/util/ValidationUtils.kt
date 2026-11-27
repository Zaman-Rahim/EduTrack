package com.example.edutrack.util

/**
 * Pure input-validation helpers shared across ViewModels and unit tests.
 */
object ValidationUtils {

    data class SessionValidationResult(
        val subjectError: String? = null,
        val durationError: String? = null,
        val dateError: String? = null,
        val notesError: String? = null
    ) {
        val isValid: Boolean
            get() = subjectError == null &&
                durationError == null &&
                dateError == null &&
                notesError == null
    }

    data class SubjectValidationResult(
        val nameError: String? = null,
        val goalError: String? = null,
        val scheduleError: String? = null
    ) {
        val isValid: Boolean
            get() = nameError == null && goalError == null && scheduleError == null
    }

    fun validateStudySession(
        selectedSubjectId: Long?,
        durationText: String,
        sessionDateEpochDay: Long?,
        notes: String,
        todayEpochDay: Long = DateTimeUtils.todayEpochDay()
    ): SessionValidationResult {
        val subjectError = if (selectedSubjectId == null || selectedSubjectId <= 0L) {
            "Please select a subject"
        } else {
            null
        }

        val trimmedDuration = durationText.trim()
        val parsedDuration = trimmedDuration.toLongOrNull()
        val isDigitOverflow = parsedDuration == null &&
            trimmedDuration.isNotEmpty() &&
            trimmedDuration.all { it.isDigit() }
        val durationError = when {
            trimmedDuration.isEmpty() -> "Duration is required"
            isDigitOverflow -> "Duration cannot exceed 1440 minutes (24 hours)"
            parsedDuration == null -> "Enter a valid whole number in minutes"
            parsedDuration <= 0L -> "Duration must be greater than 0 minutes"
            parsedDuration > 1440L -> "Duration cannot exceed 1440 minutes (24 hours)"
            else -> null
        }

        val dateError = when {
            sessionDateEpochDay == null -> "Please choose a study date"
            sessionDateEpochDay > todayEpochDay -> "Study date cannot be in the future"
            else -> null
        }

        val notesError = when {
            notes.trim().isEmpty() -> "Please enter brief study notes"
            notes.trim().length < 3 -> "Notes must be at least 3 characters"
            else -> null
        }

        return SessionValidationResult(
            subjectError = subjectError,
            durationError = durationError,
            dateError = dateError,
            notesError = notesError
        )
    }

    fun validateSubject(
        name: String,
        weeklyGoalHoursText: String,
        isDuplicateName: Boolean = false
    ): SubjectValidationResult {
        val nameError = validateSubjectName(name, isDuplicateName)

        val trimmedGoal = weeklyGoalHoursText.trim()
        val parsedGoal = trimmedGoal.toDoubleOrNull()
        val goalError = when {
            trimmedGoal.isEmpty() -> "Weekly goal hours is required"
            parsedGoal == null || !parsedGoal.isFinite() -> "Enter a valid number (e.g., 5 or 4.5)"
            parsedGoal <= 0.0 -> "Weekly goal must be greater than 0 hours"
            parsedGoal > 168.0 -> "Weekly goal cannot exceed 168 hours"
            else -> null
        }

        return SubjectValidationResult(
            nameError = nameError,
            goalError = goalError
        )
    }

    fun validateSubjectSchedule(
        name: String,
        dailyHoursText: String,
        dailyMinutesText: String,
        isCustomMode: Boolean,
        scheduledDaysMask: Int,
        isDuplicateName: Boolean = false
    ): SubjectValidationResult {
        val nameError = validateSubjectName(name, isDuplicateName)

        val trimmedHours = dailyHoursText.trim()
        val trimmedMinutes = dailyMinutesText.trim()
        val hours = if (trimmedHours.isEmpty()) 0L else trimmedHours.toLongOrNull()
        val minutes = if (trimmedMinutes.isEmpty()) 0L else trimmedMinutes.toLongOrNull()

        val hasDigitOverflow =
            (trimmedHours.isNotEmpty() && hours == null && trimmedHours.all { it.isDigit() }) ||
                (trimmedMinutes.isNotEmpty() && minutes == null && trimmedMinutes.all { it.isDigit() })

        val goalError = when {
            hasDigitOverflow -> "Daily study goal must be between 1 minute and 24 hours"
            hours == null || minutes == null -> "Enter valid whole numbers for hours and minutes"
            hours < 0L || minutes < 0L || hours > 24L || minutes > 1440L ->
                "Daily study goal must be between 1 minute and 24 hours"
            (hours * 60L + minutes) !in 1L..1440L ->
                "Daily study goal must be between 1 minute and 24 hours"
            else -> null
        }

        val scheduleError = if (isCustomMode && (scheduledDaysMask and 0b1111111) == 0) {
            "Select at least one day of the week"
        } else {
            null
        }

        return SubjectValidationResult(
            nameError = nameError,
            goalError = goalError,
            scheduleError = scheduleError
        )
    }

    private fun validateSubjectName(name: String, isDuplicateName: Boolean): String? {
        val trimmedName = name.trim()
        return when {
            trimmedName.isEmpty() -> "Subject name is required"
            trimmedName.length < 2 -> "Subject name must be at least 2 characters"
            isDuplicateName -> "A subject with this name already exists"
            else -> null
        }
    }
}
