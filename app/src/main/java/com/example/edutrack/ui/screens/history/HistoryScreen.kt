package com.example.edutrack.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FilterListOff
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edutrack.R
import com.example.edutrack.ui.components.EmptyStateCard
import com.example.edutrack.ui.components.SessionListCard
import com.example.edutrack.ui.components.SubjectColorDot
import com.example.edutrack.ui.theme.CardLightGreen
import com.example.edutrack.ui.theme.ForestGreenDark
import com.example.edutrack.ui.theme.ForestGreenPrimary
import com.example.edutrack.ui.theme.TrackBorder
import com.example.edutrack.ui.viewmodel.DateRangeFilter
import com.example.edutrack.ui.viewmodel.HistoryUiState
import com.example.edutrack.util.DateTimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    uiState: HistoryUiState,
    onSearchQueryChanged: (String) -> Unit,
    onSelectSubjectFilter: (Long?) -> Unit,
    onSelectDateFilter: (DateRangeFilter) -> Unit,
    onCustomDateRangeSelected: (Long, Long) -> Unit,
    onClearFilters: () -> Unit,
    onSessionClick: (Long) -> Unit,
    onLogSessionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCustomRangePicker by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }

    val todayEpoch = DateTimeUtils.todayEpochDay()
    val groupedSessions = remember(uiState.sessions) {
        uiState.sessions.groupBy { it.session.sessionDateEpochDay }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Exact Figma Header with 48dp Dark Green circular plus icon button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Session history",
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Small sessions. Lasting progress.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(ForestGreenPrimary)
                    .clickable(onClick = onLogSessionClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_figma_plus),
                    contentDescription = "Log new session",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Filter row matching Figma: Subject pill, Date pill, and Filter icon toggle for search/date bar
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                Box(
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(19.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { showSearchBar = !showSearchBar }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_figma_filter),
                        contentDescription = "Toggle search & filters",
                        tint = if (showSearchBar || uiState.searchQuery.isNotEmpty()) ForestGreenPrimary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            item {
                FilterChip(
                    selected = uiState.selectedSubjectId == null,
                    onClick = { onSelectSubjectFilter(null) },
                    label = { Text("All subjects") },
                    border = null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ForestGreenPrimary,
                        selectedLabelColor = Color.White,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(19.dp)
                )
            }
            items(uiState.subjects, key = { "filter_subject_${it.id}" }) { subject ->
                FilterChip(
                    selected = uiState.selectedSubjectId == subject.id,
                    onClick = {
                        val next = if (uiState.selectedSubjectId == subject.id) null else subject.id
                        onSelectSubjectFilter(next)
                    },
                    leadingIcon = {
                        SubjectColorDot(colorHex = subject.colorHex, size = 10.dp)
                    },
                    label = { Text(subject.name) },
                    border = null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ForestGreenPrimary,
                        selectedLabelColor = Color.White,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(19.dp)
                )
            }
        }

        // Date range filter chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(DateRangeFilter.entries, key = { "filter_date_${it.name}" }) { filter ->
                val label = if (
                    filter == DateRangeFilter.CUSTOM &&
                    uiState.selectedDateFilter == DateRangeFilter.CUSTOM &&
                    uiState.customStartEpochDay != null &&
                    uiState.customEndEpochDay != null
                ) {
                    "${DateTimeUtils.formatShortDate(uiState.customStartEpochDay)} - " +
                        DateTimeUtils.formatShortDate(uiState.customEndEpochDay)
                } else {
                    filter.label
                }

                FilterChip(
                    selected = uiState.selectedDateFilter == filter,
                    onClick = {
                        if (filter == DateRangeFilter.CUSTOM) {
                            showCustomRangePicker = true
                        } else {
                            onSelectDateFilter(filter)
                        }
                    },
                    label = { Text(label) },
                    border = null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ForestGreenPrimary,
                        selectedLabelColor = Color.White,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(19.dp)
                )
            }
        }

        // Expandable search bar
        if (showSearchBar || uiState.searchQuery.isNotEmpty()) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChanged,
                placeholder = { Text("Search notes or subject...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChanged("") }) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Clear search"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = ForestGreenPrimary,
                    unfocusedBorderColor = TrackBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp)
            )
        }

        // Figma Weekly Session Summary Card (#EAF0E7 + Figma Clock icon)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 10.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = CardLightGreen
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = DateTimeUtils.formatMinutesAsHours(uiState.totalFilteredMinutes),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = ForestGreenDark
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "across ${uiState.sessions.size} session(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    painter = painterResource(id = R.drawable.ic_figma_clock),
                    contentDescription = null,
                    tint = ForestGreenPrimary,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        if (uiState.searchQuery.isNotBlank() ||
            uiState.selectedSubjectId != null ||
            uiState.selectedDateFilter != DateRangeFilter.ALL
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onClearFilters) {
                    Icon(
                        imageVector = Icons.Outlined.FilterListOff,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear filters", color = ForestGreenPrimary)
                }
            }
        }

        if (!uiState.isLoading && uiState.sessions.isEmpty()) {
            Column(modifier = Modifier.padding(24.dp)) {
                val hasActiveFilter = uiState.searchQuery.isNotBlank() ||
                    uiState.selectedSubjectId != null ||
                    uiState.selectedDateFilter != DateRangeFilter.ALL
                EmptyStateCard(
                    icon = Icons.Outlined.History,
                    title = if (hasActiveFilter) "No matching sessions" else "No session history yet",
                    description = if (hasActiveFilter) {
                        "Try clearing or adjusting your subject and date range filters."
                    } else {
                        "Logged study sessions will appear here for review."
                    },
                    actionLabel = if (hasActiveFilter) "Clear Filters" else "Log First Session",
                    onActionClick = if (hasActiveFilter) onClearFilters else onLogSessionClick
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                groupedSessions.forEach { (epochDay, daySessions) ->
                    val dayHeader = when (epochDay) {
                        todayEpoch -> "Today"
                        todayEpoch - 1 -> "Yesterday"
                        else -> DateTimeUtils.formatShortDate(epochDay)
                    }
                    val dayTotalMinutes = daySessions.sumOf { it.session.durationMinutes }

                    item(key = "header_$epochDay") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dayHeader,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = DateTimeUtils.formatDuration(dayTotalMinutes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    items(daySessions, key = { "history_session_${it.session.id}" }) { sessionItem ->
                        SessionListCard(
                            item = sessionItem,
                            onClick = { onSessionClick(sessionItem.session.id) }
                        )
                    }
                }
            }
        }
    }

    if (showCustomRangePicker) {
        val rangeState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = uiState.customStartEpochDay?.let {
                DateTimeUtils.epochDayToUtcMillis(it)
            },
            initialSelectedEndDateMillis = uiState.customEndEpochDay?.let {
                DateTimeUtils.epochDayToUtcMillis(it)
            }
        )
        DatePickerDialog(
            onDismissRequest = { showCustomRangePicker = false },
            confirmButton = {
                TextButton(
                    enabled = rangeState.selectedStartDateMillis != null &&
                        rangeState.selectedEndDateMillis != null,
                    onClick = {
                        val start = rangeState.selectedStartDateMillis
                        val end = rangeState.selectedEndDateMillis
                        if (start != null && end != null) {
                            onCustomDateRangeSelected(
                                DateTimeUtils.utcMillisToEpochDay(start),
                                DateTimeUtils.utcMillisToEpochDay(end)
                            )
                        }
                        showCustomRangePicker = false
                    }
                ) {
                    Text("Apply", color = ForestGreenPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomRangePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DateRangePicker(
                state = rangeState,
                modifier = Modifier.height(420.dp)
            )
        }
    }
}
