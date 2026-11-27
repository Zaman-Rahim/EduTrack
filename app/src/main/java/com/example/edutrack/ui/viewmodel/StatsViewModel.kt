package com.example.edutrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.edutrack.data.local.entity.SubjectEntity
import com.example.edutrack.data.preferences.UserPreferencesManager
import com.example.edutrack.data.repository.EduTrackRepository
import com.example.edutrack.util.DateTimeUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import kotlin.math.roundToInt

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
    val weeklyGoalMinutes: Int = 0
) {
    val weeklyHours: Double get() = weeklyMinutes / 60.0
}

/**
 * Immutable UI state for [com.example.edutrack.ui.screens.stats.StatsScreen].
 */
data class StatsUiState(
    val totalAllTimeMinutes: Int = 0,
    val totalWeeklyMinutes: Int = 0,
    val totalWeeklyGoalMinutes: Int = 0,
    val totalSessionsCount: Int = 0,
    val currentStreakDays: Int = 0,
    val selectedWeekOffset: Int = 0,
    val selectedWeekRangeLabel: String = "",
    val canGoNextWeek: Boolean = false,
    val dailyBars: List<DailyStudyBar> = emptyList(),
    val subjectStats: List<SubjectStatsItem> = emptyList(),
    val isLoading: Boolean = true
)

/**
 * ViewModel computing study streaks, weekly daily totals, and subject progress breakdown.
 */
class StatsViewModel(
    repository: EduTrackRepository,
    preferencesManager: UserPreferencesManager? = null
) : ViewModel() {

    private val weekOffset = MutableStateFlow(0)

    private val todayDatePulseFlow: Flow<LocalDate> = flow {
        while (true) {
            emit(LocalDate.now())
            delay(60_000L)
        }
    }.distinctUntilChanged()

    private val weekStartsOnFlow: Flow<String> = preferencesManager?.state
        ?.map { it.weekStartsOn }
        ?.distinctUntilChanged()
        ?: flowOf("Monday")

    val uiState: StateFlow<StatsUiState> = combine(
        repository.observeSubjectsWithSessions(),
        repository.observeAllSessionsWithSubject(),
        weekOffset,
        weekStartsOnFlow,
        todayDatePulseFlow
    ) { subjectsWithSessions, allSessions, offset, weekStartsOn, todayDate ->
        val todayEpochDay = DateTimeUtils.todayEpochDay(todayDate)
        val safeOffset = offset.coerceAtMost(0)
        val weekBounds = DateTimeUtils.weekBoundsForOffset(
            weekOffset = safeOffset,
            weekStartsOn = weekStartsOn,
            today = todayDate
        )
        val selectedWeekRangeLabel =
            "${DateTimeUtils.formatShortDate(weekBounds.first)} – ${DateTimeUtils.formatShortDate(weekBounds.second)}"

        val totalAllTimeMinutes = allSessions.sumOf { it.session.durationMinutes }
        val totalWeeklyMinutes = allSessions
            .filter { it.session.sessionDateEpochDay in weekBounds.first..weekBounds.second }
            .sumOf { it.session.durationMinutes }

        val (currentStreak, _) = DateTimeUtils.calculateStreaks(
            sessionEpochDays = allSessions.map { it.session.sessionDateEpochDay },
            todayEpochDay = todayEpochDay
        )

        val minutesByDay = allSessions
            .groupBy { it.session.sessionDateEpochDay }
            .mapValues { (_, list) -> list.sumOf { it.session.durationMinutes } }

        val dailyBars = (0L..6L).map { dayOffset ->
            val day = weekBounds.first + dayOffset
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
            val goalMins = if (item.subject.weeklyGoalHours > 0.0 && item.subject.weeklyGoalHours.isFinite()) {
                (item.subject.weeklyGoalHours * 60.0).roundToInt().coerceAtLeast(0)
            } else {
                (item.subject.dailyGoalMinutes * item.subject.scheduledDaysPerWeekCount()).coerceAtLeast(0)
            }
            SubjectStatsItem(
                subject = item.subject,
                weeklyMinutes = weeklyMins,
                allTimeMinutes = allMins,
                weeklyGoalMinutes = goalMins
            )
        }.sortedByDescending { it.allTimeMinutes }

        val totalWeeklyGoalMinutes = subjectStats.sumOf { it.weeklyGoalMinutes }

        StatsUiState(
            totalAllTimeMinutes = totalAllTimeMinutes,
            totalWeeklyMinutes = totalWeeklyMinutes,
            totalWeeklyGoalMinutes = totalWeeklyGoalMinutes,
            totalSessionsCount = allSessions.size,
            currentStreakDays = currentStreak,
            selectedWeekOffset = safeOffset,
            selectedWeekRangeLabel = selectedWeekRangeLabel,
            canGoNextWeek = safeOffset < 0,
            dailyBars = dailyBars,
            subjectStats = subjectStats,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StatsUiState()
    )

    fun shiftWeek(delta: Int) {
        weekOffset.update { current -> (current + delta).coerceAtMost(0) }
    }
}
