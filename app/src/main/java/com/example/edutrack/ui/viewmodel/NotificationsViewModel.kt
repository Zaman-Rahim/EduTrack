package com.example.edutrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.edutrack.data.local.entity.NotificationEntity
import com.example.edutrack.data.repository.EduTrackRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val notifications: List<NotificationEntity> = emptyList(),
    val unreadCount: Int = 0
)

class NotificationsViewModel(
    private val repository: EduTrackRepository
) : ViewModel() {

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
            repository.markAllNotificationsAsRead()
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            repository.clearAllNotifications()
        }
    }
}
