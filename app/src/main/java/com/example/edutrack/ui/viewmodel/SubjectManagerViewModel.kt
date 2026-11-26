package com.example.edutrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.edutrack.data.local.entity.SubjectEntity
import com.example.edutrack.data.repository.EduTrackRepository
import com.example.edutrack.ui.theme.SubjectTagPalette
import com.example.edutrack.util.ValidationUtils
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
 * Subject row with aggregated session metrics for Subject Manager.
 */
data class SubjectItemUi(
    val subject: SubjectEntity,
    val sessionCount: Int,
    val totalMinutesLogged: Int
)

/**
 * Form state inside the Add/Edit Subject dialog.
 */
data class SubjectFormState(
    val isVisible: Boolean = false,
    val editingSubjectId: Long? = null,
    val name: String = "",
    val weeklyGoalHoursText: String = "",
    val selectedColorHex: Long = SubjectTagPalette.first(),
    val nameError: String? = null,
    val goalError: String? = null
)

data class SubjectManagerUiState(
    val subjects: List<SubjectItemUi> = emptyList(),
    val formState: SubjectFormState = SubjectFormState(),
    val isLoading: Boolean = true
)

sealed interface SubjectManagerEvent {
    data class Feedback(val message: String) : SubjectManagerEvent
}

/**
 * ViewModel handling full CRUD and validation for [SubjectEntity].
 */
class SubjectManagerViewModel(
    private val repository: EduTrackRepository
) : ViewModel() {

    private val formState = MutableStateFlow(SubjectFormState())

    private val _events = Channel<SubjectManagerEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val uiState: StateFlow<SubjectManagerUiState> = combine(
        repository.observeSubjectsWithSessions(),
        formState
    ) { subjectsWithSessions, currentForm ->
        val items = subjectsWithSessions.map { item ->
            SubjectItemUi(
                subject = item.subject,
                sessionCount = item.sessions.size,
                totalMinutesLogged = item.sessions.sumOf { it.durationMinutes }
            )
        }
        SubjectManagerUiState(
            subjects = items,
            formState = currentForm,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SubjectManagerUiState()
    )

    fun openAddDialog() {
        formState.value = SubjectFormState(
            isVisible = true,
            selectedColorHex = SubjectTagPalette.first()
        )
    }

    fun openEditDialog(subject: SubjectEntity) {
        formState.value = SubjectFormState(
            isVisible = true,
            editingSubjectId = subject.id,
            name = subject.name,
            weeklyGoalHoursText = subject.weeklyGoalHours.toString().removeSuffix(".0"),
            selectedColorHex = subject.colorHex
        )
    }

    fun dismissDialog() {
        formState.value = SubjectFormState()
    }

    fun onNameChanged(name: String) {
        formState.update { it.copy(name = name, nameError = null) }
    }

    fun onWeeklyGoalChanged(goal: String) {
        formState.update { it.copy(weeklyGoalHoursText = goal, goalError = null) }
    }

    fun onColorSelected(colorHex: Long) {
        formState.update { it.copy(selectedColorHex = colorHex) }
    }

    fun saveSubject() {
        val current = formState.value
        val editingId = current.editingSubjectId ?: 0L

        viewModelScope.launch {
            val isDuplicate = if (current.name.trim().isNotEmpty()) {
                repository.isSubjectNameTaken(current.name, excludeId = editingId)
            } else {
                false
            }

            val validation = ValidationUtils.validateSubject(
                name = current.name,
                weeklyGoalHoursText = current.weeklyGoalHoursText,
                isDuplicateName = isDuplicate
            )

            if (!validation.isValid) {
                formState.update {
                    it.copy(
                        nameError = validation.nameError,
                        goalError = validation.goalError
                    )
                }
                return@launch
            }

            val cleanName = current.name.trim()
            val goalHours = current.weeklyGoalHoursText.trim().toDouble()

            if (editingId > 0L) {
                val existing = repository.getSubjectById(editingId)
                repository.updateSubject(
                    SubjectEntity(
                        id = editingId,
                        name = cleanName,
                        colorHex = current.selectedColorHex,
                        weeklyGoalHours = goalHours,
                        createdAt = existing?.createdAt ?: System.currentTimeMillis()
                    )
                )
                dismissDialog()
                _events.send(SubjectManagerEvent.Feedback("Subject \"$cleanName\" updated"))
            } else {
                repository.insertSubject(
                    SubjectEntity(
                        name = cleanName,
                        colorHex = current.selectedColorHex,
                        weeklyGoalHours = goalHours
                    )
                )
                dismissDialog()
                _events.send(SubjectManagerEvent.Feedback("Subject \"$cleanName\" added"))
            }
        }
    }

    fun deleteSubject(subject: SubjectEntity) {
        viewModelScope.launch {
            repository.deleteSubject(subject)
            _events.send(SubjectManagerEvent.Feedback("Deleted \"${subject.name}\""))
        }
    }
}
