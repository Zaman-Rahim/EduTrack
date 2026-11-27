package com.example.edutrack.ui.screens.home

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edutrack.R
import com.example.edutrack.ui.components.EmptyStateCard
import com.example.edutrack.ui.components.SessionListCard
import com.example.edutrack.ui.components.TopRightHeaderButtons
import com.example.edutrack.ui.components.bounceClick
import com.example.edutrack.ui.components.figmaSubjectIconRes
import com.example.edutrack.ui.theme.ForestGreenDark
import com.example.edutrack.ui.theme.ForestGreenPrimary
import com.example.edutrack.ui.theme.LimeAccent
import com.example.edutrack.ui.theme.TrackBorder
import com.example.edutrack.ui.theme.subjectPastelContainer
import com.example.edutrack.ui.viewmodel.HomeUiState
import com.example.edutrack.ui.viewmodel.SubjectDashboardSummary
import com.example.edutrack.util.DateTimeUtils
import java.util.Locale

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    unreadNotificationsCount: Int,
    onNotificationsClick: () -> Unit,
    onMeClick: () -> Unit,
    onAddFirstSubjectClick: () -> Unit,
    onAddSessionClick: () -> Unit,
    onQuickAddForSubject: (Long) -> Unit,
    onSubjectClick: (Long) -> Unit,
    onSessionClick: (Long) -> Unit,
    onManageSubjectsClick: () -> Unit,
    onViewAllHistoryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBrandNewUser = !uiState.isLoading &&
        uiState.subjectSummaries.isEmpty() &&
        uiState.recentSessions.isEmpty()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Exact Figma Header
        item {
            HomeHeaderSection(
                unreadNotificationsCount = unreadNotificationsCount,
                onNotificationsClick = onNotificationsClick,
                onMeClick = onMeClick
            )
        }

        if (isBrandNewUser) {
            item {
                FirstStudyPlanCard(
                    onAddSubjectClick = onAddFirstSubjectClick,
                    onLogSessionClick = onAddSessionClick
                )
            }
            return@LazyColumn
        }

        // Exact Figma Hero Focus Card
        item {
            TodayFocusCard(uiState = uiState)
        }

        // Exact Figma Weekly Goal Section
        item {
            WeeklyGoalSection(uiState = uiState)
        }

        // Your Subjects Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your subjects",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "View all",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClick = onManageSubjectsClick)
                )
            }
        }

        if (!uiState.isLoading && uiState.subjectSummaries.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.AutoMirrored.Outlined.MenuBook,
                    title = "No subjects added yet",
                    description = "Create your first subject in Subjects to start tracking weekly study goals.",
                    actionLabel = "Add Subject",
                    onActionClick = onManageSubjectsClick
                )
            }
        } else {
            items(uiState.subjectSummaries, key = { "subject_${it.subject.id}" }) { summary ->
                FigmaSubjectCard(
                    summary = summary,
                    onClick = { onSubjectClick(summary.subject.id) },
                    onQuickAddClick = { onQuickAddForSubject(summary.subject.id) }
                )
            }
        }

        // Recent Study Sessions Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent sessions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(onClick = onViewAllHistoryClick) {
                    Text(
                        text = "History",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        if (!uiState.isLoading && uiState.recentSessions.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Outlined.AutoGraph,
                    title = "No study sessions logged",
                    description = "Tap 'Log a study session' above to record your first study session.",
                    actionLabel = "Log Session",
                    onActionClick = onAddSessionClick
                )
            }
        } else {
            items(uiState.recentSessions, key = { "session_${it.session.id}" }) { sessionItem ->
                SessionListCard(
                    item = sessionItem,
                    onClick = { onSessionClick(sessionItem.session.id) }
                )
            }
        }
    }
}

@Composable
private fun HomeHeaderSection(
    unreadNotificationsCount: Int,
    onNotificationsClick: () -> Unit,
    onMeClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = DateTimeUtils.formatFullDate(DateTimeUtils.todayEpochDay()).uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Your effort.\nYour momentum.",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 30.sp,
                    lineHeight = 38.sp
                ),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        TopRightHeaderButtons(
            unreadNotificationsCount = unreadNotificationsCount,
            onNotificationsClick = onNotificationsClick,
            onMeClick = onMeClick,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun FirstStudyPlanCard(
    onAddSubjectClick: () -> Unit,
    onLogSessionClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = ForestGreenPrimary),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = LimeAccent
                ) {
                    Text(
                        text = "FRESH START",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = ForestGreenDark,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Build your first\ndaily study plan",
                    style = MaterialTheme.typography.headlineSmall.copy(lineHeight = 32.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Add the subjects you want to focus on, pick your daily minutes and repeat days, and count down live.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFD3E0D5)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onAddSubjectClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .bounceClick(onClick = onAddSubjectClick),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LimeAccent,
                        contentColor = ForestGreenDark
                    )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_figma_plus),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Add your first subject",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "How EduTrack works",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "1. Add subjects with daily target minutes and weekly repeat days.\n2. Tap a subject in Today's To-Do to start the live countdown timer.\n3. When the timer hits 00:00, it auto-saves your session and moves to Completed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
                TextButton(onClick = onLogSessionClick) {
                    Text(
                        text = "Or log a past session manually",
                        color = ForestGreenPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun TodayFocusCard(uiState: HomeUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = ForestGreenPrimary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TODAY’S FOCUS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFD3E0D5),
                    letterSpacing = 0.8.sp
                )
                Surface(
                    shape = RoundedCornerShape(15.dp),
                    color = LimeAccent
                ) {
                    Text(
                        text = "${uiState.currentStreakDays}-day streak",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = ForestGreenPrimary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = DateTimeUtils.formatDuration(uiState.todayMinutes),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 44.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "A little progress, every day.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFD3E0D5)
            )

            Spacer(modifier = Modifier.height(22.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${uiState.todaySessionCount} sessions completed",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                Icon(
                    painter = painterResource(id = R.drawable.ic_figma_check),
                    contentDescription = null,
                    tint = LimeAccent,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun WeeklyGoalSection(uiState: HomeUiState) {
    val loggedHours = uiState.weekMinutes / 60.0
    val targetHours = uiState.totalWeeklyGoalHours
    val percentage = uiState.weeklyPercentage

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Weekly goal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = String.format(Locale.getDefault(), "%.1f / %.1f hrs", loggedHours, targetHours),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val animatedProgress by animateFloatAsState(
            targetValue = uiState.weeklyProgressFraction,
            label = "weekly_progress"
        )

        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = ForestGreenPrimary,
            trackColor = TrackBorder,
            strokeCap = StrokeCap.Round
        )

        Text(
            text = "You’re $percentage% there. Keep your rhythm.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FigmaSubjectCard(
    summary: SubjectDashboardSummary,
    onClick: () -> Unit,
    onQuickAddClick: () -> Unit
) {
    val containerBg = subjectPastelContainer(summary.subject.colorHex)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Exact Figma 48dp pastel icon box (15dp radius)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(containerBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = figmaSubjectIconRes(summary.subject.name)),
                    contentDescription = null,
                    tint = ForestGreenDark,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = summary.subject.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                val notesText = if (summary.latestSessionNotes.isNullOrBlank()) {
                    String.format(Locale.getDefault(), "%.1fh weekly goal", summary.subject.weeklyGoalHours)
                } else {
                    summary.latestSessionNotes
                }
                Text(
                    text = notesText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = String.format(Locale.getDefault(), "%.1fh", summary.hoursThisWeek),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onQuickAddClick)
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }
    }
}
