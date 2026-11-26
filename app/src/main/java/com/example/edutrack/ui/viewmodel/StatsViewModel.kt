package com.example.edutrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.edutrack.data.local.entity.SubjectEntity
import com.example.edutrack.data.repository.EduTrackRepository
import com.example.edutrack.util.DateTimeUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Single bar item for the 7-day study bar chart.
 */
data class DailyStudyBar(
    val epochDay: Long,
    val dayLabel: String,
    val minutes: Int
) {
    val hours: Float get() = minutes / 60f
}

/**
 * Per-subject breakdown displayed on the Progress / Stats screen.
 */
data class SubjectStatsItem(
    val subject: SubjectEntity,
    val weeklyMinutes: Int,
    val allTimeMinutes: Int,
    val sessionCount: Int
) {
    val weeklyHours: Double get() = weeklyMinutes / 60.0
    val goalProgress: Float
        get() = if (subject.weeklyGoalHours <= 0.0) {
            0f
        } else {
            (weeklyHours / subject.weeklyGoalHours).toFloat().coerceIn(0f, 1f)
        }
}

/**
 * Immutable UI state for [com.example.edutrack.ui.screens.stats.StatsScreen].
 */
data class StatsUiState(
    val totalAllTimeMinutes: Int = 0,
    val totalWeeklyMinutes: Int = 0,
    val totalSessionsCount: Int = 0,
    val currentStreakDays: Int = 0,
    val longestStreakDays: Int = 0,
    val dailyBars: List<DailyStudyBar> = emptyList(),
    val subjectStats: List<SubjectStatsItem> = emptyList(),
    val isLoading: Boolean = true
)

/**
 * ViewModel computing study streaks, weekly daily totals, and subject progress breakdown.
 */
class StatsViewModel(repository: EduTrackRepository) : ViewModel() {

    private val todayEpochDay = DateTimeUtils.todayEpochDay()
    private val weekBounds = DateTimeUtils.currentWeekBounds()

    val uiState: StateFlow<StatsUiState> = combine(
        repository.observeSubjectsWithSessions(),
        repository.observeAllSessionsWithSubject()
    ) { subjectsWithSessions, allSessions ->
        val totalAllTimeMinutes = allSessions.sumOf { it.session.durationMinutes }
        val totalWeeklyMinutes = allSessions
            .filter { it.session.sessionDateEpochDay in weekBounds.first..weekBounds.second }
            .sumOf { it.session.durationMinutes }

        val (currentStreak, longestStreak) = DateTimeUtils.calculateStreaks(
            sessionEpochDays = allSessions.map { it.session.sessionDateEpochDay },
            todayEpochDay = todayEpochDay
        )

        val minutesByDay = allSessions
            .groupBy { it.session.sessionDateEpochDay }
            .mapValues { (_, list) -> list.sumOf { it.session.durationMinutes } }

        val dailyBars = (6 downTo 0).map { offset ->
            val day = todayEpochDay - offset
            DailyStudyBar(
                epochDay = day,
                dayLabel = DateTimeUtils.formatDayLabel(day),
                minutes = minutesByDay[day] ?: 0
            )
        }

        val subjectStats = subjectsWithSessions.map { item ->
            val weeklyMins = item.sessions
                .filter { it.sessionDateEpochDay in weekBounds.first..weekBounds.second }
                .sumOf { it.durationMinutes }
            val allMins = item.sessions.sumOf { it.durationMinutes }
            SubjectStatsItem(
                subject = item.subject,
                weeklyMinutes = weeklyMins,
                allTimeMinutes = allMins,
                sessionCount = item.sessions.size
            )
        }.sortedByDescending { it.allTimeMinutes }

        StatsUiState(
            totalAllTimeMinutes = totalAllTimeMinutes,
            totalWeeklyMinutes = totalWeeklyMinutes,
            totalSessionsCount = allSessions.size,
            currentStreakDays = currentStreak,
            longestStreakDays = longestStreak,
            dailyBars = dailyBars,
            subjectStats = subjectStats,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StatsUiState()
    )
}
