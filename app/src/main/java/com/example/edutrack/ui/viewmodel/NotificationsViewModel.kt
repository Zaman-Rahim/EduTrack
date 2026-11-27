package com.example.edutrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.edutrack.data.local.entity.NotificationEntity
import com.example.edutrack.data.repository.EduTrackRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val notifications: List<NotificationEntity> = emptyList(),
    val unreadCount: Int = 0
)

class NotificationsViewModel(
    private val repository: EduTrackRepository
) : ViewModel() {

    private val _events = Channel<String>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val uiState: StateFlow<NotificationsUiState> = combine(
        repository.observeAllNotifications(),
        repository.observeUnreadNotificationCount()
    ) { items, unread ->
        NotificationsUiState(
            notifications = items,
            unreadCount = unread
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NotificationsUiState()
    )

    fun markAllAsRead() {
        viewModelScope.launch {
            try {
                repository.markAllNotificationsAsRead()
                _events.send("All notifications marked as read")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _events.send(e.localizedMessage ?: "Failed to update notifications")
            }
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            try {
                repository.clearAllNotifications()
                _events.send("Notifications cleared")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _events.send(e.localizedMessage ?: "Failed to clear notifications")
            }
        }
    }
}
