package com.example.edutrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.edutrack.data.local.entity.SubjectEntity
import com.example.edutrack.data.local.relation.SessionWithSubject
import com.example.edutrack.data.repository.EduTrackRepository
import com.example.edutrack.util.DateTimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Date range presets supported by the Session History filter bar.
 */
enum class DateRangeFilter(val label: String) {
    ALL("All Time"),
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    CUSTOM("Custom")
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
    val customStartEpochDay: Long? = null,
    val customEndEpochDay: Long? = null,
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
    initialSubjectId: Long = -1L
) : ViewModel() {

    private data class FilterCriteria(
        val searchQuery: String,
        val subjectId: Long?,
        val dateFilter: DateRangeFilter,
        val customStartEpochDay: Long?,
        val customEndEpochDay: Long?
    )

    private val filterFlow = MutableStateFlow(
        FilterCriteria(
            searchQuery = "",
            subjectId = initialSubjectId.takeIf { it > 0L },
            dateFilter = DateRangeFilter.ALL,
            customStartEpochDay = null,
            customEndEpochDay = null
        )
    )

    val uiState: StateFlow<HistoryUiState> = combine(
        repository.observeAllSubjects(),
        repository.observeAllSessionsWithSubject(),
        filterFlow
    ) { subjects, allSessions, filter ->
        val today = DateTimeUtils.todayEpochDay()
        val weekBounds = DateTimeUtils.currentWeekBounds()
        val monthBounds = DateTimeUtils.currentMonthBounds()
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
                DateRangeFilter.CUSTOM -> {
                    val start = filter.customStartEpochDay
                    val end = filter.customEndEpochDay
                    if (start != null && end != null) day in start..end else true
                }
            }
            matchesQuery && matchesSubject && matchesDate
        }

        HistoryUiState(
            subjects = subjects,
            sessions = filtered,
            searchQuery = filter.searchQuery,
            selectedSubjectId = filter.subjectId,
            selectedDateFilter = filter.dateFilter,
            customStartEpochDay = filter.customStartEpochDay,
            customEndEpochDay = filter.customEndEpochDay,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HistoryUiState(selectedSubjectId = initialSubjectId.takeIf { it > 0L })
    )

    fun onSearchQueryChanged(query: String) {
        filterFlow.value = filterFlow.value.copy(searchQuery = query)
    }

    fun selectSubjectFilter(subjectId: Long?) {
        filterFlow.value = filterFlow.value.copy(subjectId = subjectId)
    }

    fun selectDateFilter(filter: DateRangeFilter) {
        filterFlow.value = filterFlow.value.copy(dateFilter = filter)
    }

    fun setCustomDateRange(startEpochDay: Long, endEpochDay: Long) {
        val orderedStart = minOf(startEpochDay, endEpochDay)
        val orderedEnd = maxOf(startEpochDay, endEpochDay)
        filterFlow.value = filterFlow.value.copy(
            dateFilter = DateRangeFilter.CUSTOM,
            customStartEpochDay = orderedStart,
            customEndEpochDay = orderedEnd
        )
    }

    fun clearFilters() {
        filterFlow.value = FilterCriteria(
            searchQuery = "",
            subjectId = null,
            dateFilter = DateRangeFilter.ALL,
            customStartEpochDay = null,
            customEndEpochDay = null
        )
    }
}
