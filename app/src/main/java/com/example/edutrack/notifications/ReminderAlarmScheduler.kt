package com.example.edutrack.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.edutrack.data.local.entity.ReminderEntity
import com.example.edutrack.data.local.entity.ReminderRepeatType
import com.example.edutrack.data.preferences.UserPreferencesManager
import com.example.edutrack.data.repository.EduTrackRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Schedules wake-up [AlarmManager] triggers for Reminders, Daily Study nudges,
 * and Focus Timer completions so notifications & alarms fire even when the app is
 * backgrounded or the screen is off.
 */
object ReminderAlarmScheduler {

    private const val REQUEST_DAILY_STUDY = 50_000
    private const val REQUEST_FOCUS_COMPLETE = 60_000
    private const val MAX_REPEAT_ADVANCE_STEPS = 10_000
    private val rescheduleMutex = Mutex()

    fun scheduleReminder(context: Context, reminder: ReminderEntity) {
        if (reminder.id <= 0L) return
        if (!reminder.notifyEnabled || reminder.isCompleted) {
            cancelReminder(context, reminder.id)
            return
        }

        val triggerMillis = computeTriggerMillis(reminder.dueDateEpochDay, reminder.dueTimeMinutes)
        if (triggerMillis <= System.currentTimeMillis()) {
            return
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_REMINDER_ALARM
            putExtra(AlarmReceiver.EXTRA_REMINDER_ID, reminder.id)
            putExtra(AlarmReceiver.EXTRA_TITLE, reminder.title)
            putExtra(AlarmReceiver.EXTRA_BODY, reminder.notes.ifBlank { "Scheduled reminder" })
        }

        val pending = PendingIntent.getBroadcast(
            context,
            reminderRequestCode(reminder.id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleAlarmCompat(context, triggerMillis, pending)
    }

    fun cancelReminder(context: Context, reminderId: Long) {
        if (reminderId <= 0L) return
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_REMINDER_ALARM
        }
        val pending = PendingIntent.getBroadcast(
            context,
            reminderRequestCode(reminderId),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pending)
        pending.cancel()
    }

    fun computeNextOccurrenceEpochDay(reminder: ReminderEntity): Long? {
        val type = runCatching { ReminderRepeatType.valueOf(reminder.repeatType) }
            .getOrDefault(ReminderRepeatType.NONE)
        val current = LocalDate.ofEpochDay(reminder.dueDateEpochDay)
        return when (type) {
            ReminderRepeatType.NONE -> null
            ReminderRepeatType.DAILY -> current.plusDays(1).toEpochDay()
            ReminderRepeatType.WEEKLY -> current.plusWeeks(1).toEpochDay()
            ReminderRepeatType.MONTHLY -> current.plusMonths(1).toEpochDay()
            ReminderRepeatType.WEEKDAYS -> {
                var next = current.plusDays(1)
                while (next.dayOfWeek.value > 5) {
                    next = next.plusDays(1)
                }
                next.toEpochDay()
            }
            ReminderRepeatType.CUSTOM -> {
                for (offset in 1..7) {
                    val candidate = current.plusDays(offset.toLong())
                    val bit = 1 shl (candidate.dayOfWeek.value - 1)
                    if ((reminder.repeatDaysMask and bit) != 0) {
                        return candidate.toEpochDay()
                    }
                }
                null
            }
        }
    }

    /**
     * Advances a repeating reminder forward across any missed occurrences until its trigger
     * timestamp is strictly after [nowMillis], or returns null if the reminder does not repeat.
     */
    fun computeNextFutureOccurrenceEpochDay(
        reminder: ReminderEntity,
        nowMillis: Long = System.currentTimeMillis()
    ): Long? {
        var candidateEpochDay = computeNextOccurrenceEpochDay(reminder) ?: return null
        var steps = 0
        while (
            computeTriggerMillis(candidateEpochDay, reminder.dueTimeMinutes) <= nowMillis &&
            steps < MAX_REPEAT_ADVANCE_STEPS
        ) {
            val next = computeNextOccurrenceEpochDay(reminder.copy(dueDateEpochDay = candidateEpochDay))
                ?: return null
            if (next <= candidateEpochDay) return null
            candidateEpochDay = next
            steps++
        }
        return if (computeTriggerMillis(candidateEpochDay, reminder.dueTimeMinutes) > nowMillis) {
            candidateEpochDay
        } else {
            null
        }
    }

    /**
     * Reschedules the daily study nudge and all non-completed reminders, advancing any past-due
     * repeating reminders forward so missed schedules never stay stuck in the past.
     */
    suspend fun rescheduleAll(
        context: Context,
        repository: EduTrackRepository,
        preferencesManager: UserPreferencesManager
    ) = rescheduleMutex.withLock {
        val prefs = preferencesManager.state.value
        scheduleDailyStudyReminder(
            context = context,
            enabled = prefs.dailyReminderEnabled,
            timeMinutes = prefs.dailyReminderTimeMinutes
        )

        val nowMillis = System.currentTimeMillis()
        val reminders = repository.observeAllReminders().first()
        for (reminder in reminders) {
            if (reminder.isCompleted || !reminder.notifyEnabled) {
                cancelReminder(context, reminder.id)
                continue
            }
            val triggerMillis = computeTriggerMillis(reminder.dueDateEpochDay, reminder.dueTimeMinutes)
            if (triggerMillis > nowMillis) {
                scheduleReminder(context, reminder)
            } else {
                val nextEpochDay = computeNextFutureOccurrenceEpochDay(reminder, nowMillis)
                if (nextEpochDay != null) {
                    val updated = reminder.copy(dueDateEpochDay = nextEpochDay)
                    repository.updateReminder(updated)
                    scheduleReminder(context, updated)
                }
            }
        }
    }

    fun scheduleDailyStudyReminder(context: Context, enabled: Boolean, timeMinutes: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_DAILY_STUDY_REMINDER
        }
        if (!enabled) {
            PendingIntent.getBroadcast(
                context,
                REQUEST_DAILY_STUDY,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )?.let {
                alarmManager.cancel(it)
                it.cancel()
            }
            return
        }

        val now = ZonedDateTime.now()
        val safeMins = timeMinutes.coerceIn(0, 1439)
        var target = now.toLocalDate()
            .atTime(LocalTime.of(safeMins / 60, safeMins % 60))
            .atZone(ZoneId.systemDefault())
        if (!target.isAfter(now)) {
            target = target.plusDays(1)
        }

        val pending = PendingIntent.getBroadcast(
            context,
            REQUEST_DAILY_STUDY,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        scheduleAlarmCompat(context, target.toInstant().toEpochMilli(), pending)
    }

    fun scheduleFocusCompletion(context: Context, endEpochMillis: Long, subjectName: String) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_FOCUS_COMPLETE
            putExtra(AlarmReceiver.EXTRA_TITLE, subjectName)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            REQUEST_FOCUS_COMPLETE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        scheduleAlarmCompat(context, endEpochMillis, pending)
    }

    fun cancelFocusCompletion(context: Context) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_FOCUS_COMPLETE
        }
        val pending = PendingIntent.getBroadcast(
            context,
            REQUEST_FOCUS_COMPLETE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pending)
        pending.cancel()
    }

    private fun scheduleAlarmCompat(context: Context, triggerAtMillis: Long, pending: PendingIntent) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
            }
        }.onFailure {
            runCatching {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
            }
        }
    }

    private fun computeTriggerMillis(epochDay: Long, timeMinutes: Int): Long {
        val safeMins = timeMinutes.coerceIn(0, 1439)
        val date = LocalDate.ofEpochDay(epochDay)
        val time = LocalTime.of(safeMins / 60, safeMins % 60)
        return date.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun reminderRequestCode(reminderId: Long): Int =
        (10_000 + (reminderId % 30_000)).toInt()
}
