package com.example.edutrack.data.preferences

import android.content.Context
import android.media.RingtoneManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Snapshot of an in-progress or paused focus timer persisted across process death.
 */
data class ActiveTimerSnapshot(
    val status: String,
    val subjectId: Long,
    val subjectName: String,
    val targetMinutes: Int,
    val endEpochMillis: Long,
    val pausedRemainingSeconds: Int,
    val isBarHidden: Boolean
)

data class UserPreferencesState(
    val dailyReminderEnabled: Boolean = true,
    val dailyReminderTimeMinutes: Int = 19 * 60, // 7:00 PM
    val timerCompletionSound: Boolean = true,
    val timerToneUri: String = "",
    val timerToneLabel: String = "Default Alarm",
    val reminderToneUri: String = "",
    val reminderToneLabel: String = "Default Notification",
    val notificationToneUri: String = "",
    val notificationToneLabel: String = "Default Notification",
    val weekStartsOn: String = "Monday",
    val isDarkTheme: Boolean = false
)

/**
 * Lightweight local preferences store for the Me / Study Preferences, Sound Tones, and active focus timer snapshot.
 */
class UserPreferencesManager(context: Context) {
    private val prefs = context.getSharedPreferences("edutrack_prefs", Context.MODE_PRIVATE)
    private val timerSnapshotPrefs = context.getSharedPreferences("edutrack_active_timer", Context.MODE_PRIVATE)

