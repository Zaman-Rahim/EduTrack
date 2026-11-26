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
        val goalError: String? = null
    ) {
        val isValid: Boolean
            get() = nameError == null && goalError == null
    }

    fun validateStudySession(
        selectedSubjectId: Long?,
        durationText: String,
        sessionDateEpochDay: Long?,
        notes: String
    ): SessionValidationResult {
        val subjectError = if (selectedSubjectId == null || selectedSubjectId <= 0L) {
            "Please select a subject"
        } else {
            null
        }

        val trimmedDuration = durationText.trim()
        val parsedDuration = trimmedDuration.toIntOrNull()
        val durationError = when {
            trimmedDuration.isEmpty() -> "Duration is required"
            parsedDuration == null -> "Enter a valid whole number in minutes"
            parsedDuration <= 0 -> "Duration must be greater than 0 minutes"
            parsedDuration > 1440 -> "Duration cannot exceed 1440 minutes (24 hours)"
            else -> null
        }

        val dateError = if (sessionDateEpochDay == null) {
            "Please choose a study date"
        } else {
            null
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
        val trimmedName = name.trim()
        val nameError = when {
            trimmedName.isEmpty() -> "Subject name is required"
            trimmedName.length < 2 -> "Subject name must be at least 2 characters"
            isDuplicateName -> "A subject with this name already exists"
            else -> null
        }

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
}
