package com.example.edutrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.edutrack.data.local.entity.NotificationEntity
import com.example.edutrack.data.local.entity.NotificationType
import com.example.edutrack.data.local.entity.ReminderEntity
import com.example.edutrack.data.local.entity.ReminderPriority
import com.example.edutrack.data.local.entity.ReminderRepeatType
import com.example.edutrack.data.repository.EduTrackRepository
import com.example.edutrack.util.DateTimeUtils
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    val dueTimeMinutes: Int = 9 * 60, // 9:00 AM
    val repeatType: ReminderRepeatType = ReminderRepeatType.NONE,
    val repeatDaysMask: Int = 0b0010101,
    val priority: ReminderPriority = ReminderPriority.NORMAL,
    val notifyEnabled: Boolean = true,
    val titleError: String? = null
)

data class RemindersUiState(
    val selectedTab: ReminderTabFilter = ReminderTabFilter.TODAY,
    val visibleReminders: List<ReminderEntity> = emptyList(),
    val todayCount: Int = 0,
    val upcomingCount: Int = 0,
    val completedCount: Int = 0,
    val editorState: ReminderEditorState = ReminderEditorState(),
    val isLoading: Boolean = true
)

sealed interface RemindersEvent {
    data class Feedback(val message: String) : RemindersEvent
    data object SavedSuccess : RemindersEvent
    data object DeletedSuccess : RemindersEvent
}