    private val defaultAlarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)?.toString().orEmpty()
    private val defaultNotifUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)?.toString().orEmpty()

    private val _state = MutableStateFlow(
        UserPreferencesState(
            dailyReminderEnabled = prefs.getBoolean(KEY_DAILY_REMINDER, true),
            dailyReminderTimeMinutes = prefs.getInt(KEY_REMINDER_TIME_MIN, 19 * 60),
            timerCompletionSound = prefs.getBoolean(KEY_COMPLETION_SOUND, true),
            timerToneUri = prefs.getString(KEY_TIMER_TONE_URI, defaultAlarmUri) ?: defaultAlarmUri,
            timerToneLabel = prefs.getString(KEY_TIMER_TONE_LABEL, "Default Alarm") ?: "Default Alarm",
            reminderToneUri = prefs.getString(KEY_REMINDER_TONE_URI, defaultNotifUri) ?: defaultNotifUri,
            reminderToneLabel = prefs.getString(KEY_REMINDER_TONE_LABEL, "Default Notification") ?: "Default Notification",
            notificationToneUri = prefs.getString(KEY_NOTIF_TONE_URI, defaultNotifUri) ?: defaultNotifUri,
            notificationToneLabel = prefs.getString(KEY_NOTIF_TONE_LABEL, "Default Notification") ?: "Default Notification",
            weekStartsOn = prefs.getString(KEY_WEEK_START, "Monday") ?: "Monday",
            isDarkTheme = prefs.getBoolean(KEY_DARK_THEME, false)
        )
    )
    val state: StateFlow<UserPreferencesState> = _state.asStateFlow()

    fun setDailyReminderEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DAILY_REMINDER, enabled).apply()
        _state.update { it.copy(dailyReminderEnabled = enabled) }
    }

    fun setDailyReminderTimeMinutes(minutesFromMidnight: Int) {
        prefs.edit().putInt(KEY_REMINDER_TIME_MIN, minutesFromMidnight).apply()
        _state.update { it.copy(dailyReminderTimeMinutes = minutesFromMidnight) }
    }

    fun setTimerCompletionSound(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_COMPLETION_SOUND, enabled).apply()
        _state.update { it.copy(timerCompletionSound = enabled) }
    }

    fun setTimerTone(uriString: String, label: String) {
        prefs.edit()
            .putString(KEY_TIMER_TONE_URI, uriString)
            .putString(KEY_TIMER_TONE_LABEL, label)
            .apply()
        _state.update { it.copy(timerToneUri = uriString, timerToneLabel = label) }
    }

    fun setReminderTone(uriString: String, label: String) {
        prefs.edit()
            .putString(KEY_REMINDER_TONE_URI, uriString)
            .putString(KEY_REMINDER_TONE_LABEL, label)
            .apply()
        _state.update { it.copy(reminderToneUri = uriString, reminderToneLabel = label) }
    }

    fun setNotificationTone(uriString: String, label: String) {
        prefs.edit()
            .putString(KEY_NOTIF_TONE_URI, uriString)
            .putString(KEY_NOTIF_TONE_LABEL, label)
            .apply()
        _state.update { it.copy(notificationToneUri = uriString, notificationToneLabel = label) }
    }

    fun setWeekStartsOn(day: String) {
        prefs.edit().putString(KEY_WEEK_START, day).apply()
        _state.update { it.copy(weekStartsOn = day) }
    }

    fun setDarkTheme(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_THEME, enabled).apply()
        _state.update { it.copy(isDarkTheme = enabled) }
    }

    fun saveActiveTimerSnapshot(snapshot: ActiveTimerSnapshot) {
        timerSnapshotPrefs.edit()
            .putString(KEY_TIMER_STATUS, snapshot.status)
            .putLong(KEY_TIMER_SUBJECT_ID, snapshot.subjectId)
            .putString(KEY_TIMER_SUBJECT_NAME, snapshot.subjectName)
            .putInt(KEY_TIMER_TARGET_MINUTES, snapshot.targetMinutes)
            .putLong(KEY_TIMER_END_EPOCH_MILLIS, snapshot.endEpochMillis)
            .putInt(KEY_TIMER_PAUSED_REMAINING_SECONDS, snapshot.pausedRemainingSeconds)
            .putBoolean(KEY_TIMER_IS_BAR_HIDDEN, snapshot.isBarHidden)
            .apply()
    }

    fun readActiveTimerSnapshot(): ActiveTimerSnapshot? {
        val status = timerSnapshotPrefs.getString(KEY_TIMER_STATUS, null) ?: return null
        val subjectId = timerSnapshotPrefs.getLong(KEY_TIMER_SUBJECT_ID, -1L)
        val subjectName = timerSnapshotPrefs.getString(KEY_TIMER_SUBJECT_NAME, null) ?: return null
        val targetMinutes = timerSnapshotPrefs.getInt(KEY_TIMER_TARGET_MINUTES, 0)
        if (subjectId <= 0L || targetMinutes <= 0) return null
        return ActiveTimerSnapshot(
            status = status,
            subjectId = subjectId,
            subjectName = subjectName,
            targetMinutes = targetMinutes,
            endEpochMillis = timerSnapshotPrefs.getLong(KEY_TIMER_END_EPOCH_MILLIS, 0L),
            pausedRemainingSeconds = timerSnapshotPrefs.getInt(KEY_TIMER_PAUSED_REMAINING_SECONDS, 0),
            isBarHidden = timerSnapshotPrefs.getBoolean(KEY_TIMER_IS_BAR_HIDDEN, false)
        )
    }

    fun clearActiveTimerSnapshot() {
        timerSnapshotPrefs.edit().clear().apply()
    }

    private companion object {
        const val KEY_DAILY_REMINDER = "daily_reminder_enabled"
        const val KEY_REMINDER_TIME_MIN = "daily_reminder_time_min"
        const val KEY_COMPLETION_SOUND = "timer_completion_sound"
        const val KEY_TIMER_TONE_URI = "timer_tone_uri"
        const val KEY_TIMER_TONE_LABEL = "timer_tone_label"
        const val KEY_REMINDER_TONE_URI = "reminder_tone_uri"
        const val KEY_REMINDER_TONE_LABEL = "reminder_tone_label"
        const val KEY_NOTIF_TONE_URI = "notif_tone_uri"
        const val KEY_NOTIF_TONE_LABEL = "notif_tone_label"
        const val KEY_WEEK_START = "week_starts_on"
        const val KEY_DARK_THEME = "dark_theme"

        const val KEY_TIMER_STATUS = "timer_status"
        const val KEY_TIMER_SUBJECT_ID = "timer_subject_id"
        const val KEY_TIMER_SUBJECT_NAME = "timer_subject_name"
        const val KEY_TIMER_TARGET_MINUTES = "timer_target_minutes"
        const val KEY_TIMER_END_EPOCH_MILLIS = "timer_end_epoch_millis"
        const val KEY_TIMER_PAUSED_REMAINING_SECONDS = "timer_paused_remaining_seconds"
        const val KEY_TIMER_IS_BAR_HIDDEN = "timer_is_bar_hidden"
    }
}
