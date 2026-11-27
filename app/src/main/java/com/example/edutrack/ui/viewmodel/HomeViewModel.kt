package com.example.edutrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.edutrack.data.local.entity.StudySessionEntity
import com.example.edutrack.data.local.entity.SubjectEntity
import com.example.edutrack.data.local.relation.SessionWithSubject
import com.example.edutrack.data.preferences.UserPreferencesManager
import com.example.edutrack.data.repository.EduTrackRepository
import com.example.edutrack.util.DateTimeUtils
import com.example.edutrack.util.HomeDynamicMessage
import com.example.edutrack.util.HomeMessageContext
import com.example.edutrack.util.HomeMessageEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.LocalTime
import kotlin.random.Random

/**
 * Aggregated subject summary displayed on the Home Dashboard.
 */
data class SubjectDashboardSummary(
    val subject: SubjectEntity,
    val minutesThisWeek: Int,
    val latestSessionNotes: String?
) {
    val hoursThisWeek: Double get() = minutesThisWeek / 60.0
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
    val dynamicMessage: HomeDynamicMessage? = null,
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
class HomeViewModel(
    repository: EduTrackRepository,
    preferencesManager: UserPreferencesManager? = null
) : ViewModel() {

    private data class TemporalSnapshot(
        val todayDate: LocalDate,
        val localTime: LocalTime,
        val weekStartsOn: String
    )

    private val visitVariantSeed = Random.nextInt(0, 10_000)

    private val dateTimePulseFlow: Flow<Pair<LocalDate, LocalTime>> = flow {
        while (true) {
            emit(LocalDate.now() to LocalTime.now())
            delay(60_000L)
        }
    }

    private val weekStartsOnFlow: Flow<String> = preferencesManager?.state
        ?.map { it.weekStartsOn }
        ?.distinctUntilChanged()
        ?: flowOf("Monday")

    private val temporalFlow: Flow<TemporalSnapshot> = combine(
        dateTimePulseFlow,
        weekStartsOnFlow
    ) { (todayDate, nowTime), weekStartsOn ->
        TemporalSnapshot(
            todayDate = todayDate,
            localTime = nowTime,
            weekStartsOn = weekStartsOn
        )
    }

    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeSubjectsWithSessions(),
        repository.observeAllSessionsWithSubject(),
        repository.observeAllReminders(),
        temporalFlow
    ) { subjectsWithSessions, allSessions, allReminders, temporal ->
        val todayDate = temporal.todayDate
        val todayEpochDay = DateTimeUtils.todayEpochDay(todayDate)
        val weekBounds = DateTimeUtils.currentWeekBounds(
            weekStartsOn = temporal.weekStartsOn,
            today = todayDate
        )

        val todayMinutes = allSessions
            .filter { it.session.sessionDateEpochDay == todayEpochDay }
            .sumOf { it.session.durationMinutes }

        val todaySessionsCount = allSessions.count {
            it.session.sessionDateEpochDay == todayEpochDay
        }

        val weekMinutes = allSessions
            .filter { it.session.sessionDateEpochDay in weekBounds.first..weekBounds.second }
            .sumOf { it.session.durationMinutes }

        val summaries = subjectsWithSessions.map { item ->
            val latest = item.sessions.maxWithOrNull(
                compareBy<StudySessionEntity> { it.sessionDateEpochDay }
                    .thenBy { it.createdAt }
            )
            val minutesThisWeek = item.sessions
                .filter { it.sessionDateEpochDay in weekBounds.first..weekBounds.second }
                .sumOf { it.durationMinutes }

            SubjectDashboardSummary(
                subject = item.subject,
                minutesThisWeek = minutesThisWeek,
                latestSessionNotes = latest?.notes
            )
        }

        val totalGoalHours = subjectsWithSessions.sumOf { it.subject.weeklyGoalHours }
        val totalDailyGoalMinutes = subjectsWithSessions
            .filter { it.subject.isScheduledOn(todayDate) }
            .sumOf { it.subject.dailyGoalMinutes }
        val (currentStreak, _) = DateTimeUtils.calculateStreaks(
            sessionEpochDays = allSessions.map { it.session.sessionDateEpochDay },
            todayEpochDay = todayEpochDay
        )

        val nowTime = temporal.localTime
        val nowMinutesOfDay = nowTime.hour * 60 + nowTime.minute
        val pendingDueCount = allReminders.count { reminder ->
            !reminder.isCompleted && (
                reminder.dueDateEpochDay < todayEpochDay ||
                    (reminder.dueDateEpochDay == todayEpochDay && reminder.dueTimeMinutes <= nowMinutesOfDay)
                )
        }
        val latestSessionEpochDay = allSessions.maxOfOrNull { it.session.sessionDateEpochDay }
        val daysSinceLastStudy = latestSessionEpochDay?.let {
            (todayEpochDay - it).toInt().coerceAtLeast(0)
        }

        val dynamicMessage = HomeMessageEngine.selectMessage(
            context = HomeMessageContext(
                localTime = nowTime,
                pendingDueRemindersCount = pendingDueCount,
                todayMinutes = todayMinutes,
                todaySessionCount = todaySessionsCount,
                totalDailyGoalMinutes = totalDailyGoalMinutes,
                currentStreakDays = currentStreak,
                daysSinceLastStudy = daysSinceLastStudy
            ),
            variantSeed = visitVariantSeed
        )

        HomeUiState(
            todayMinutes = todayMinutes,
            todaySessionCount = todaySessionsCount,
            weekMinutes = weekMinutes,
            totalWeeklyGoalHours = totalGoalHours,
            currentStreakDays = currentStreak,
            subjectSummaries = summaries,
            recentSessions = allSessions.take(4),
            dynamicMessage = dynamicMessage,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )
}