class RemindersViewModel(
    private val repository: EduTrackRepository
) : ViewModel() {

    private val selectedTab = MutableStateFlow(ReminderTabFilter.TODAY)
    private val editorState = MutableStateFlow(ReminderEditorState())

    private val _events = Channel<RemindersEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val uiState: StateFlow<RemindersUiState> = combine(
        repository.observeAllReminders(),
        selectedTab,
        editorState
    ) { allReminders, tab, editor ->
        val todayEpoch = DateTimeUtils.todayEpochDay()
        val todayList = allReminders.filter { !it.isCompleted && it.dueDateEpochDay <= todayEpoch }
        val upcomingList = allReminders.filter { !it.isCompleted && it.dueDateEpochDay > todayEpoch }
        val completedList = allReminders.filter { it.isCompleted }

        val visible = when (tab) {
            ReminderTabFilter.TODAY -> todayList
            ReminderTabFilter.UPCOMING -> upcomingList
            ReminderTabFilter.COMPLETED -> completedList
        }

        RemindersUiState(
            selectedTab = tab,
            visibleReminders = visible,
            todayCount = todayList.size,
            upcomingCount = upcomingList.size,
            completedCount = completedList.size,
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

    fun prepareNewReminder() {
        editorState.value = ReminderEditorState(
            dueDateEpochDay = DateTimeUtils.todayEpochDay(),
            dueTimeMinutes = 9 * 60
        )
    }

    fun loadReminderForEdit(reminderId: Long) {
        if (reminderId <= 0L) {
            prepareNewReminder()
            return
        }
        viewModelScope.launch {
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
                    notifyEnabled = reminder.notifyEnabled
                )
            }
        }
    }

    fun onTitleChanged(title: String) = editorState.update { it.copy(title = title, titleError = null) }
    fun onNotesChanged(notes: String) = editorState.update { it.copy(notes = notes) }
    fun onDueDateSelected(epochDay: Long) = editorState.update { it.copy(dueDateEpochDay = epochDay) }
    fun onDueTimeSelected(minutesFromMidnight: Int) = editorState.update { it.copy(dueTimeMinutes = minutesFromMidnight) }
    fun onRepeatTypeSelected(repeatType: ReminderRepeatType) = editorState.update { it.copy(repeatType = repeatType) }
    fun onRepeatDaysMaskChanged(mask: Int) = editorState.update {
        it.copy(
            repeatType = if (mask == 0) ReminderRepeatType.NONE else ReminderRepeatType.CUSTOM,
            repeatDaysMask = mask
        )
    }
    fun onToggleRepeatDayBit(dayIndexZeroToSix: Int) = editorState.update { current ->
        val nextMask = current.repeatDaysMask xor (1 shl dayIndexZeroToSix)
        current.copy(repeatDaysMask = nextMask)
    }
    fun onPrioritySelected(priority: ReminderPriority) = editorState.update { it.copy(priority = priority) }
    fun onNotifyToggle(enabled: Boolean) = editorState.update { it.copy(notifyEnabled = enabled) }

    fun saveReminder() {
        val current = editorState.value
        val cleanTitle = current.title.trim()
        if (cleanTitle.isEmpty()) {
            editorState.update { it.copy(titleError = "Task title is required") }
            return
        }

        viewModelScope.launch {
            val editingId = current.editingId ?: 0L
            if (editingId > 0L) {
                val existing = repository.getReminderById(editingId)
                repository.updateReminder(
                    ReminderEntity(
                        id = editingId,
                        title = cleanTitle,
                        notes = current.notes.trim(),
                        dueDateEpochDay = current.dueDateEpochDay,
                        dueTimeMinutes = current.dueTimeMinutes,
                        repeatType = current.repeatType.name,
                        repeatDaysMask = current.repeatDaysMask,
                        priority = current.priority.name,
                        notifyEnabled = current.notifyEnabled,
                        isCompleted = existing?.isCompleted ?: false,
                        completedAt = existing?.completedAt,
                        createdAt = existing?.createdAt ?: System.currentTimeMillis()
                    )
                )
                _events.send(RemindersEvent.Feedback("Reminder updated"))
            } else {
                repository.insertReminder(
                    ReminderEntity(
                        title = cleanTitle,
                        notes = current.notes.trim(),
                        dueDateEpochDay = current.dueDateEpochDay,
                        dueTimeMinutes = current.dueTimeMinutes,
                        repeatType = current.repeatType.name,
                        repeatDaysMask = current.repeatDaysMask,
                        priority = current.priority.name,
                        notifyEnabled = current.notifyEnabled
                    )
                )
                if (current.notifyEnabled) {
                    repository.insertNotification(
                        NotificationEntity(
                            title = "Reminder scheduled: $cleanTitle",
                            message = "${DateTimeUtils.formatShortDate(current.dueDateEpochDay)} · ${formatTimeOfDay(current.dueTimeMinutes)}",
                            type = NotificationType.REMINDER.name
                        )
                    )
                }
                _events.send(RemindersEvent.Feedback("Reminder added"))
            }
            _events.send(RemindersEvent.SavedSuccess)
        }
    }

    fun toggleComplete(reminder: ReminderEntity) {
        viewModelScope.launch {
            val nowCompleted = !reminder.isCompleted
            repository.updateReminder(
                reminder.copy(
                    isCompleted = nowCompleted,
                    completedAt = if (nowCompleted) System.currentTimeMillis() else null
                )
            )
            if (nowCompleted) {
                repository.insertNotification(
                    NotificationEntity(
                        title = "Completed: ${reminder.title}",
                        message = "Moved to Completed reminders",
                        type = NotificationType.REMINDER.name
                    )
                )
                _events.send(RemindersEvent.Feedback("Completed \"${reminder.title}\""))
            }
        }
    }

    fun snoozeReminder(reminder: ReminderEntity, addMinutes: Int) {
        viewModelScope.launch {
            val totalMins = reminder.dueTimeMinutes + addMinutes
            val dayShift = totalMins / 1440
            val newMinutes = totalMins % 1440
            repository.updateReminder(
                reminder.copy(
                    dueDateEpochDay = reminder.dueDateEpochDay + dayShift,
                    dueTimeMinutes = newMinutes,
                    isCompleted = false,
                    completedAt = null
                )
            )
            _events.send(RemindersEvent.Feedback("Snoozed to ${formatTimeOfDay(newMinutes)}"))
        }
    }

    fun deleteReminder(reminderId: Long) {
        viewModelScope.launch {
            repository.deleteReminderById(reminderId)
            _events.send(RemindersEvent.Feedback("Reminder deleted"))
            _events.send(RemindersEvent.DeletedSuccess)
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
            return String.format("%d:%02d %s", hour12, min, amPm)
        }
    }
}
