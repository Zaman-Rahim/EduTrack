package com.example.edutrack.ui.screens.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edutrack.R
import com.example.edutrack.ui.components.ClayScrollableChipRow
import com.example.edutrack.ui.components.EmptyStateCard
import com.example.edutrack.ui.components.SessionListCard
import com.example.edutrack.ui.components.TopRightHeaderButtons
import com.example.edutrack.ui.components.bounceClick
import com.example.edutrack.ui.viewmodel.DateRangeFilter
import com.example.edutrack.ui.viewmodel.HistoryUiState
import com.example.edutrack.util.DateTimeUtils

@Composable
fun HistoryScreen(
    uiState: HistoryUiState,
    unreadNotificationsCount: Int,
    onNotificationsClick: () -> Unit,
    onMeClick: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onSelectSubjectFilter: (Long?) -> Unit,
    onSelectDateFilter: (DateRangeFilter) -> Unit,
    @Suppress("UNUSED_PARAMETER") onCustomDateRangeSelected: (Long, Long) -> Unit = { _, _ -> },
    onClearFilters: () -> Unit,
    onSessionClick: (Long) -> Unit,
    onLogSessionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSearchBar by remember { mutableStateOf(false) }

    val todayEpoch = remember { DateTimeUtils.todayEpochDay() }
    val groupedSessions = remember(uiState.sessions) {
        uiState.sessions.groupBy { it.session.sessionDateEpochDay }
    }
    val hasActiveFilter = uiState.searchQuery.isNotBlank() ||
        uiState.selectedSubjectId != null ||
        uiState.selectedDateFilter != DateRangeFilter.ALL

    Column(modifier = modifier.fillMaxSize()) {
        // Header with TopRightHeaderButtons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Session history",
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Small sessions. Lasting progress.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            TopRightHeaderButtons(
                unreadNotificationsCount = unreadNotificationsCount,
                onNotificationsClick = onNotificationsClick,
                onMeClick = onMeClick
            )
        }

        // Subject filter chips with passing clay pill + Filter icon toggle
        val subjectOptions = remember(uiState.subjects) {
            listOf("All subjects") + uiState.subjects.map { it.name }
        }
        val selectedSubjectIndex = if (uiState.selectedSubjectId == null) {
            0
        } else {
            val found = uiState.subjects.indexOfFirst { it.id == uiState.selectedSubjectId }
            if (found >= 0) found + 1 else 0
        }
        ClayScrollableChipRow(
            options = subjectOptions,
            selectedIndex = selectedSubjectIndex,
            onOptionSelected = { index ->
                if (index == 0) {
                    onSelectSubjectFilter(null)
                } else {
                    val subject = uiState.subjects.getOrNull(index - 1)
                    if (subject != null) {
                        val next = if (uiState.selectedSubjectId == subject.id) null else subject.id
                        onSelectSubjectFilter(next)
                    }
                }
            },
            leadingContent = {
                Box(
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(19.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .bounceClick { showSearchBar = !showSearchBar }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_figma_filter),
                        contentDescription = "Toggle search & filters",
                        tint = if (showSearchBar || uiState.searchQuery.isNotEmpty()) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Date range filter chips (All time, Today, This week, This month) with passing clay pill
        val dateFilters = remember { DateRangeFilter.entries.toList() }
        val dateLabels = remember(dateFilters) { dateFilters.map { it.label } }
        ClayScrollableChipRow(
            options = dateLabels,
            selectedIndex = dateFilters.indexOf(uiState.selectedDateFilter).coerceAtLeast(0),
            onOptionSelected = { index ->
                onSelectDateFilter(dateFilters[index])
            }
        )

        // Expandable search bar
        AnimatedVisibility(
            visible = showSearchBar || uiState.searchQuery.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChanged,
                placeholder = { Text("Search notes or subject...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChanged("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
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
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp)
            )
        }

        // Weekly Session Summary Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 10.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = DateTimeUtils.formatDuration(uiState.totalFilteredMinutes),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val sessionWord = if (uiState.sessions.size == 1) "session" else "sessions"
                    Text(
                        text = "across ${uiState.sessions.size} $sessionWord",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }

                Icon(
                    painter = painterResource(id = R.drawable.ic_figma_clock),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = hasActiveFilter,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onClearFilters) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_figma_filter),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear filters", color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        if (!uiState.isLoading && uiState.sessions.isEmpty()) {
            Column(modifier = Modifier.padding(24.dp)) {
                EmptyStateCard(
                    iconRes = R.drawable.ic_figma_history,
                    title = if (hasActiveFilter) "No matching sessions" else "No session history yet",
                    description = if (hasActiveFilter) {
                        "Try clearing or adjusting your subject and date range filters."
                    } else {
                        "Completed focus sessions will appear here for review."
                    },
                    actionLabel = if (hasActiveFilter) "Clear Filters" else null,
                    onActionClick = if (hasActiveFilter) onClearFilters else null
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

                    item(key = "header_$epochDay", contentType = "day_header") {
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

                    items(
                        items = daySessions,
                        key = { "history_session_${it.session.id}" },
                        contentType = { "session_card" }
                    ) { sessionItem ->
                        SessionListCard(
                            item = sessionItem,
                            onClick = { onSessionClick(sessionItem.session.id) }
                        )
                    }
                }
            }
        }
    }
}
