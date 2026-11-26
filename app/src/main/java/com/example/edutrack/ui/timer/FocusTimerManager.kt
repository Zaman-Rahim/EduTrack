package com.example.edutrack.ui.timer

import android.content.Context
import android.media.RingtoneManager
import com.example.edutrack.data.local.entity.NotificationEntity
import com.example.edutrack.data.local.entity.NotificationType
import com.example.edutrack.data.local.entity.StudySessionEntity
import com.example.edutrack.data.local.entity.SubjectEntity
import com.example.edutrack.data.preferences.UserPreferencesManager
import com.example.edutrack.data.repository.EduTrackRepository
import com.example.edutrack.util.DateTimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class TimerStatus {
    IDLE,
    RUNNING,
    PAUSED,
    COMPLETED
}

data class FocusTimerUiState(
    val subject: SubjectEntity? = null,
    val targetMinutes: Int = 25,
    val totalSeconds: Int = 0,
    val remainingSeconds: Int = 0,
    val status: TimerStatus = TimerStatus.IDLE,
    val isMinimized: Boolean = false
) {
    val isActive: Boolean get() = status != TimerStatus.IDLE
    val progressFraction: Float
        get() = if (totalSeconds <= 0) 0f else (remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
}

/**
 * Application-scoped countdown focus timer that continues running when minimized
 * into the Uber/Foodpanda-style bottom Live Focus Bar, logs completed sessions
 * directly to Room, and records an in-app notification upon reaching 00:00.
 */
class FocusTimerManager(
    private val context: Context,
    private val repository: EduTrackRepository,
    private val preferencesManager: UserPreferencesManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var tickerJob: Job? = null

    private val _state = MutableStateFlow(FocusTimerUiState())
    val state: StateFlow<FocusTimerUiState> = _state.asStateFlow()

    fun startFocus(subject: SubjectEntity, durationMinutes: Int = subject.dailyGoalMinutes) {
        val safeMinutes = durationMinutes.coerceAtLeast(1)
        val totalSec = safeMinutes * 60
        tickerJob?.cancel()
        _state.value = FocusTimerUiState(
            subject = subject,
            targetMinutes = safeMinutes,
            totalSeconds = totalSec,
            remainingSeconds = totalSec,
            status = TimerStatus.RUNNING,
            isMinimized = false
        )
        startTicker()
    }

    fun togglePauseResume() {
        when (_state.value.status) {
            TimerStatus.RUNNING -> {
                tickerJob?.cancel()
                _state.update { it.copy(status = TimerStatus.PAUSED) }
            }
            TimerStatus.PAUSED -> {
                _state.update { it.copy(status = TimerStatus.RUNNING) }
                startTicker()
            }
            else -> Unit
        }
    }

    fun minimize() {
        _state.update { it.copy(isMinimized = true) }
    }

    fun expand() {
        _state.update { it.copy(isMinimized = false) }
    }

    fun dismissOrCancel() {
        tickerJob?.cancel()
        _state.value = FocusTimerUiState()
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (_state.value.status == TimerStatus.RUNNING && _state.value.remainingSeconds > 0) {
                delay(1_000L)
                val next = (_state.value.remainingSeconds - 1).coerceAtLeast(0)
                _state.update { it.copy(remainingSeconds = next) }
                if (next == 0) {
                    onTimerReachedZero()
                    break
                }
            }
        }
    }

    private fun onTimerReachedZero() {
        val current = _state.value
        val subject = current.subject ?: return
        _state.update { it.copy(status = TimerStatus.COMPLETED) }

        if (preferencesManager.state.value.timerCompletionSound) {
            playCompletionSound()
        }

        scope.launch(Dispatchers.IO) {
            val todayEpoch = DateTimeUtils.todayEpochDay()
            repository.insertSession(
                StudySessionEntity(
                    subjectId = subject.id,
                    durationMinutes = current.targetMinutes,
                    sessionDateEpochDay = todayEpoch,
                    notes = "Completed ${current.targetMinutes}m focus session"
                )
            )
            repository.insertNotification(
                NotificationEntity(
                    title = "${subject.name} goal complete",
                    message = "${current.targetMinutes} min focused · Moved to Completed",
                    type = NotificationType.FOCUS_COMPLETE.name
                )
            )
        }
    }

    private fun playCompletionSound() {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context.applicationContext, uri)
            ringtone?.play()
        } catch (_: Exception) {
            // Safe fallback on headless/muted environments
        }
    }
}
