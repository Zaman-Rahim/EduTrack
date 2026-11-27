package com.example.edutrack.ui.timer

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.PowerManager
import com.example.edutrack.data.local.entity.NotificationEntity
import com.example.edutrack.data.local.entity.NotificationType
import com.example.edutrack.data.local.entity.StudySessionEntity
import com.example.edutrack.data.local.entity.SubjectEntity
import com.example.edutrack.data.preferences.ActiveTimerSnapshot
import com.example.edutrack.data.preferences.UserPreferencesManager
import com.example.edutrack.data.repository.EduTrackRepository
import com.example.edutrack.notifications.NotificationHelper
import com.example.edutrack.notifications.ReminderAlarmScheduler
import com.example.edutrack.service.FocusTimerService
import com.example.edutrack.util.DateTimeUtils
import com.example.edutrack.util.SystemSoundHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val endEpochMillis: Long = 0L,
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
 * Application-scoped countdown focus timer backed by wall-clock timestamps,
 * [FocusTimerService] (Foreground Service), and [ReminderAlarmScheduler] so
 * countdowns survive leaving the app and turning off the screen, enforce a
 * single active subject at a time, and fire real Android notifications + alarms at 00:00.
 */
class FocusTimerManager(
    private val context: Context,
    private val repository: EduTrackRepository,
    private val preferencesManager: UserPreferencesManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var tickerJob: Job? = null
    private var alarmJob: Job? = null
    private data class PausedSubjectState(
        val subject: SubjectEntity,
        val targetMinutes: Int,
        val totalSeconds: Int,
        val remainingSeconds: Int
    )

    private val pausedSubjects = mutableMapOf<Long, PausedSubjectState>()

    private val alarmLock = Any()
    private var activeRingtone: Ringtone? = null
    private var activeToneGenerator: ToneGenerator? = null
    private var alarmWakeLock: PowerManager.WakeLock? = null
    private var alarmSessionToken: Long = 0L

    private var endEpochMillis: Long = 0L

    private val _state = MutableStateFlow(FocusTimerUiState())
    val state: StateFlow<FocusTimerUiState> = _state.asStateFlow()

    init {
        restoreSnapshot()
    }

    /**
     * Returns true only when a different subject currently has an actively RUNNING focus timer.
     * Paused timers do not block switching to another subject.
     */
    fun isAnotherSubjectRunning(subjectId: Long): Boolean {
        val current = _state.value
        return current.status == TimerStatus.RUNNING && current.subject?.id != subjectId
    }

    fun isAnotherSubjectActive(subjectId: Long): Boolean = isAnotherSubjectRunning(subjectId)

    fun startFocus(subject: SubjectEntity, durationMinutes: Int = subject.dailyGoalMinutes): Boolean {
        if (isAnotherSubjectRunning(subject.id)) {
            return false
        }

        val current = _state.value
        if (current.status == TimerStatus.PAUSED && current.subject != null && current.subject.id != subject.id) {
            pausedSubjects[current.subject.id] = PausedSubjectState(
                subject = current.subject,
                targetMinutes = current.targetMinutes,
                totalSeconds = current.totalSeconds,
                remainingSeconds = current.remainingSeconds.coerceAtLeast(1)
            )
        }

        val resumed = pausedSubjects.remove(subject.id)
        val safeMinutes = resumed?.targetMinutes ?: durationMinutes.coerceAtLeast(1)
        val totalSec = resumed?.totalSeconds ?: (safeMinutes * 60)
        val remainingSec = resumed?.remainingSeconds ?: totalSec

        tickerJob?.cancel()
        stopAlarm()

        val targetEnd = System.currentTimeMillis() + remainingSec * 1000L
        endEpochMillis = targetEnd

        val newState = FocusTimerUiState(
            subject = subject,
            targetMinutes = safeMinutes,
            totalSeconds = totalSec,
            remainingSeconds = remainingSec,
            endEpochMillis = targetEnd,
            status = TimerStatus.RUNNING,
            isMinimized = false,
            isBarHidden = false,
            isAlarmRinging = false
        )
        _state.value = newState
        persistSnapshot(newState)

        FocusTimerService.start(context)
        ReminderAlarmScheduler.scheduleFocusCompletion(context, targetEnd, subject.name)
        startTicker()
        return true
    }

    fun togglePauseResume() {
        val current = _state.value
        when (current.status) {
            TimerStatus.RUNNING -> {
                tickerJob?.cancel()
                ReminderAlarmScheduler.cancelFocusCompletion(context)
                val remaining = computeRemainingSeconds()
                if (remaining <= 0) {
                    onCountdownCompleted()
                    return
                }
                endEpochMillis = 0L
                val pausedState = current.copy(
                    status = TimerStatus.PAUSED,
                    remainingSeconds = remaining,
                    endEpochMillis = 0L
                )
                _state.value = pausedState
                persistSnapshot(pausedState)
            }
            TimerStatus.PAUSED -> {
                val targetEnd = System.currentTimeMillis() + current.remainingSeconds * 1000L
                endEpochMillis = targetEnd
                val runningState = current.copy(
                    status = TimerStatus.RUNNING,
                    endEpochMillis = targetEnd
                )
                _state.value = runningState
                persistSnapshot(runningState)
                FocusTimerService.start(context)
                ReminderAlarmScheduler.scheduleFocusCompletion(
                    context = context,
                    endEpochMillis = targetEnd,
                    subjectName = current.subject?.name ?: "Study Focus"
                )
                startTicker()
            }
            else -> Unit
        }
    }

    fun minimize() {
        _state.update { it.copy(isMinimized = true, isBarHidden = false) }
        persistSnapshotIfActive()
    }

    fun expand() {
        _state.update { it.copy(isMinimized = false, isBarHidden = false) }
        persistSnapshotIfActive()
    }

    fun hideLiveBar() {
        _state.update { it.copy(isBarHidden = true) }
        persistSnapshotIfActive()
    }

    fun unhideLiveBar() {
        _state.update { it.copy(isBarHidden = false) }
        persistSnapshotIfActive()
    }

    fun stopAlarm() {
        stopAlarmResources(releaseWakeLock = true)
        _state.update { it.copy(isAlarmRinging = false) }
        NotificationHelper.cancelFocusCompleteNotification(context)
    }

    /**
     * Marks the active or paused focus timer as completed right now without waiting
     * for the countdown to finish or ringing the completion alarm.
     */
    fun completeNowManually() {
        val current = _state.value
        if (current.status != TimerStatus.RUNNING && current.status != TimerStatus.PAUSED) return

        val subject = current.subject
        subject?.id?.let { pausedSubjects.remove(it) }
        tickerJob?.cancel()
        stopAlarm()
        ReminderAlarmScheduler.cancelFocusCompletion(context)
        FocusTimerService.stop(context)
        preferencesManager.clearActiveTimerSnapshot()
        endEpochMillis = 0L

        _state.update {
            it.copy(
                remainingSeconds = 0,
                endEpochMillis = 0L,
                status = TimerStatus.COMPLETED,
                isBarHidden = false,
                isAlarmRinging = false
            )
        }

        if (subject != null) {
            val loggedMinutes = maxOf(current.targetMinutes, subject.dailyGoalMinutes).coerceAtLeast(1)
            scope.launch(Dispatchers.IO) {
                try {
                    val existingSubject = repository.getSubjectById(subject.id)
                    if (existingSubject != null) {
                        repository.insertSession(
                            StudySessionEntity(
                                subjectId = existingSubject.id,
                                durationMinutes = loggedMinutes,
                                sessionDateEpochDay = DateTimeUtils.todayEpochDay(),
                                notes = "Marked focus session complete"
                            )
                        )
                        val formattedDuration = DateTimeUtils.formatDuration(loggedMinutes)
                        repository.insertNotification(
                            NotificationEntity(
                                title = "Completed · ${existingSubject.name}",
                                message = "Logged $formattedDuration and moved to Completed today.",
                                type = NotificationType.FOCUS_COMPLETE.name
                            )
                        )
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                }
            }
        }
    }

    fun dismissOrCancel() {
        val subjectId = _state.value.subject?.id
        if (subjectId != null) {
            pausedSubjects.remove(subjectId)
        }
        tickerJob?.cancel()
        stopAlarm()
        ReminderAlarmScheduler.cancelFocusCompletion(context)
        FocusTimerService.stop(context)
        endEpochMillis = 0L
        preferencesManager.clearActiveTimerSnapshot()
        _state.value = FocusTimerUiState()
    }

    /**
     * Cancels any active/paused/completed timer belonging to a subject that was deleted.
     */
    fun dismissIfSubjectDeleted(subjectId: Long) {
        if (subjectId <= 0L) return
        pausedSubjects.remove(subjectId)
        val current = _state.value
        if (current.isActive && current.subject?.id == subjectId) {
            dismissOrCancel()
        }
    }

    /**
     * Re-arms the completion alarm when system clock/timezone/exact-alarm permission changes.
     */
    fun rescheduleAlarmIfRunning() {
        val current = _state.value
        if (current.status != TimerStatus.RUNNING) return
        val targetEnd = current.endEpochMillis.takeIf { it > 0L } ?: endEpochMillis
        if (targetEnd <= 0L) return
        if (System.currentTimeMillis() >= targetEnd - COMPLETE_TOLERANCE_MS) {
            completeNowIfDue(forceFromAlarm = true)
        } else {
            ReminderAlarmScheduler.scheduleFocusCompletion(
                context = context,
                endEpochMillis = targetEnd,
                subjectName = current.subject?.name ?: "Study Focus"
            )
        }
    }

    fun completeNowIfDue(forceFromAlarm: Boolean = false) {
        val current = _state.value
        if (current.status == TimerStatus.RUNNING) {
            val targetEnd = current.endEpochMillis.takeIf { it > 0L } ?: endEpochMillis
            val isDue = forceFromAlarm ||
                (targetEnd > 0L && System.currentTimeMillis() >= targetEnd - COMPLETE_TOLERANCE_MS)
            if (isDue) {
                tickerJob?.cancel()
                onCountdownCompleted()
            }
        }
    }

    private fun computeRemainingSeconds(): Int =
        computeRemainingSecondsFor(endEpochMillis)

    private fun computeRemainingSecondsFor(targetEndMillis: Long): Int {
        if (targetEndMillis <= 0L) return _state.value.remainingSeconds
        val diffMs = targetEndMillis - System.currentTimeMillis()
        return if (diffMs <= 0L) 0 else ((diffMs + 999L) / 1000L).toInt()
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (_state.value.status == TimerStatus.RUNNING) {
                val now = System.currentTimeMillis()
                val diffMs = endEpochMillis - now
                if (diffMs <= 0L) {
                    onCountdownCompleted()
                    break
                }
                val remaining = ((diffMs + 999L) / 1000L).toInt()
                if (_state.value.remainingSeconds != remaining) {
                    _state.update { it.copy(remainingSeconds = remaining) }
                }
                val alignedDelay = (1000L - (now % 1000L)).coerceIn(100L, 1000L)
                val sleepMs = if (diffMs < alignedDelay) diffMs.coerceAtLeast(50L) else alignedDelay
                delay(sleepMs)
            }
        }
    }

    private fun onCountdownCompleted() {
        val current = _state.value
        if (current.status != TimerStatus.RUNNING) return
        val subject = current.subject
        val duration = current.targetMinutes.coerceAtLeast(1)
        val prefs = preferencesManager.state.value

        preferencesManager.clearActiveTimerSnapshot()
        ReminderAlarmScheduler.cancelFocusCompletion(context)
        FocusTimerService.stop(context)
        endEpochMillis = 0L

        acquireAlarmWakeLock()

        try {
            _state.update {
                it.copy(
                    remainingSeconds = 0,
                    endEpochMillis = 0L,
                    status = TimerStatus.COMPLETED,
                    isBarHidden = false,
                    isAlarmRinging = prefs.timerCompletionSound
                )
            }

            NotificationHelper.showFocusCompleteNotification(
                context = context,
                subjectName = subject?.name ?: "Study Focus",
                durationMinutes = duration,
                alarmSoundEnabled = prefs.timerCompletionSound
            )

            if (prefs.timerCompletionSound) {
                startAlarmLoop(prefs.timerToneUri)
            }

            if (subject != null) {
                scope.launch(Dispatchers.IO) {
                    try {
                        val existingSubject = repository.getSubjectById(subject.id)
                        if (existingSubject != null) {
                            repository.insertSession(
                                StudySessionEntity(
                                    subjectId = existingSubject.id,
                                    durationMinutes = duration,
                                    sessionDateEpochDay = DateTimeUtils.todayEpochDay(),
                                    notes = "Completed daily focus timer"
                                )
                            )
                            val formattedDuration = DateTimeUtils.formatDuration(duration)
                            repository.insertNotification(
                                NotificationEntity(
                                    title = "Focus complete · ${existingSubject.name}",
                                    message = "Logged $formattedDuration and moved to Completed today.",
                                    type = NotificationType.FOCUS_COMPLETE.name
                                )
                            )
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        // Guard against FK constraint failures if the subject was concurrently deleted.
                    } finally {
                        if (alarmJob?.isActive != true) {
                            releaseAlarmWakeLock()
                        }
                    }
                }
            } else if (!prefs.timerCompletionSound) {
                releaseAlarmWakeLock()
            }
        } catch (t: Throwable) {
            if (alarmJob?.isActive != true) {
                releaseAlarmWakeLock()
            }
            throw t
        }
    }

    private fun startAlarmLoop(toneUriString: String) {
        stopAlarmResources(releaseWakeLock = false)
        val myToken = synchronized(alarmLock) {
            alarmSessionToken++
            alarmSessionToken
        }
        _state.update { it.copy(isAlarmRinging = true) }

        alarmJob = scope.launch(Dispatchers.IO) {
            try {
                val alarmUri = SystemSoundHelper.resolveUri(toneUriString, RingtoneManager.TYPE_ALARM)
                val ringtone = alarmUri?.let {
                    runCatching { RingtoneManager.getRingtone(context.applicationContext, it) }.getOrNull()
                }

                var startedRingtone: Ringtone? = null
                if (ringtone != null) {
                    runCatching {
                        ringtone.audioAttributes = AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            ringtone.isLooping = true
                        }
                        synchronized(alarmLock) {
                            if (isActive && alarmSessionToken == myToken) {
                                ringtone.play()
                                activeRingtone = ringtone
                                startedRingtone = ringtone
                            } else {
                                runCatching { ringtone.stop() }
                            }
                        }
                    }.onFailure {
                        runCatching { ringtone.stop() }
                    }
                }

                val startedAt = System.currentTimeMillis()
                while (isActive && System.currentTimeMillis() - startedAt < ALARM_AUTO_STOP_MS) {
                    val stillValid = synchronized(alarmLock) { alarmSessionToken == myToken }
                    if (!stillValid) break

                    if (startedRingtone == null) {
                        val tg = runCatching { ToneGenerator(AudioManager.STREAM_ALARM, 90) }.getOrNull()
                        if (tg != null) {
                            var shouldPlay = false
                            synchronized(alarmLock) {
                                if (isActive && alarmSessionToken == myToken) {
                                    activeToneGenerator = tg
                                    shouldPlay = true
                                }
                            }
                            if (shouldPlay) {
                                runCatching { tg.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 650) }
                                try {
                                    delay(700L)
                                } finally {
                                    synchronized(alarmLock) {
                                        runCatching { tg.release() }
                                        if (activeToneGenerator === tg) {
                                            activeToneGenerator = null
                                        }
                                    }
                                }
                            } else {
                                runCatching { tg.release() }
                                break
                            }
                        }
                        delay(450L)
                    } else {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                            synchronized(alarmLock) {
                                if (alarmSessionToken == myToken && startedRingtone?.isPlaying == false) {
                                    runCatching { startedRingtone?.play() }
                                }
                            }
                        }
                        delay(1000L)
                    }
                }
            } finally {
                withContext(NonCancellable) {
                    val isCurrentSession = synchronized(alarmLock) { alarmSessionToken == myToken }
                    if (isCurrentSession) {
                        stopAlarmResources(releaseWakeLock = true)
                        _state.update { it.copy(isAlarmRinging = false) }
                    }
                }
            }
        }
    }

    private fun stopAlarmResources(releaseWakeLock: Boolean) {
        val jobToCancel = alarmJob
        alarmJob = null
        synchronized(alarmLock) {
            alarmSessionToken++
            runCatching { activeRingtone?.stop() }
            activeRingtone = null
            runCatching { activeToneGenerator?.release() }
            activeToneGenerator = null
        }
        jobToCancel?.cancel()
        if (releaseWakeLock) {
            releaseAlarmWakeLock()
        }
    }

    private fun acquireAlarmWakeLock() {
        releaseAlarmWakeLock()
        runCatching {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
            alarmWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG).apply {
                setReferenceCounted(false)
                acquire(ALARM_WAKELOCK_TIMEOUT_MS)
            }
        }
    }

    private fun releaseAlarmWakeLock() {
        runCatching {
            alarmWakeLock?.let { wl ->
                if (wl.isHeld) wl.release()
            }
        }
        alarmWakeLock = null
    }

    private fun persistSnapshotIfActive() {
        val current = _state.value
        if (current.status == TimerStatus.RUNNING || current.status == TimerStatus.PAUSED) {
            persistSnapshot(current)
        }
    }

    private fun persistSnapshot(state: FocusTimerUiState) {
        val subject = state.subject ?: return
        if (state.status != TimerStatus.RUNNING && state.status != TimerStatus.PAUSED) {
            preferencesManager.clearActiveTimerSnapshot()
            return
        }
        preferencesManager.saveActiveTimerSnapshot(
            ActiveTimerSnapshot(
                status = state.status.name,
                subjectId = subject.id,
                subjectName = subject.name,
                targetMinutes = state.targetMinutes,
                endEpochMillis = if (state.status == TimerStatus.RUNNING) state.endEpochMillis else 0L,
                pausedRemainingSeconds = if (state.status == TimerStatus.PAUSED) state.remainingSeconds else 0,
                isBarHidden = state.isBarHidden
            )
        )
    }

    private fun restoreSnapshot() {
        val snapshot = preferencesManager.readActiveTimerSnapshot() ?: return
        val restoredStatus = runCatching { TimerStatus.valueOf(snapshot.status) }.getOrNull()
        if (restoredStatus != TimerStatus.RUNNING && restoredStatus != TimerStatus.PAUSED) {
            preferencesManager.clearActiveTimerSnapshot()
            return
        }

        val safeMinutes = snapshot.targetMinutes.coerceAtLeast(1)
        val totalSec = safeMinutes * 60
        val placeholderSubject = SubjectEntity(
            id = snapshot.subjectId,
            name = snapshot.subjectName,
            colorHex = 0xFF2D6A4FL,
            weeklyGoalHours = safeMinutes / 60.0,
            dailyGoalMinutes = safeMinutes
        )

        when (restoredStatus) {
            TimerStatus.RUNNING -> {
                if (snapshot.endEpochMillis <= 0L) {
                    preferencesManager.clearActiveTimerSnapshot()
                    return
                }
                val now = System.currentTimeMillis()
                if (snapshot.endEpochMillis <= now) {
                    endEpochMillis = snapshot.endEpochMillis
                    _state.value = FocusTimerUiState(
                        subject = placeholderSubject,
                        targetMinutes = safeMinutes,
                        totalSeconds = totalSec,
                        remainingSeconds = 0,
                        endEpochMillis = snapshot.endEpochMillis,
                        status = TimerStatus.RUNNING,
                        isMinimized = false,
                        isBarHidden = false,
                        isAlarmRinging = false
                    )
                    onCountdownCompleted()
                } else {
                    val remainingSec = computeRemainingSecondsFor(snapshot.endEpochMillis)
                        .coerceIn(1, totalSec)
                    endEpochMillis = snapshot.endEpochMillis
                    _state.value = FocusTimerUiState(
                        subject = placeholderSubject,
                        targetMinutes = safeMinutes,
                        totalSeconds = totalSec,
                        remainingSeconds = remainingSec,
                        endEpochMillis = snapshot.endEpochMillis,
                        status = TimerStatus.RUNNING,
                        isMinimized = true,
                        isBarHidden = snapshot.isBarHidden,
                        isAlarmRinging = false
                    )
                    ReminderAlarmScheduler.scheduleFocusCompletion(
                        context = context,
                        endEpochMillis = snapshot.endEpochMillis,
                        subjectName = placeholderSubject.name
                    )
                    FocusTimerService.start(context)
                    startTicker()
                    refreshRestoredSubject(snapshot.subjectId)
                }
            }

            TimerStatus.PAUSED -> {
                val remainingSec = snapshot.pausedRemainingSeconds.coerceIn(1, totalSec)
                endEpochMillis = 0L
                _state.value = FocusTimerUiState(
                    subject = placeholderSubject,
                    targetMinutes = safeMinutes,
                    totalSeconds = totalSec,
                    remainingSeconds = remainingSec,
                    endEpochMillis = 0L,
                    status = TimerStatus.PAUSED,
                    isMinimized = true,
                    isBarHidden = snapshot.isBarHidden,
                    isAlarmRinging = false
                )
                refreshRestoredSubject(snapshot.subjectId)
            }

            else -> Unit
        }
    }

    private fun refreshRestoredSubject(subjectId: Long) {
        scope.launch(Dispatchers.IO) {
            try {
                val loaded = repository.getSubjectById(subjectId)
                if (loaded == null) {
                    withContext(Dispatchers.Main.immediate) {
                        dismissIfSubjectDeleted(subjectId)
                    }
                } else {
                    _state.update { current ->
                        if (current.isActive && current.subject?.id == subjectId) {
                            current.copy(subject = loaded)
                        } else {
                            current
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Keep placeholder subject if Room read fails transiently during startup.
            }
        }
    }

    private companion object {
        const val ALARM_AUTO_STOP_MS = 30_000L
        const val ALARM_WAKELOCK_TIMEOUT_MS = 40_000L
        const val COMPLETE_TOLERANCE_MS = 1_500L
        const val WAKE_LOCK_TAG = "edutrack:FocusCompleteWakeLock"
    }
}
