package com.example.edutrack.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.edutrack.EduTrackApplication
import com.example.edutrack.notifications.NotificationHelper
import com.example.edutrack.ui.timer.FocusTimerUiState
import com.example.edutrack.ui.timer.TimerStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Foreground service keeping the active Focus Timer alive when the user leaves the app
 * or turns off the screen, displaying a silent ongoing notification with Pause/Resume.
 */
class FocusTimerService : Service() {

    private data class NotificationKey(
        val status: TimerStatus,
        val subjectName: String,
        val endEpochMillis: Long,
        val pausedRemainingSeconds: Int
    )

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observerJob: Job? = null
    private var lastNotificationKey: NotificationKey? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = application as? EduTrackApplication ?: run {
            stopSelf()
            return START_NOT_STICKY
        }
        val state = app.container.focusTimerManager.state.value
        if (state.status != TimerStatus.RUNNING && state.status != TimerStatus.PAUSED) {
            lastNotificationKey = null
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val subjectName = state.subject?.name ?: "Study Focus"
        val notification = NotificationHelper.buildOngoingFocusNotification(
            context = this,
            subjectName = subjectName,
            status = state.status,
            remainingSeconds = state.remainingSeconds,
            endEpochMillis = state.endEpochMillis
        )
        lastNotificationKey = keyFor(state)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NotificationHelper.NOTIF_ID_FOCUS_ONGOING,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NotificationHelper.NOTIF_ID_FOCUS_ONGOING, notification)
        }

        if (observerJob == null) {
            observerJob = serviceScope.launch {
                app.container.focusTimerManager.state.collectLatest { current ->
                    if (current.status == TimerStatus.RUNNING || current.status == TimerStatus.PAUSED) {
                        val nextKey = keyFor(current)
                        if (nextKey != lastNotificationKey) {
                            lastNotificationKey = nextKey
                            val updatedNotif = NotificationHelper.buildOngoingFocusNotification(
                                context = this@FocusTimerService,
                                subjectName = current.subject?.name ?: "Study Focus",
                                status = current.status,
                                remainingSeconds = current.remainingSeconds,
                                endEpochMillis = current.endEpochMillis
                            )
                            if (NotificationHelper.hasNotificationPermission(this@FocusTimerService)) {
                                NotificationManagerCompat.from(this@FocusTimerService)
                                    .notify(NotificationHelper.NOTIF_ID_FOCUS_ONGOING, updatedNotif)
                            }
                        }
                    } else {
                        lastNotificationKey = null
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                }
            }
        }

        return START_STICKY
    }

    private fun keyFor(state: FocusTimerUiState): NotificationKey =
        NotificationKey(
            status = state.status,
            subjectName = state.subject?.name ?: "Study Focus",
            endEpochMillis = if (state.status == TimerStatus.RUNNING) state.endEpochMillis else 0L,
            pausedRemainingSeconds = if (state.status == TimerStatus.PAUSED) state.remainingSeconds else 0
        )

    override fun onDestroy() {
        observerJob?.cancel()
        observerJob = null
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, FocusTimerService::class.java)
            runCatching {
                ContextCompat.startForegroundService(context, intent)
            }
        }

        fun stop(context: Context) {
            runCatching {
                context.stopService(Intent(context, FocusTimerService::class.java))
            }
        }
    }
}
