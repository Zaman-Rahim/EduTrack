package com.example.edutrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.edutrack.data.local.relation.SessionWithSubject
import com.example.edutrack.data.repository.EduTrackRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * UI state for [com.example.edutrack.ui.screens.detail.SessionDetailScreen].
 */
data class SessionDetailUiState(
    val sessionWithSubject: SessionWithSubject? = null,
    val isLoading: Boolean = true
)

/**
 * One-shot events emitted by [SessionDetailViewModel].
 */
sealed interface SessionDetailEvent {
    data class Deleted(val message: String) : SessionDetailEvent
    data class Error(val message: String) : SessionDetailEvent
}

/**
 * ViewModel displaying a single study session and handling deletion.
 */
class SessionDetailViewModel(
    private val repository: EduTrackRepository,
    private val sessionId: Long
) : ViewModel() {

    val uiState: StateFlow<SessionDetailUiState> =
        repository.observeSessionWithSubjectById(sessionId)
            .map { SessionDetailUiState(sessionWithSubject = it, isLoading = false) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = SessionDetailUiState()
            )

    private val _events = Channel<SessionDetailEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun deleteSession() {
        viewModelScope.launch {
            try {
                repository.deleteSessionById(sessionId)
                _events.send(SessionDetailEvent.Deleted("Study session deleted"))
            } catch (e: Exception) {
                _events.send(
                    SessionDetailEvent.Error(
                        e.localizedMessage ?: "Failed to delete study session"
                    )
                )
            }
        }
    }
}
