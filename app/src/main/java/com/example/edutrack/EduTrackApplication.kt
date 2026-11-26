package com.example.edutrack

import android.app.Application
import com.example.edutrack.data.local.EduTrackDatabase
import com.example.edutrack.data.repository.EduTrackRepository
import com.example.edutrack.data.repository.OfflineEduTrackRepository

/**
 * Simple manual dependency container scoped to the application lifecycle.
 */
class AppContainer(application: Application) {
    private val database: EduTrackDatabase by lazy {
        EduTrackDatabase.getInstance(application)
    }

    val repository: EduTrackRepository by lazy {
        OfflineEduTrackRepository(
            subjectDao = database.subjectDao(),
            studySessionDao = database.studySessionDao()
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
