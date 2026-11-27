package com.example.edutrack.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.edutrack.EduTrackApplication
import com.example.edutrack.data.local.entity.NotificationEntity
import com.example.edutrack.data.local.entity.NotificationType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Receives scheduled [AlarmManager] triggers and notification action buttons
 * even when the app is backgrounded or the screen is off.
 */
class AlarmReceiver : BroadcastReceiver() {

    private val exceptionHandler = CoroutineExceptionHandler { _, _ -> }

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? EduTrackApplication ?: return
        val container = app.container
        val appContext = context.applicationContext

        when (intent.action) {
            ACTION_FOCUS_COMPLETE -> {
                container.focusTimerManager.completeNowIfDue(forceFromAlarm = true)
            }

            ACTION_STOP_FOCUS_ALARM -> {
                container.focusTimerManager.stopAlarm()
                NotificationHelper.cancelFocusCompleteNotification(appContext)
            }

            ACTION_TOGGLE_FOCUS_PAUSE -> {
                container.focusTimerManager.togglePauseResume()
            }

            ACTION_DAILY_STUDY_REMINDER -> {
                val pendingResult = goAsync()
                CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler).launch {
                    try {
                        val prefs = container.userPreferences.state.value
                        if (!prefs.dailyReminderEnabled) {
                            ReminderAlarmScheduler.scheduleDailyStudyReminder(
                                context = appContext,
                                enabled = false,
                                timeMinutes = prefs.dailyReminderTimeMinutes
                            )
                            return@launch
                        }
                        NotificationHelper.showReminderNotification(
                            context = appContext,
                            notificationId = NotificationHelper.NOTIF_ID_DAILY_NUDGE,
                            title = "Time to study",
                            body = "Open EduTrack to check today's subjects and start a focus session.",
                            toneUriString = prefs.notificationToneUri
                        )
                        container.repository.insertNotification(
                            NotificationEntity(
                                title = "Daily study reminder",
                                message = "Ready for today's study plan.",
                                type = NotificationType.REMINDER.name
                            )
                        )
                        ReminderAlarmScheduler.scheduleDailyStudyReminder(
                            context = appContext,
                            enabled = true,
                            timeMinutes = prefs.dailyReminderTimeMinutes
                        )
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        // Guard broadcast receiver against transient database or system errors.
                    } finally {
                        runCatching { pendingResult.finish() }
                    }
                }
            }

            ACTION_REMINDER_ALARM -> {
                val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
                val pendingResult = goAsync()

                CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler).launch {
                    try {
                        if (reminderId <= 0L) return@launch
                        val reminder = container.repository.getReminderById(reminderId) ?: return@launch
                        val prefs = container.userPreferences.state.value

                        val title = reminder.title
                        val body = reminder.notes.ifBlank { "Scheduled reminder" }
                        val notifId = (2000 + (reminderId % 10000)).toInt()

                        if (!reminder.isCompleted && reminder.notifyEnabled) {
                            NotificationHelper.showReminderNotification(
                                context = appContext,
                                notificationId = notifId,
                                title = title,
                                body = body,
                                toneUriString = prefs.reminderToneUri
                            )
                            container.repository.insertNotification(
                                NotificationEntity(
                                    title = "Reminder: $title",
                                    message = body,
                                    type = NotificationType.REMINDER.name
                                )
                            )
                        }

                        if (!reminder.isCompleted) {
                            val nextEpoch = ReminderAlarmScheduler.computeNextFutureOccurrenceEpochDay(
                                reminder = reminder,
                                nowMillis = System.currentTimeMillis()
                            )
                            if (nextEpoch != null) {
                                val updated = reminder.copy(dueDateEpochDay = nextEpoch)
                                container.repository.updateReminder(updated)
                                ReminderAlarmScheduler.scheduleReminder(appContext, updated)
                            }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        // Guard broadcast receiver against transient database or system errors.
                    } finally {
                        runCatching { pendingResult.finish() }
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_REMINDER_ALARM = "com.example.edutrack.ACTION_REMINDER_ALARM"
        const val ACTION_DAILY_STUDY_REMINDER = "com.example.edutrack.ACTION_DAILY_STUDY_REMINDER"
        const val ACTION_FOCUS_COMPLETE = "com.example.edutrack.ACTION_FOCUS_COMPLETE"
        const val ACTION_STOP_FOCUS_ALARM = "com.example.edutrack.ACTION_STOP_FOCUS_ALARM"
        const val ACTION_TOGGLE_FOCUS_PAUSE = "com.example.edutrack.ACTION_TOGGLE_FOCUS_PAUSE"

        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_BODY = "extra_body"
    }
}
