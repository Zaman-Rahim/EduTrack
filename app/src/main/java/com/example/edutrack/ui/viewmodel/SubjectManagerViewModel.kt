package com.example.edutrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.edutrack.data.local.entity.SubjectEntity
import com.example.edutrack.data.local.entity.SubjectRepeatMode
import com.example.edutrack.data.repository.EduTrackRepository
import com.example.edutrack.ui.theme.SubjectTagPalette
import com.example.edutrack.util.DateTimeUtils
import com.example.edutrack.util.ValidationUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Subject row with daily To-Do progress metrics.
 */
data class SubjectItemUi(
    val subject: SubjectEntity,
    val todayMinutesLogged: Int,
    val isScheduledToday: Boolean,
    val isCompletedToday: Boolean
)

/**
 * Form state for Add/Edit Subject & Schedule screen.
 */
data class SubjectFormState(
    val editingSubjectId: Long? = null,
    val name: String = "",
    val dailyHoursText: String = "0",
    val dailyMinutesText: String = "25",
    val startDateEpochDay: Long = DateTimeUtils.todayEpochDay(),
    val repeatMode: SubjectRepeatMode = SubjectRepeatMode.EVERY_DAY,
    val selectedDaysMask: Int = 127,
    val selectedColorHex: Long = SubjectTagPalette.first(),
    val nameError: String? = null,
    val goalError: String? = null,
    val scheduleError: String? = null,
    val isSaving: Boolean = false,
    val isLoading: Boolean = false,
    val notFoundError: String? = null
) {
    val dailyTotalMinutes: Int
        get() {
            val h = (dailyHoursText.toIntOrNull() ?: 0).coerceAtLeast(0)
            val m = (dailyMinutesText.toIntOrNull() ?: 0).coerceAtLeast(0)
            return h * 60 + m
        }

    val activeDaysCount: Int
        get() = when (repeatMode) {
            SubjectRepeatMode.ONCE -> 1
            SubjectRepeatMode.EVERY_DAY -> 7
            SubjectRepeatMode.CUSTOM -> Integer.bitCount(selectedDaysMask and 127)
        }

    val calculatedWeeklyMinutes: Int
        get() = dailyTotalMinutes * activeDaysCount.coerceAtLeast(1)
}

data class SubjectManagerUiState(
    val readyToday: List<SubjectItemUi> = emptyList(),
    val completedToday: List<SubjectItemUi> = emptyList(),
    val offToday: List<SubjectItemUi> = emptyList(),
    val allSubjects: List<SubjectItemUi> = emptyList(),
    val formState: SubjectFormState = SubjectFormState(),
    val isLoading: Boolean = true
)

sealed interface SubjectManagerEvent {
    data class Feedback(val message: String) : SubjectManagerEvent
    data object SavedSuccess : SubjectManagerEvent
    data class DeletedSuccess(val message: String) : SubjectManagerEvent
    data class ErrorMessage(val message: String) : SubjectManagerEvent
}

/**
 * ViewModel handling Subject To-Do list, daily goal schedules, repeat weekdays, and Room CRUD.
 */
