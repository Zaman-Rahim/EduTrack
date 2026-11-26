package com.example.edutrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.edutrack.data.local.entity.StudySessionEntity
import com.example.edutrack.data.local.entity.SubjectEntity
import com.example.edutrack.data.repository.EduTrackRepository
import com.example.edutrack.util.DateTimeUtils
import com.example.edutrack.util.ValidationUtils
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for adding or editing a study session.
 */
data class AddEditSessionUiState(
    val isEditMode: Boolean = false,
    val subjects: List<SubjectEntity> = emptyList(),
    val selectedSubjectId: Long? = null,
    val durationText: String = "",
    val sessionDateEpochDay: Long = DateTimeUtils.todayEpochDay(),
    val notes: String = "",
    val subjectError: String? = null,
    val durationError: String? = null,
    val dateError: String? = null,
    val notesError: String? = null,
    val isSaving: Boolean = false
)

/**
 * One-shot navigation/feedback events emitted by [AddEditSessionViewModel].
 */
sealed interface AddEditSessionEvent {
    data class SavedSuccess(val message: String) : AddEditSessionEvent
    data class ErrorMessage(val message: String) : AddEditSessionEvent
}

/**
 * ViewModel handling form state, input validation, and Room persistence for study sessions.
 */
class AddEditSessionViewModel(
    private val repository: EduTrackRepository,
    private val sessionId: Long,
    private val initialSubjectId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AddEditSessionUiState(
            isEditMode = sessionId > 0L,
            selectedSubjectId = initialSubjectId.takeIf { it > 0L }
        )
    )
    val uiState: StateFlow<AddEditSessionUiState> = _uiState.asStateFlow()

    private val _events = Channel<AddEditSessionEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var existingCreatedAt: Long? = null

    init {
        viewModelScope.launch {
            repository.observeAllSubjects().collect { subjects ->
                _uiState.update { state ->
                    val resolvedSubjectId = when {
                        state.selectedSubjectId != null &&
                            subjects.any { it.id == state.selectedSubjectId } -> state.selectedSubjectId
                        !state.isEditMode && subjects.size == 1 -> subjects.first().id
                        else -> state.selectedSubjectId
                    }
                    state.copy(
                        subjects = subjects,
                        selectedSubjectId = resolvedSubjectId
                    )
                }
            }
        }

        if (sessionId > 0L) {
            viewModelScope.launch {
                val existing = repository.getSessionById(sessionId)
                if (existing != null) {
                    existingCreatedAt = existing.createdAt
                    _uiState.update {
                        it.copy(
                            isEditMode = true,
                            selectedSubjectId = existing.subjectId,
                            durationText = existing.durationMinutes.toString(),
                            sessionDateEpochDay = existing.sessionDateEpochDay,
                            notes = existing.notes
                        )
                    }
                } else {
                    _events.send(AddEditSessionEvent.ErrorMessage("Study session not found."))
                }
            }
        }
    }

    fun onSubjectSelected(subjectId: Long) {
        _uiState.update { it.copy(selectedSubjectId = subjectId, subjectError = null) }
    }

    fun onDurationChanged(value: String) {
        _uiState.update { it.copy(durationText = value, durationError = null) }
    }

    fun onDateSelected(epochDay: Long) {
        _uiState.update { it.copy(sessionDateEpochDay = epochDay, dateError = null) }
    }

    fun onNotesChanged(value: String) {
        _uiState.update { it.copy(notes = value, notesError = null) }
    }

    fun saveSession() {
        val current = _uiState.value
        val validation = ValidationUtils.validateStudySession(
            selectedSubjectId = current.selectedSubjectId,
            durationText = current.durationText,
            sessionDateEpochDay = current.sessionDateEpochDay,
            notes = current.notes
        )

        if (!validation.isValid) {
            _uiState.update {
                it.copy(
                    subjectError = validation.subjectError,
                    durationError = validation.durationError,
                    dateError = validation.dateError,
                    notesError = validation.notesError
                )
            }
            return
        }

        val subjectId = current.selectedSubjectId ?: return
        val duration = current.durationText.trim().toInt()
        val notes = current.notes.trim()

        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            try {
                if (sessionId > 0L) {
                    repository.updateSession(
                        StudySessionEntity(
                            id = sessionId,
                            subjectId = subjectId,
                            durationMinutes = duration,
                            sessionDateEpochDay = current.sessionDateEpochDay,
                            notes = notes,
                            createdAt = existingCreatedAt ?: System.currentTimeMillis()
                        )
                    )
                    _events.send(AddEditSessionEvent.SavedSuccess("Study session updated"))
                } else {
                    repository.insertSession(
                        StudySessionEntity(
                            subjectId = subjectId,
                            durationMinutes = duration,
                            sessionDateEpochDay = current.sessionDateEpochDay,
                            notes = notes
                        )
                    )
                    _events.send(AddEditSessionEvent.SavedSuccess("Study session logged"))
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false) }
                _events.send(
                    AddEditSessionEvent.ErrorMessage(
                        e.localizedMessage ?: "Failed to save study session"
                    )
                )
            }
        }
    }
}
