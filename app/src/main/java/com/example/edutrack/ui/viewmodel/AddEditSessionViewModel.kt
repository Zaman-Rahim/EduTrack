package com.example.edutrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.edutrack.data.local.entity.StudySessionEntity
import com.example.edutrack.data.local.entity.SubjectEntity
import com.example.edutrack.data.repository.EduTrackRepository
import com.example.edutrack.util.DateTimeUtils
import com.example.edutrack.util.ValidationUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
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
    val notFoundError: String? = null,
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

    private val draftState = MutableStateFlow(
        AddEditSessionUiState(
            isEditMode = sessionId > 0L,
            selectedSubjectId = initialSubjectId.takeIf { it > 0L }
        )
    )

    val uiState: StateFlow<AddEditSessionUiState> = combine(
        repository.observeAllSubjects(),
        draftState
    ) { subjects, state ->
        val resolvedSubjectId = when {
            state.selectedSubjectId != null &&
                subjects.any { it.id == state.selectedSubjectId } -> state.selectedSubjectId
            !state.isEditMode && subjects.size == 1 -> subjects.first().id
            else -> null
        }
        state.copy(
            subjects = subjects,
            selectedSubjectId = resolvedSubjectId
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = draftState.value
    )

    private val _events = Channel<AddEditSessionEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var existingCreatedAt: Long? = null

    init {
        if (sessionId > 0L) {
            viewModelScope.launch {
                try {
                    val existing = repository.getSessionById(sessionId)
                    if (existing != null) {
                        existingCreatedAt = existing.createdAt
                        draftState.update {
                            it.copy(
                                isEditMode = true,
                                selectedSubjectId = existing.subjectId,
                                durationText = existing.durationMinutes.toString(),
                                sessionDateEpochDay = existing.sessionDateEpochDay,
                                notes = existing.notes,
                                notFoundError = null
                            )
                        }
                    } else {
                        draftState.update {
                            it.copy(notFoundError = "This study session no longer exists.")
                        }
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    draftState.update {
                        it.copy(notFoundError = "Unable to load study session details.")
                    }
                }
            }
        }
    }

    fun onSubjectSelected(subjectId: Long) {
        draftState.update { it.copy(selectedSubjectId = subjectId, subjectError = null) }
    }

    fun onDurationChanged(value: String) {
        val digitsOnly = value.filter { it.isDigit() }.take(5)
        draftState.update { it.copy(durationText = digitsOnly, durationError = null) }
    }

    fun onDateSelected(epochDay: Long) {
        draftState.update { it.copy(sessionDateEpochDay = epochDay, dateError = null) }
    }

    fun onNotesChanged(value: String) {
        draftState.update { it.copy(notes = value, notesError = null) }
    }

    fun saveSession() {
        val current = uiState.value
        if (current.isSaving || current.notFoundError != null) return

        val validation = ValidationUtils.validateStudySession(
            selectedSubjectId = current.selectedSubjectId,
            durationText = current.durationText,
            sessionDateEpochDay = current.sessionDateEpochDay,
            notes = current.notes,
            todayEpochDay = DateTimeUtils.todayEpochDay()
        )

        if (!validation.isValid) {
            draftState.update {
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
        val duration = current.durationText.trim().toIntOrNull() ?: return
        val notes = current.notes.trim()

        draftState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            try {
                if (sessionId > 0L) {
                    val existing = repository.getSessionById(sessionId)
                    if (existing == null) {
                        draftState.update {
                            it.copy(
                                isSaving = false,
                                notFoundError = "This study session was deleted."
                            )
                        }
                        return@launch
                    }
                    repository.updateSession(
                        StudySessionEntity(
                            id = sessionId,
                            subjectId = subjectId,
                            durationMinutes = duration,
                            sessionDateEpochDay = current.sessionDateEpochDay,
                            notes = notes,
                            createdAt = existingCreatedAt ?: existing.createdAt
                        )
                    )
                    _events.send(AddEditSessionEvent.SavedSuccess("Study session updated"))
                } else {
                    if (repository.getSubjectById(subjectId) == null) {
                        draftState.update {
                            it.copy(
                                isSaving = false,
                                subjectError = "Selected subject no longer exists"
                            )
                        }
                        return@launch
                    }
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
                if (e is CancellationException) throw e
                draftState.update { it.copy(isSaving = false) }
                _events.send(
                    AddEditSessionEvent.ErrorMessage(
                        e.localizedMessage ?: "Failed to save study session"
                    )
                )
            }
        }
    }
}
