package com.example.edutrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.edutrack.data.repository.EduTrackRepository

/**
 * Factory creating EduTrack ViewModels with the shared [EduTrackRepository].
 */
class AppViewModelFactory(
    private val repository: EduTrackRepository,
    private val sessionId: Long = -1L,
    private val initialSubjectId: Long = -1L
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) ->
                HomeViewModel(repository) as T
            modelClass.isAssignableFrom(AddEditSessionViewModel::class.java) ->
                AddEditSessionViewModel(repository, sessionId, initialSubjectId) as T
            modelClass.isAssignableFrom(HistoryViewModel::class.java) ->
                HistoryViewModel(repository, initialSubjectId) as T
            modelClass.isAssignableFrom(SessionDetailViewModel::class.java) ->
                SessionDetailViewModel(repository, sessionId) as T
            modelClass.isAssignableFrom(SubjectManagerViewModel::class.java) ->
                SubjectManagerViewModel(repository) as T
            modelClass.isAssignableFrom(StatsViewModel::class.java) ->
                StatsViewModel(repository) as T
            modelClass.isAssignableFrom(RemindersViewModel::class.java) ->
                RemindersViewModel(repository) as T
            modelClass.isAssignableFrom(NotificationsViewModel::class.java) ->
                NotificationsViewModel(repository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
