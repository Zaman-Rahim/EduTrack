package com.example.edutrack.notifications

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.edutrack.EduTrackApplication
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Re-schedules active Reminders, the Daily Study Reminder, and any running Focus Timer alarm
 * after device reboot, app update, clock/timezone changes, or exact-alarm permission grants.
 */
class BootReceiver : BroadcastReceiver() {

    private val exceptionHandler = CoroutineExceptionHandler { _, _ -> }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in HANDLED_ACTIONS) return

        val app = context.applicationContext as? EduTrackApplication ?: return
        val container = app.container
        val appContext = context.applicationContext

        if (container.userPreferences.readActiveTimerSnapshot() != null) {
            container.focusTimerManager.rescheduleAlarmIfRunning()
        }

        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler).launch {
            try {
                ReminderAlarmScheduler.rescheduleAll(
                    context = appContext,
                    repository = container.repository,
                    preferencesManager = container.userPreferences
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Guard broadcast receiver against transient database or alarm errors.
            } finally {
                runCatching { pending.finish() }
            }
        }
    }

    private companion object {
        const val ACTION_QUICKBOOT_POWERON = "android.intent.action.QUICKBOOT_POWERON"
        const val ACTION_HTC_QUICKBOOT_POWERON = "com.htc.intent.action.QUICKBOOT_POWERON"

        val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            ACTION_QUICKBOOT_POWERON,
            ACTION_HTC_QUICKBOOT_POWERON,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
        )
    }
}
