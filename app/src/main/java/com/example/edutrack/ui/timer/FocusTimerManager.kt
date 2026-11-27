package com.example.edutrack.ui.timer

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import com.example.edutrack.data.local.entity.NotificationEntity
import com.example.edutrack.data.local.entity.NotificationType
import com.example.edutrack.data.local.entity.StudySessionEntity
import com.example.edutrack.data.local.entity.SubjectEntity
import com.example.edutrack.data.preferences.AlarmToneOption
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
    val isMinimized: Boolean = false,
    val isBarHidden: Boolean = false,
    val isAlarmRinging: Boolean = false
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
    private var alarmJob: Job? = null
    private var previewJob: Job? = null
    private var activeRingtone: Ringtone? = null
    private var activeToneGenerator: ToneGenerator? = null

    private val _state = MutableStateFlow(FocusTimerUiState())
    val state: StateFlow<FocusTimerUiState> = _state.asStateFlow()

    fun startFocus(subject: SubjectEntity, durationMinutes: Int = subject.dailyGoalMinutes) {
        val safeMinutes = durationMinutes.coerceAtLeast(1)
        val totalSec = safeMinutes * 60
        tickerJob?.cancel()
        stopAlarm()
        _state.value = FocusTimerUiState(
            subject = subject,
            targetMinutes = safeMinutes,
            totalSeconds = totalSec,
            remainingSeconds = totalSec,
            status = TimerStatus.RUNNING,
            isMinimized = false,
            isBarHidden = false
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
        _state.update { it.copy(isMinimized = true, isBarHidden = false) }
    }

    fun expand() {
        _state.update { it.copy(isMinimized = false, isBarHidden = false) }
    }

    fun hideLiveBar() {
        _state.update { it.copy(isBarHidden = true) }
    }

    fun unhideLiveBar() {
        _state.update { it.copy(isBarHidden = false) }
    }

    fun dismissOrCancel() {
        tickerJob?.cancel()
        stopAlarm()
        _state.value = FocusTimerUiState()
    }

    fun stopAlarm() {
        alarmJob?.cancel()
        alarmJob = null
        previewJob?.cancel()
        previewJob = null
        releaseActiveAudio()
        _state.update { it.copy(isAlarmRinging = false) }
    }

    fun previewAlarmTone(tone: AlarmToneOption) {
        previewJob?.cancel()
        releaseActiveAudio()
        previewJob = scope.launch {
            val ringtone = createRingtoneForTone(tone)
            if (ringtone != null) {
                activeRingtone = ringtone
                try {
                    ringtone.play()
                } catch (_: Exception) {
                }
                delay(1_000L)
                releaseActiveAudio()
            } else {
                playToneBeep(tone, durationMs = 350)
                delay(450L)
                playToneBeep(tone, durationMs = 350)
                delay(200L)
                releaseActiveAudio()
            }
        }
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
            val tone = AlarmToneOption.fromName(preferencesManager.state.value.alarmTone)
            startAlarmLoop(tone)
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

    private fun startAlarmLoop(tone: AlarmToneOption) {
        alarmJob?.cancel()
        previewJob?.cancel()
        releaseActiveAudio()
        _state.update { it.copy(isAlarmRinging = true) }

        alarmJob = scope.launch {
            val startTime = System.currentTimeMillis()
            val ringtone = createRingtoneForTone(tone)
            activeRingtone = ringtone

            while (System.currentTimeMillis() - startTime < ALARM_AUTO_STOP_MS) {
                if (ringtone != null) {
                    try {
                        if (!ringtone.isPlaying) {
                            ringtone.play()
                        }
                    } catch (_: Exception) {
                        playToneBeep(tone, durationMs = 300)
                    }
                    delay(1_200L)
                } else {
                    playToneBeep(tone, durationMs = 260)
                    delay(320L)
                    if (System.currentTimeMillis() - startTime >= ALARM_AUTO_STOP_MS) break
                    playToneBeep(tone, durationMs = 260)
                    delay(900L)
                }
            }
            stopAlarm()
        }
    }

    private fun createRingtoneForTone(tone: AlarmToneOption): Ringtone? {
        if (tone == AlarmToneOption.DIGITAL_BEEP) return null
        return try {
            val type = when (tone) {
                AlarmToneOption.CLASSIC_ALARM -> RingtoneManager.TYPE_ALARM
                AlarmToneOption.GENTLE_CHIME -> RingtoneManager.TYPE_NOTIFICATION
                AlarmToneOption.BRIGHT_BELL -> RingtoneManager.TYPE_RINGTONE
                AlarmToneOption.DIGITAL_BEEP -> return null
            }
            val uri = RingtoneManager.getDefaultUri(type)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: return null
            RingtoneManager.getRingtone(context.applicationContext, uri)?.apply {
                audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun playToneBeep(tone: AlarmToneOption, durationMs: Int) {
        try {
            val tg = activeToneGenerator ?: ToneGenerator(AudioManager.STREAM_ALARM, 90).also {
                activeToneGenerator = it
            }
            val toneType = when (tone) {
                AlarmToneOption.CLASSIC_ALARM -> ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD
                AlarmToneOption.GENTLE_CHIME -> ToneGenerator.TONE_PROP_BEEP2
                AlarmToneOption.BRIGHT_BELL -> ToneGenerator.TONE_PROP_ACK
                AlarmToneOption.DIGITAL_BEEP -> ToneGenerator.TONE_PROP_BEEP
            }
            tg.startTone(toneType, durationMs)
        } catch (_: Exception) {
            // Safe fallback on headless/muted environments
        }
    }

    private fun releaseActiveAudio() {
        try {
            activeRingtone?.stop()
        } catch (_: Exception) {
        }
        activeRingtone = null
        try {
            activeToneGenerator?.stopTone()
            activeToneGenerator?.release()
        } catch (_: Exception) {
        }
        activeToneGenerator = null
    }

    private companion object {
        const val ALARM_AUTO_STOP_MS = 30_000L
    }
}
