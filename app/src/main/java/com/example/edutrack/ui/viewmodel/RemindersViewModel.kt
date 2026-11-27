package com.example.edutrack.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.edutrack.data.local.entity.NotificationEntity
import com.example.edutrack.data.local.entity.NotificationType
import com.example.edutrack.data.local.entity.ReminderEntity
import com.example.edutrack.data.local.entity.ReminderPriority
import com.example.edutrack.data.local.entity.ReminderRepeatType
import com.example.edutrack.data.repository.EduTrackRepository
import com.example.edutrack.notifications.ReminderAlarmScheduler
import com.example.edutrack.util.DateTimeUtils
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
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale

enum class ReminderTabFilter(val label: String) {
    TODAY("Today"),
    UPCOMING("Upcoming"),
    COMPLETED("Completed")
}

data class ReminderEditorState(
    val editingId: Long? = null,
    val title: String = "",
    val notes: String = "",
    val dueDateEpochDay: Long = DateTimeUtils.todayEpochDay(),
    val dueTimeMinutes: Int = 9 * 60,
    val repeatType: ReminderRepeatType = ReminderRepeatType.NONE,
    val repeatDaysMask: Int = 0b0010101,
    val priority: ReminderPriority = ReminderPriority.NORMAL,
    val notifyEnabled: Boolean = true,
    val titleError: String? = null,
    val pastTimeError: String? = null,
    val isSaving: Boolean = false,
    val isLoading: Boolean = false,
    val notFoundError: String? = null
)

data class RemindersUiState(
    val selectedTab: ReminderTabFilter = ReminderTabFilter.TODAY,
    val visibleReminders: List<ReminderEntity> = emptyList(),
    val completedTodayCount: Int = 0,
    val editorState: ReminderEditorState = ReminderEditorState(),
    val isLoading: Boolean = true
)

sealed interface RemindersEvent {
    data class Feedback(val message: String) : RemindersEvent
    data object SavedSuccess : RemindersEvent
    data object DeletedSuccess : RemindersEvent
}

