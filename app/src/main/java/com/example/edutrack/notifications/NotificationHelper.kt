package com.example.edutrack.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.edutrack.MainActivity
import com.example.edutrack.R
import com.example.edutrack.ui.timer.TimerStatus
import com.example.edutrack.util.DateTimeUtils
import com.example.edutrack.util.SystemSoundHelper
import java.util.Locale

/**
 * Centralized Android system notification channels and notification builders for
 * ongoing focus countdowns, focus completion alarms, and scheduled reminders.
 */
object NotificationHelper {
    const val CHANNEL_FOCUS_ONGOING = "edutrack_focus_ongoing"
    const val CHANNEL_FOCUS_COMPLETE = "edutrack_focus_complete"
    const val CHANNEL_REMINDERS = "edutrack_reminders"

    const val NOTIF_ID_FOCUS_ONGOING = 1001
    const val NOTIF_ID_FOCUS_COMPLETE = 1002
    const val NOTIF_ID_DAILY_NUDGE = 1003

    const val EXTRA_OPEN_TARGET = "extra_open_target"
    const val OPEN_TARGET_FOCUS_TIMER = "focus_timer"
    const val OPEN_TARGET_REMINDERS = "reminders"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val ongoing = NotificationChannel(
            CHANNEL_FOCUS_ONGOING,
            "Active Focus Timer",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows the live countdown while you focus or when your screen is off."
            setShowBadge(false)
        }

        // Sound is played with the user's chosen URI via RingtoneManager so changing tones in Me takes effect immediately.
        val focusComplete = NotificationChannel(
            CHANNEL_FOCUS_COMPLETE,
            "Focus Complete Alarm",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts you when a study focus countdown reaches 00:00."
            setSound(null, null)
            enableVibration(true)
        }

        val reminders = NotificationChannel(
            CHANNEL_REMINDERS,
            "Study & Task Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Scheduled reminder alerts and daily study nudges."
            setSound(null, null)
            enableVibration(true)
        }

        manager.createNotificationChannel(ongoing)
        manager.createNotificationChannel(focusComplete)
        manager.createNotificationChannel(reminders)
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun buildOngoingFocusNotification(
        context: Context,
        subjectName: String,
        status: TimerStatus,
        remainingSeconds: Int,
        endEpochMillis: Long
    ): Notification {
        val isPaused = status == TimerStatus.PAUSED

        val openIntent = PendingIntent.getActivity(
            context,
            10,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_OPEN_TARGET, OPEN_TARGET_FOCUS_TIMER)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleIntent = PendingIntent.getBroadcast(
            context,
            11,
            Intent(context, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_TOGGLE_FOCUS_PAUSE
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_FOCUS_ONGOING)
            .setSmallIcon(R.drawable.ic_figma_clock)
            .setContentTitle("Focusing: $subjectName")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setContentIntent(openIntent)
            .addAction(
                if (isPaused) R.drawable.ic_figma_play else R.drawable.ic_figma_pause,
                if (isPaused) "Resume" else "Pause",
                toggleIntent
            )

        if (status == TimerStatus.RUNNING && endEpochMillis > 0L) {
            builder
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setWhen(endEpochMillis)
                .setShowWhen(true)
                .setContentText("Focus timer running")
        } else {
            val mins = remainingSeconds.coerceAtLeast(0) / 60
            val secs = remainingSeconds.coerceAtLeast(0) % 60
            val timeText = String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
            val statusText = if (isPaused) "Paused · $timeText remaining" else "$timeText remaining"
            builder
                .setUsesChronometer(false)
                .setShowWhen(false)
                .setContentText(statusText)
        }

        return builder.build()
    }

    fun showFocusCompleteNotification(
        context: Context,
        subjectName: String,
        durationMinutes: Int,
        alarmSoundEnabled: Boolean = true
    ) {
        ensureChannelIfMissing(context, CHANNEL_FOCUS_COMPLETE)
        if (!hasNotificationPermission(context)) return

        val openIntent = PendingIntent.getActivity(
            context,
            20,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_OPEN_TARGET, OPEN_TARGET_FOCUS_TIMER)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopAlarmIntent = PendingIntent.getBroadcast(
            context,
            21,
            Intent(context, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_STOP_FOCUS_ALARM
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedDuration = DateTimeUtils.formatDuration(durationMinutes)
        val builder = NotificationCompat.Builder(context, CHANNEL_FOCUS_COMPLETE)
            .setSmallIcon(R.drawable.ic_figma_check)
            .setContentTitle("Focus complete · $subjectName")
            .setContentText("Logged $formattedDuration to your study history.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(openIntent)
            .setDeleteIntent(stopAlarmIntent)

        if (alarmSoundEnabled) {
            builder.addAction(R.drawable.ic_figma_pause, "Stop alarm", stopAlarmIntent)
        }

        NotificationManagerCompat.from(context).notify(NOTIF_ID_FOCUS_COMPLETE, builder.build())
    }

    fun cancelFocusCompleteNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIF_ID_FOCUS_COMPLETE)
    }

    fun showReminderNotification(
        context: Context,
        notificationId: Int,
        title: String,
        body: String,
        toneUriString: String
    ) {
        if (!hasNotificationPermission(context)) return
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        ensureChannelIfMissing(context, CHANNEL_REMINDERS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = manager.getNotificationChannel(CHANNEL_REMINDERS)
            if (channel?.importance == NotificationManager.IMPORTANCE_NONE) return
        }

        playOneShotTone(context, toneUriString, useAlarmUsage = false)

        val openIntent = PendingIntent.getActivity(
            context,
            notificationId,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_OPEN_TARGET, OPEN_TARGET_REMINDERS)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_figma_reminder)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openIntent)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notif)
    }

    private fun ensureChannelIfMissing(context: Context, channelId: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(channelId) == null) {
            ensureChannels(context)
        }
    }

    private fun playOneShotTone(context: Context, uriString: String, useAlarmUsage: Boolean) {
        val fallback = if (useAlarmUsage) RingtoneManager.TYPE_ALARM else RingtoneManager.TYPE_NOTIFICATION
        val uri = SystemSoundHelper.resolveUri(uriString, fallback) ?: return
        runCatching {
            val ringtone = RingtoneManager.getRingtone(context.applicationContext, uri) ?: return
            ringtone.audioAttributes = AudioAttributes.Builder()
                .setUsage(if (useAlarmUsage) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ringtone.isLooping = false
            }
            ringtone.play()
        }
    }
}
