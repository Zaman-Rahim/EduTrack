package com.example.edutrack

import android.app.Application
import com.example.edutrack.data.local.EduTrackDatabase
import com.example.edutrack.data.preferences.UserPreferencesManager
import com.example.edutrack.data.repository.EduTrackRepository
import com.example.edutrack.data.repository.OfflineEduTrackRepository
import com.example.edutrack.ui.timer.FocusTimerManager

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

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
