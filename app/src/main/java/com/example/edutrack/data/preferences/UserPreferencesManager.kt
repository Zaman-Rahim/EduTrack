package com.example.edutrack.data.preferences

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class AlarmToneOption(val label: String) {
    CLASSIC_ALARM("Classic Alarm"),
    GENTLE_CHIME("Gentle Chime"),
    BRIGHT_BELL("Bright Bell"),
    DIGITAL_BEEP("Digital Beep");

    companion object {
        fun fromName(name: String?): AlarmToneOption {
            return entries.firstOrNull { it.name == name } ?: CLASSIC_ALARM
        }
    }
}

data class UserPreferencesState(
    val dailyReminderEnabled: Boolean = true,
    val dailyReminderTimeMinutes: Int = 19 * 60, // 7:00 PM
    val timerCompletionSound: Boolean = true,
    val alarmTone: String = AlarmToneOption.CLASSIC_ALARM.name,
    val weekStartsOn: String = "Monday"
)

/**
 * Lightweight local preferences store for the Me / Study Preferences screen.
 */
class UserPreferencesManager(context: Context) {
    private val prefs = context.getSharedPreferences("edutrack_prefs", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(
        UserPreferencesState(
            dailyReminderEnabled = prefs.getBoolean(KEY_DAILY_REMINDER, true),
            dailyReminderTimeMinutes = prefs.getInt(KEY_REMINDER_TIME_MIN, 19 * 60),
            timerCompletionSound = prefs.getBoolean(KEY_COMPLETION_SOUND, true),
            alarmTone = prefs.getString(KEY_ALARM_TONE, AlarmToneOption.CLASSIC_ALARM.name)
                ?: AlarmToneOption.CLASSIC_ALARM.name,
            weekStartsOn = prefs.getString(KEY_WEEK_START, "Monday") ?: "Monday"
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

    fun setAlarmTone(toneName: String) {
        prefs.edit().putString(KEY_ALARM_TONE, toneName).apply()
        _state.update { it.copy(alarmTone = toneName) }
    }

    fun setWeekStartsOn(day: String) {
        prefs.edit().putString(KEY_WEEK_START, day).apply()
        _state.update { it.copy(weekStartsOn = day) }
    }

    private companion object {
        const val KEY_DAILY_REMINDER = "daily_reminder_enabled"
        const val KEY_REMINDER_TIME_MIN = "daily_reminder_time_min"
        const val KEY_COMPLETION_SOUND = "timer_completion_sound"
        const val KEY_ALARM_TONE = "alarm_tone"
        const val KEY_WEEK_START = "week_starts_on"
    }
}