class SubjectManagerViewModel(
    private val repository: EduTrackRepository
) : ViewModel() {

    private val formState = MutableStateFlow(SubjectFormState())

    private val _events = Channel<SubjectManagerEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var loadJob: Job? = null

    val uiState: StateFlow<SubjectManagerUiState> = combine(
        repository.observeSubjectsWithSessions(),
        formState
    ) { subjectsWithSessions, currentForm ->
        val today = LocalDate.now()
        val todayEpoch = today.toEpochDay()

        val items = subjectsWithSessions.map { item ->
            val todayMinutes = item.sessions
                .filter { it.sessionDateEpochDay == todayEpoch }
                .sumOf { it.durationMinutes }
            val scheduled = item.subject.isScheduledOn(today)
            val completed = scheduled && todayMinutes >= item.subject.dailyGoalMinutes
            SubjectItemUi(
                subject = item.subject,
                todayMinutesLogged = todayMinutes,
                isScheduledToday = scheduled,
                isCompletedToday = completed
            )
        }

        SubjectManagerUiState(
            readyToday = items.filter { it.isScheduledToday && !it.isCompletedToday },
            completedToday = items.filter { it.isCompletedToday },
            offToday = items.filter { !it.isScheduledToday },
            allSubjects = items,
            formState = currentForm,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SubjectManagerUiState()
    )

    private fun prepareNewSubject() {
        formState.value = SubjectFormState(
            selectedColorHex = SubjectTagPalette.first(),
            startDateEpochDay = DateTimeUtils.todayEpochDay()
        )
    }

    fun loadSubjectForEdit(subjectId: Long) {
        loadJob?.cancel()
        if (subjectId <= 0L) {
            prepareNewSubject()
            return
        }
        formState.value = SubjectFormState(
            editingSubjectId = subjectId,
            isLoading = true
        )
        loadJob = viewModelScope.launch {
            try {
                val subject = repository.getSubjectById(subjectId)
                if (subject != null) {
                    val hours = subject.dailyGoalMinutes / 60
                    val mins = subject.dailyGoalMinutes % 60
                    val mode = runCatching { SubjectRepeatMode.valueOf(subject.repeatMode) }
                        .getOrDefault(SubjectRepeatMode.EVERY_DAY)
                    formState.value = SubjectFormState(
                        editingSubjectId = subject.id,
                        name = subject.name,
                        dailyHoursText = hours.toString(),
                        dailyMinutesText = mins.toString(),
                        startDateEpochDay = subject.startDateEpochDay,
                        repeatMode = mode,
                        selectedDaysMask = subject.scheduledDaysMask,
                        selectedColorHex = subject.colorHex,
                        isLoading = false
                    )
                } else {
                    formState.value = SubjectFormState(
                        editingSubjectId = subjectId,
                        isLoading = false,
                        notFoundError = "This subject no longer exists."
                    )
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                formState.value = SubjectFormState(
                    editingSubjectId = subjectId,
                    isLoading = false,
                    notFoundError = "Unable to load subject details."
                )
            }
        }
    }

    fun onNameChanged(name: String) {
        formState.update { it.copy(name = name, nameError = null) }
    }

    fun onDailyHoursChanged(hoursText: String) {
        val filtered = hoursText.filter { it.isDigit() }.take(2)
        formState.update { it.copy(dailyHoursText = filtered, goalError = null) }
    }

    fun onDailyMinutesChanged(minutesText: String) {
        val filtered = minutesText.filter { it.isDigit() }.take(2)
        formState.update { it.copy(dailyMinutesText = filtered, goalError = null) }
    }

    fun onStartDateSelected(epochDay: Long) {
        formState.update { it.copy(startDateEpochDay = epochDay) }
    }

    fun onRepeatModeSelected(mode: SubjectRepeatMode) {
        formState.update { current ->
            val updatedMask = when (mode) {
                SubjectRepeatMode.EVERY_DAY -> 127
                SubjectRepeatMode.CUSTOM -> if (current.selectedDaysMask == 0) 0b0010101 else current.selectedDaysMask
                SubjectRepeatMode.ONCE -> current.selectedDaysMask
            }
            current.copy(repeatMode = mode, selectedDaysMask = updatedMask, scheduleError = null)
        }
    }

    fun onToggleDayBit(dayIndexZeroToSix: Int) {
        formState.update { current ->
            val bit = 1 shl dayIndexZeroToSix
            val toggled = current.selectedDaysMask xor bit
            current.copy(
                repeatMode = SubjectRepeatMode.CUSTOM,
                selectedDaysMask = toggled,
                scheduleError = null
            )
        }
    }

    fun onColorSelected(colorHex: Long) {
        formState.update { it.copy(selectedColorHex = colorHex) }
    }

    fun saveSubject() {
        val current = formState.value
        if (current.isSaving || current.isLoading || current.notFoundError != null) return
        val editingId = current.editingSubjectId ?: 0L

        formState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            try {
                val cleanName = current.name.trim()
                val isDuplicate = cleanName.isNotEmpty() &&
                    repository.isSubjectNameTaken(cleanName, excludeId = editingId)

                val validation = ValidationUtils.validateSubjectSchedule(
                    name = current.name,
                    dailyHoursText = current.dailyHoursText,
                    dailyMinutesText = current.dailyMinutesText,
                    isCustomMode = current.repeatMode == SubjectRepeatMode.CUSTOM,
                    scheduledDaysMask = current.selectedDaysMask,
                    isDuplicateName = isDuplicate
                )
                if (!validation.isValid) {
                    formState.update {
                        it.copy(
                            isSaving = false,
                            nameError = validation.nameError,
                            goalError = validation.goalError,
                            scheduleError = validation.scheduleError
                        )
                    }
                    return@launch
                }

                val dailyMinutes = current.dailyTotalMinutes
                val weeklyHours = current.calculatedWeeklyMinutes / 60.0
                val maskToSave = when (current.repeatMode) {
                    SubjectRepeatMode.EVERY_DAY -> 127
                    SubjectRepeatMode.ONCE -> current.selectedDaysMask
                    SubjectRepeatMode.CUSTOM -> current.selectedDaysMask and 127
                }

                if (editingId > 0L) {
                    val existing = repository.getSubjectById(editingId)
                    if (existing == null) {
                        formState.update {
                            it.copy(
                                isSaving = false,
                                notFoundError = "This subject was deleted."
                            )
                        }
                        return@launch
                    }
                    repository.updateSubject(
                        SubjectEntity(
                            id = editingId,
                            name = cleanName,
                            colorHex = current.selectedColorHex,
                            weeklyGoalHours = weeklyHours,
                            dailyGoalMinutes = dailyMinutes,
                            repeatMode = current.repeatMode.name,
                            scheduledDaysMask = maskToSave,
                            startDateEpochDay = current.startDateEpochDay,
                            createdAt = existing.createdAt
                        )
                    )
                } else {
                    repository.insertSubject(
                        SubjectEntity(
                            name = cleanName,
                            colorHex = current.selectedColorHex,
                            weeklyGoalHours = weeklyHours,
                            dailyGoalMinutes = dailyMinutes,
                            repeatMode = current.repeatMode.name,
                            scheduledDaysMask = maskToSave,
                            startDateEpochDay = current.startDateEpochDay
                        )
                    )
                }
                formState.update { it.copy(isSaving = false) }
                _events.send(SubjectManagerEvent.SavedSuccess)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                formState.update { it.copy(isSaving = false) }
                _events.send(
                    SubjectManagerEvent.ErrorMessage(
                        e.localizedMessage ?: "Failed to save subject"
                    )
                )
            }
        }
    }

    fun deleteSubject(subject: SubjectEntity) {
        if (formState.value.isSaving) return
        formState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                repository.deleteSubject(subject)
                formState.update { it.copy(isSaving = false) }
                _events.send(SubjectManagerEvent.DeletedSuccess("Deleted \"${subject.name}\""))
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                formState.update { it.copy(isSaving = false) }
                _events.send(
                    SubjectManagerEvent.ErrorMessage(
                        e.localizedMessage ?: "Failed to delete subject"
                    )
                )
            }
        }
    }
}