class RemindersViewModel(
    private val repository: EduTrackRepository,
    private val appContext: Context? = null
) : ViewModel() {

    private val selectedTab = MutableStateFlow(ReminderTabFilter.TODAY)
    private val editorState = MutableStateFlow(ReminderEditorState())

    private val _listEvents = Channel<RemindersEvent>(Channel.BUFFERED)
    val listEvents = _listEvents.receiveAsFlow()

    private val _editorEvents = Channel<RemindersEvent>(Channel.BUFFERED)
    val editorEvents = _editorEvents.receiveAsFlow()

    private val _detailEvents = Channel<RemindersEvent>(Channel.BUFFERED)
    val detailEvents = _detailEvents.receiveAsFlow()

    private var loadJob: Job? = null

    val uiState: StateFlow<RemindersUiState> = combine(
        repository.observeAllReminders(),
        selectedTab,
        editorState
    ) { allReminders, tab, editor ->
        val todayEpoch = DateTimeUtils.todayEpochDay()
        val todayList = allReminders.filter { !it.isCompleted && it.dueDateEpochDay <= todayEpoch }
        val upcomingList = allReminders.filter { !it.isCompleted && it.dueDateEpochDay > todayEpoch }
        val completedList = allReminders.filter { it.isCompleted }
        val zone = ZoneId.systemDefault()
        val completedTodayCount = completedList.count { item ->
            val completedMillis = item.completedAt ?: return@count false
            Instant.ofEpochMilli(completedMillis).atZone(zone).toLocalDate().toEpochDay() == todayEpoch
        }

        val visible = when (tab) {
            ReminderTabFilter.TODAY -> todayList
            ReminderTabFilter.UPCOMING -> upcomingList
            ReminderTabFilter.COMPLETED -> completedList
        }

        RemindersUiState(
            selectedTab = tab,
            visibleReminders = visible,
            completedTodayCount = completedTodayCount,
            editorState = editor,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RemindersUiState()
    )

    fun observeReminder(reminderId: Long) = repository.observeReminderById(reminderId)

    fun selectTab(tab: ReminderTabFilter) {
        selectedTab.value = tab
    }

    private fun prepareNewReminder() {
        val now = LocalDateTime.now()
        val nextHour = now.plusHours(1).withMinute(0).withSecond(0).withNano(0)
        editorState.value = ReminderEditorState(
            dueDateEpochDay = nextHour.toLocalDate().toEpochDay(),
            dueTimeMinutes = nextHour.hour * 60 + nextHour.minute
        )
    }

    fun loadReminderForEdit(reminderId: Long) {
        loadJob?.cancel()
        if (reminderId <= 0L) {
            prepareNewReminder()
            return
        }
        editorState.value = ReminderEditorState(
            editingId = reminderId,
            isLoading = true
        )
        loadJob = viewModelScope.launch {
            try {
                val reminder = repository.getReminderById(reminderId)
                if (reminder != null) {
                    editorState.value = ReminderEditorState(
                        editingId = reminder.id,
                        title = reminder.title,
                        notes = reminder.notes,
                        dueDateEpochDay = reminder.dueDateEpochDay,
                        dueTimeMinutes = reminder.dueTimeMinutes,
                        repeatType = runCatching { ReminderRepeatType.valueOf(reminder.repeatType) }
                            .getOrDefault(ReminderRepeatType.NONE),
                        repeatDaysMask = reminder.repeatDaysMask,
                        priority = runCatching { ReminderPriority.valueOf(reminder.priority) }
                            .getOrDefault(ReminderPriority.NORMAL),
                        notifyEnabled = reminder.notifyEnabled,
                        isLoading = false
                    )
                } else {
                    editorState.value = ReminderEditorState(
                        editingId = reminderId,
                        isLoading = false,
                        notFoundError = "This reminder no longer exists."
                    )
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                editorState.value = ReminderEditorState(
                    editingId = reminderId,
                    isLoading = false,
                    notFoundError = "Unable to load reminder details."
                )
            }
        }
    }

    fun onTitleChanged(title: String) = editorState.update { it.copy(title = title, titleError = null) }
    fun onNotesChanged(notes: String) = editorState.update { it.copy(notes = notes) }
    fun onDueDateSelected(epochDay: Long) = editorState.update { it.copy(dueDateEpochDay = epochDay, pastTimeError = null) }
    fun onDueTimeSelected(minutesFromMidnight: Int) =
        editorState.update { it.copy(dueTimeMinutes = minutesFromMidnight.coerceIn(0, 1439), pastTimeError = null) }
    fun dismissPastTimeError() = editorState.update { it.copy(pastTimeError = null) }

    fun onRepeatTypeSelected(repeatType: ReminderRepeatType) = editorState.update { current ->
        val mask = if (repeatType == ReminderRepeatType.CUSTOM && (current.repeatDaysMask and 127) == 0) {
            0b0010101
        } else {
            current.repeatDaysMask
        }
        current.copy(repeatType = repeatType, repeatDaysMask = mask)
    }

    fun onRepeatDaysMaskChanged(mask: Int) = editorState.update {
        val cleanMask = mask and 127
        it.copy(
            repeatType = if (cleanMask == 0) ReminderRepeatType.NONE else ReminderRepeatType.CUSTOM,
            repeatDaysMask = cleanMask
        )
    }

    fun onToggleRepeatDayBit(dayIndexZeroToSix: Int) = editorState.update { current ->
        val nextMask = (current.repeatDaysMask xor (1 shl dayIndexZeroToSix)) and 127
        current.copy(repeatDaysMask = nextMask)
    }

    fun onPrioritySelected(priority: ReminderPriority) = editorState.update { it.copy(priority = priority) }
    fun onNotifyToggle(enabled: Boolean) = editorState.update { it.copy(notifyEnabled = enabled) }

    fun saveReminder() {
        val current = editorState.value
        if (current.isSaving || current.isLoading || current.notFoundError != null) return

        val cleanTitle = current.title.trim()
        if (cleanTitle.isEmpty()) {
            editorState.update { it.copy(titleError = "Task title is required") }
            return
        }

        val safeTimeMinutes = current.dueTimeMinutes.coerceIn(0, 1439)
        val selectedDateTime = LocalDateTime.of(
            LocalDate.ofEpochDay(current.dueDateEpochDay),
            LocalTime.of(safeTimeMinutes / 60, safeTimeMinutes % 60)
        )
        if (!selectedDateTime.isAfter(LocalDateTime.now())) {
            val formattedTime = formatTimeOfDay(safeTimeMinutes)
            val formattedDate = if (current.dueDateEpochDay == DateTimeUtils.todayEpochDay()) {
                "Today"
            } else {
                DateTimeUtils.formatShortDate(current.dueDateEpochDay)
            }
            editorState.update {
                it.copy(
                    pastTimeError = "The selected time ($formattedDate · $formattedTime) has already passed. Please choose an upcoming time so your reminder can alert you."
                )
            }
            return
        }

        val resolvedRepeatType = if (
            current.repeatType == ReminderRepeatType.CUSTOM &&
            (current.repeatDaysMask and 127) == 0
        ) {
            ReminderRepeatType.NONE
        } else {
            current.repeatType
        }
        val cleanMask = current.repeatDaysMask and 127

        editorState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            try {
                val editingId = current.editingId ?: 0L
                if (editingId > 0L) {
                    val existing = repository.getReminderById(editingId)
                    if (existing == null) {
                        editorState.update {
                            it.copy(
                                isSaving = false,
                                notFoundError = "This reminder was deleted."
                            )
                        }
                        return@launch
                    }
                    val updatedReminder = ReminderEntity(
                        id = editingId,
                        title = cleanTitle,
                        notes = current.notes.trim(),
                        dueDateEpochDay = current.dueDateEpochDay,
                        dueTimeMinutes = safeTimeMinutes,
                        repeatType = resolvedRepeatType.name,
                        repeatDaysMask = cleanMask,
                        priority = current.priority.name,
                        notifyEnabled = current.notifyEnabled,
                        isCompleted = existing.isCompleted,
                        completedAt = existing.completedAt,
                        createdAt = existing.createdAt
                    )
                    repository.updateReminder(updatedReminder)
                    appContext?.let { ReminderAlarmScheduler.scheduleReminder(it, updatedReminder) }
                    _editorEvents.send(RemindersEvent.Feedback("Reminder updated"))
                } else {
                    val newReminder = ReminderEntity(
                        title = cleanTitle,
                        notes = current.notes.trim(),
                        dueDateEpochDay = current.dueDateEpochDay,
                        dueTimeMinutes = safeTimeMinutes,
                        repeatType = resolvedRepeatType.name,
                        repeatDaysMask = cleanMask,
                        priority = current.priority.name,
                        notifyEnabled = current.notifyEnabled
                    )
                    val newId = repository.insertReminder(newReminder)
                    appContext?.let { ReminderAlarmScheduler.scheduleReminder(it, newReminder.copy(id = newId)) }
                    if (current.notifyEnabled) {
                        repository.insertNotification(
                            NotificationEntity(
                                title = "Reminder scheduled: $cleanTitle",
                                message = "${DateTimeUtils.formatShortDate(current.dueDateEpochDay)} · ${formatTimeOfDay(safeTimeMinutes)}",
                                type = NotificationType.REMINDER.name
                            )
                        )
                    }
                    _editorEvents.send(RemindersEvent.Feedback("Reminder added"))
                }
                editorState.update { it.copy(isSaving = false) }
                _editorEvents.send(RemindersEvent.SavedSuccess)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                editorState.update { it.copy(isSaving = false) }
                _editorEvents.send(
                    RemindersEvent.Feedback(e.localizedMessage ?: "Failed to save reminder")
                )
            }
        }
    }

    fun toggleComplete(reminder: ReminderEntity) {
        viewModelScope.launch {
            try {
                val nowCompleted = !reminder.isCompleted
                val updated = reminder.copy(
                    isCompleted = nowCompleted,
                    completedAt = if (nowCompleted) System.currentTimeMillis() else null
                )
                repository.updateReminder(updated)
                appContext?.let { ReminderAlarmScheduler.scheduleReminder(it, updated) }
                if (nowCompleted) {
                    repository.insertNotification(
                        NotificationEntity(
                            title = "Completed: ${reminder.title}",
                            message = "Moved to Completed reminders",
                            type = NotificationType.REMINDER.name
                        )
                    )
                    _listEvents.send(RemindersEvent.Feedback("Completed \"${reminder.title}\""))
                    _detailEvents.send(RemindersEvent.Feedback("Completed \"${reminder.title}\""))
                } else {
                    _listEvents.send(RemindersEvent.Feedback("Marked \"${reminder.title}\" as pending"))
                    _detailEvents.send(RemindersEvent.Feedback("Marked \"${reminder.title}\" as pending"))
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _listEvents.send(RemindersEvent.Feedback(e.localizedMessage ?: "Failed to update reminder"))
            }
        }
    }

    fun snoozeReminder(reminder: ReminderEntity, addMinutes: Int) {
        viewModelScope.launch {
            try {
                val safeMins = reminder.dueTimeMinutes.coerceIn(0, 1439)
                val scheduledDateTime = LocalDateTime.of(
                    LocalDate.ofEpochDay(reminder.dueDateEpochDay),
                    LocalTime.of(safeMins / 60, safeMins % 60)
                )
                val now = LocalDateTime.now().withSecond(0).withNano(0)
                val baseTime = if (scheduledDateTime.isAfter(now)) scheduledDateTime else now
                val targetDateTime = baseTime.plusMinutes(addMinutes.toLong())
                val newEpochDay = targetDateTime.toLocalDate().toEpochDay()
                val newMinutes = targetDateTime.hour * 60 + targetDateTime.minute

                val updated = reminder.copy(
                    dueDateEpochDay = newEpochDay,
                    dueTimeMinutes = newMinutes,
                    isCompleted = false,
                    completedAt = null
                )
                repository.updateReminder(updated)
                appContext?.let { ReminderAlarmScheduler.scheduleReminder(it, updated) }
                _detailEvents.send(RemindersEvent.Feedback("Snoozed to ${formatTimeOfDay(newMinutes)}"))
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _detailEvents.send(RemindersEvent.Feedback(e.localizedMessage ?: "Failed to snooze reminder"))
            }
        }
    }

    fun snoozeReminderToTomorrowMorning(reminder: ReminderEntity) {
        viewModelScope.launch {
            try {
                val tomorrowMorning = LocalDate.now().plusDays(1)
                val targetMinutes = 9 * 60
                val updated = reminder.copy(
                    dueDateEpochDay = tomorrowMorning.toEpochDay(),
                    dueTimeMinutes = targetMinutes,
                    isCompleted = false,
                    completedAt = null
                )
                repository.updateReminder(updated)
                appContext?.let { ReminderAlarmScheduler.scheduleReminder(it, updated) }
                _detailEvents.send(RemindersEvent.Feedback("Snoozed to tomorrow · 9:00 AM"))
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _detailEvents.send(RemindersEvent.Feedback(e.localizedMessage ?: "Failed to snooze reminder"))
            }
        }
    }

    fun deleteReminder(reminderId: Long) {
        viewModelScope.launch {
            try {
                repository.deleteReminderById(reminderId)
                appContext?.let { ReminderAlarmScheduler.cancelReminder(it, reminderId) }
                _listEvents.send(RemindersEvent.Feedback("Reminder deleted"))
                _detailEvents.send(RemindersEvent.DeletedSuccess)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _detailEvents.send(RemindersEvent.Feedback(e.localizedMessage ?: "Failed to delete reminder"))
            }
        }
    }

    companion object {
        fun formatTimeOfDay(minutesFromMidnight: Int): String {
            val safe = minutesFromMidnight.coerceIn(0, 1439)
            val hour24 = safe / 60
            val min = safe % 60
            val amPm = if (hour24 < 12) "AM" else "PM"
            val hour12 = when {
                hour24 == 0 -> 12
                hour24 > 12 -> hour24 - 12
                else -> hour24
            }
            return String.format(Locale.getDefault(), "%d:%02d %s", hour12, min, amPm)
        }
    }
}
