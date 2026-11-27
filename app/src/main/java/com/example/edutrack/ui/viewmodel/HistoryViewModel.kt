package com.example.edutrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.edutrack.data.local.entity.SubjectEntity
import com.example.edutrack.data.local.relation.SessionWithSubject
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

/**
 * Date range presets supported by the Session History filter bar.
 */
enum class DateRangeFilter(val label: String) {
    ALL("All Time"),
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month")
}

/**
 * Immutable UI state for [com.example.edutrack.ui.screens.history.HistoryScreen].
 */
data class HistoryUiState(
    val subjects: List<SubjectEntity> = emptyList(),
    val sessions: List<SessionWithSubject> = emptyList(),
    val searchQuery: String = "",
    val selectedSubjectId: Long? = null,
    val selectedDateFilter: DateRangeFilter = DateRangeFilter.ALL,
    val isLoading: Boolean = true
) {
    val totalFilteredMinutes: Int
        get() = sessions.sumOf { it.session.durationMinutes }
}

/**
 * ViewModel managing Session History filtering by search query, subject, and date range.
 */
class HistoryViewModel(
    repository: EduTrackRepository,
    preferencesManager: UserPreferencesManager? = null
) : ViewModel() {

    private data class FilterCriteria(
        val searchQuery: String = "",
        val subjectId: Long? = null,
        val dateFilter: DateRangeFilter = DateRangeFilter.ALL
    )

    private val filterFlow = MutableStateFlow(FilterCriteria())

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

    val uiState: StateFlow<HistoryUiState> = combine(
        repository.observeAllSubjects(),
        repository.observeAllSessionsWithSubject(),
        filterFlow,
        weekStartsOnFlow,
        todayDatePulseFlow
    ) { subjects, allSessions, filter, weekStartsOn, todayDate ->
        val today = DateTimeUtils.todayEpochDay(todayDate)
        val weekBounds = DateTimeUtils.currentWeekBounds(weekStartsOn = weekStartsOn, today = todayDate)
        val monthBounds = DateTimeUtils.currentMonthBounds(today = todayDate)
        val normalizedQuery = filter.searchQuery.trim()

        val filtered = allSessions.filter { item ->
            val matchesQuery = normalizedQuery.isEmpty() ||
                item.session.notes.contains(normalizedQuery, ignoreCase = true) ||
                item.subject.name.contains(normalizedQuery, ignoreCase = true)
            val matchesSubject = filter.subjectId == null || item.subject.id == filter.subjectId
            val day = item.session.sessionDateEpochDay
            val matchesDate = when (filter.dateFilter) {
                DateRangeFilter.ALL -> true
                DateRangeFilter.TODAY -> day == today
                DateRangeFilter.THIS_WEEK -> day in weekBounds.first..weekBounds.second
                DateRangeFilter.THIS_MONTH -> day in monthBounds.first..monthBounds.second
            }
            matchesQuery && matchesSubject && matchesDate
        }

        HistoryUiState(
            subjects = subjects,
            sessions = filtered,
            searchQuery = filter.searchQuery,
            selectedSubjectId = filter.subjectId,
            selectedDateFilter = filter.dateFilter,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HistoryUiState()
    )

    fun onSearchQueryChanged(query: String) {
        filterFlow.update { it.copy(searchQuery = query) }
    }

    fun selectSubjectFilter(subjectId: Long?) {
        filterFlow.update { it.copy(subjectId = subjectId) }
    }

    fun selectDateFilter(filter: DateRangeFilter) {
        filterFlow.update { it.copy(dateFilter = filter) }
    }

    fun clearFilters() {
        filterFlow.value = FilterCriteria()
    }
}
