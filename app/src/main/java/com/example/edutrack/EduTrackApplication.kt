package com.example.edutrack

import android.app.Application
import com.example.edutrack.data.local.EduTrackDatabase
import com.example.edutrack.data.preferences.UserPreferencesManager
import com.example.edutrack.data.repository.EduTrackRepository
import com.example.edutrack.data.repository.OfflineEduTrackRepository
import com.example.edutrack.notifications.NotificationHelper
import com.example.edutrack.notifications.ReminderAlarmScheduler
import com.example.edutrack.ui.timer.FocusTimerManager
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Manual dependency container scoped to the application lifecycle.
 */
class AppContainer(application: Application) {
    private val database: EduTrackDatabase by lazy {
        EduTrackDatabase.getInstance(application)
    }

    val userPreferences: UserPreferencesManager by lazy {
        UserPreferencesManager(application)
    }

    val repository: EduTrackRepository by lazy {
        OfflineEduTrackRepository(
            subjectDao = database.subjectDao(),
            studySessionDao = database.studySessionDao(),
            reminderDao = database.reminderDao(),
            notificationDao = database.notificationDao()
        )
    }

    val focusTimerManager: FocusTimerManager by lazy {
        FocusTimerManager(
            context = application,
            repository = repository,
            preferencesManager = userPreferences
        )
    }
}

/**
 * Custom [Application] class providing the shared [AppContainer].
 */
class EduTrackApplication : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, _ -> }
    )

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.ensureChannels(this)

        if (container.userPreferences.readActiveTimerSnapshot() != null) {
            container.focusTimerManager
        }

        appScope.launch {
            ReminderAlarmScheduler.rescheduleAll(
                context = this@EduTrackApplication,
                repository = container.repository,
                preferencesManager = container.userPreferences
            )
        }
    }
}
