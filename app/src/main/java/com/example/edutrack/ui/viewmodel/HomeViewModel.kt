package com.example.edutrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.edutrack.data.local.entity.SubjectEntity
import com.example.edutrack.data.local.relation.SessionWithSubject
import com.example.edutrack.data.repository.EduTrackRepository
import com.example.edutrack.util.DateTimeUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Aggregated subject summary displayed on the Home Dashboard.
 */
data class SubjectDashboardSummary(
    val subject: SubjectEntity,
    val minutesThisWeek: Int,
    val totalSessionCount: Int,
    val latestSessionDateEpochDay: Long?,
    val latestSessionNotes: String?,
    val latestSessionMinutes: Int?
) {
    val hoursThisWeek: Double get() = minutesThisWeek / 60.0
    val goalProgressFraction: Float
        get() = if (subject.weeklyGoalHours <= 0.0) {
            0f
        } else {
            (hoursThisWeek / subject.weeklyGoalHours).toFloat().coerceIn(0f, 1f)
        }
}

/**
 * Immutable UI state for [com.example.edutrack.ui.screens.home.HomeScreen].
 */
data class HomeUiState(
    val todayMinutes: Int = 0,
    val todaySessionCount: Int = 0,
    val weekMinutes: Int = 0,
    val totalWeeklyGoalHours: Double = 0.0,
    val currentStreakDays: Int = 0,
    val subjectSummaries: List<SubjectDashboardSummary> = emptyList(),
    val recentSessions: List<SessionWithSubject> = emptyList(),
    val isLoading: Boolean = true
) {
    val weeklyProgressFraction: Float
        get() = if (totalWeeklyGoalHours <= 0.0) {
            0f
        } else {
            ((weekMinutes / 60.0) / totalWeeklyGoalHours).toFloat().coerceIn(0f, 1f)
        }

    val weeklyPercentage: Int
        get() = (weeklyProgressFraction * 100).toInt()
}

/**
 * ViewModel powering the Home Dashboard with reactive Room streams.
 */
class HomeViewModel(repository: EduTrackRepository) : ViewModel() {

    private val todayEpochDay = DateTimeUtils.todayEpochDay()
    private val weekBounds = DateTimeUtils.currentWeekBounds()

    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeSubjectsWithSessions(),
        repository.observeAllSessionsWithSubject(),
        repository.observeTotalMinutesForDay(todayEpochDay),
        repository.observeTotalMinutesInRange(weekBounds.first, weekBounds.second)
    ) { subjectsWithSessions, allSessions, todayMinutes, weekMinutes ->

        val todaySessionsCount = allSessions.count {
            it.session.sessionDateEpochDay == todayEpochDay
        }

        val summaries = subjectsWithSessions.map { item ->
            val sortedSessions = item.sessions.sortedWith(
                compareByDescending<com.example.edutrack.data.local.entity.StudySessionEntity> {
                    it.sessionDateEpochDay
                }.thenByDescending { it.createdAt }
            )
            val minutesThisWeek = item.sessions
                .filter { it.sessionDateEpochDay in weekBounds.first..weekBounds.second }
                .sumOf { it.durationMinutes }
            val latest = sortedSessions.firstOrNull()

            SubjectDashboardSummary(
                subject = item.subject,
                minutesThisWeek = minutesThisWeek,
                totalSessionCount = item.sessions.size,
                latestSessionDateEpochDay = latest?.sessionDateEpochDay,
                latestSessionNotes = latest?.notes,
                latestSessionMinutes = latest?.durationMinutes
            )
        }

        val totalGoalHours = subjectsWithSessions.sumOf { it.subject.weeklyGoalHours }
        val (currentStreak, _) = DateTimeUtils.calculateStreaks(
            sessionEpochDays = allSessions.map { it.session.sessionDateEpochDay },
            todayEpochDay = todayEpochDay
        )

        HomeUiState(
            todayMinutes = todayMinutes,
            todaySessionCount = todaySessionsCount,
            weekMinutes = weekMinutes,
            totalWeeklyGoalHours = totalGoalHours,
            currentStreakDays = currentStreak,
            subjectSummaries = summaries,
            recentSessions = allSessions.take(4),
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )
}
