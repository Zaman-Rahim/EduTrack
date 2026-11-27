package com.example.edutrack.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.edutrack.data.preferences.UserPreferencesManager
import com.example.edutrack.data.repository.EduTrackRepository

/**
 * Factory creating EduTrack ViewModels with the shared [EduTrackRepository] and preferences.
 */
class AppViewModelFactory(
    private val repository: EduTrackRepository,
    private val preferencesManager: UserPreferencesManager? = null,
    private val sessionId: Long = -1L,
    private val initialSubjectId: Long = -1L,
    private val appContext: Context? = null
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) ->
                HomeViewModel(repository, preferencesManager) as T
            modelClass.isAssignableFrom(AddEditSessionViewModel::class.java) ->
                AddEditSessionViewModel(repository, sessionId, initialSubjectId) as T
            modelClass.isAssignableFrom(HistoryViewModel::class.java) ->
                HistoryViewModel(repository, preferencesManager) as T
            modelClass.isAssignableFrom(SessionDetailViewModel::class.java) ->
                SessionDetailViewModel(repository, sessionId) as T
            modelClass.isAssignableFrom(SubjectManagerViewModel::class.java) ->
                SubjectManagerViewModel(repository) as T
            modelClass.isAssignableFrom(StatsViewModel::class.java) ->
                StatsViewModel(repository, preferencesManager) as T
            modelClass.isAssignableFrom(RemindersViewModel::class.java) ->
                RemindersViewModel(repository, appContext) as T
            modelClass.isAssignableFrom(NotificationsViewModel::class.java) ->
                NotificationsViewModel(repository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
