package com.example.edutrack.ui.screens.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.edutrack.ui.components.ClayButton
import com.example.edutrack.ui.components.EmptyStateCard
import com.example.edutrack.ui.components.SessionListCard
import com.example.edutrack.ui.components.TopRightHeaderButtons
import com.example.edutrack.ui.components.bounceClick
import com.example.edutrack.ui.components.figmaSubjectIconRes
import com.example.edutrack.ui.theme.ForestGreenDark
import com.example.edutrack.ui.theme.ForestGreenPrimary
import com.example.edutrack.ui.theme.LimeAccent
import com.example.edutrack.ui.theme.subjectPastelContainer
import com.example.edutrack.ui.viewmodel.HomeUiState
import com.example.edutrack.ui.viewmodel.SubjectDashboardSummary
import com.example.edutrack.util.DateTimeUtils
import com.example.edutrack.util.HomeDynamicMessage
import com.example.edutrack.util.HomeMessageAction
import com.example.edutrack.util.HomeMessageCategory
import java.util.Locale

private enum class HomeViewMode {
    LOADING,
    WELCOME,
    DASHBOARD
}

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
    onDynamicActionClick: (HomeMessageAction) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isBrandNewUser = !uiState.isLoading &&
        uiState.subjectSummaries.isEmpty() &&
        uiState.recentSessions.isEmpty()

    val viewMode = when {
        uiState.isLoading -> HomeViewMode.LOADING
        isBrandNewUser -> HomeViewMode.WELCOME
        else -> HomeViewMode.DASHBOARD
    }

    AnimatedContent(
        targetState = viewMode,
        transitionSpec = {
            fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(140))
        },
        modifier = modifier.fillMaxSize(),
        label = "home_loading_transition"
    ) { mode ->
        when (mode) {
            HomeViewMode.LOADING -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            HomeViewMode.WELCOME -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    item(key = "welcome_header", contentType = "header") {
                        WelcomeHeaderSection(
                            unreadNotificationsCount = unreadNotificationsCount,
                            onNotificationsClick = onNotificationsClick,
                            onMeClick = onMeClick
                        )
                    }
                    item(key = "first_study_plan", contentType = "welcome_card") {
                        FirstStudyPlanCard(
                            onAddSubjectClick = onAddFirstSubjectClick
                        )
                    }
                }
            }

            HomeViewMode.DASHBOARD -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Exact Figma Header
                    item(key = "home_header", contentType = "header") {
                        HomeHeaderSection(
                            dynamicMessage = uiState.dynamicMessage,
                            unreadNotificationsCount = unreadNotificationsCount,
                            onNotificationsClick = onNotificationsClick,
                            onMeClick = onMeClick,
                            onDynamicActionClick = onDynamicActionClick
                        )
                    }

                    // Exact Figma Hero Focus Card
                    item(key = "today_focus_card", contentType = "hero_card") {
                        TodayFocusCard(uiState = uiState)
                    }

                    // Exact Figma Weekly Goal Section
                    item(key = "weekly_goal_section", contentType = "weekly_goal") {
                        WeeklyGoalSection(uiState = uiState)
                    }

                    // Your Subjects Section Header
                    item(key = "subjects_header", contentType = "section_header") {
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
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .bounceClick(onClick = onManageSubjectsClick)
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (uiState.subjectSummaries.isEmpty()) {
                        item(key = "subjects_empty", contentType = "empty_state") {
                            EmptyStateCard(
                                iconRes = R.drawable.ic_figma_book,
                                title = "No subjects added yet",
                                description = "Create your first subject in Subjects to start tracking weekly study goals.",
                                actionLabel = "Add Subject",
                                onActionClick = onManageSubjectsClick
                            )
                        }
                    } else {
                        items(
                            items = uiState.subjectSummaries,
                            key = { "subject_${it.subject.id}" },
                            contentType = { "subject_card" }
                        ) { summary ->
                            FigmaSubjectCard(
                                summary = summary,
                                onClick = { onSubjectClick(summary.subject.id) }
                            )
                        }
                    }

                    // Recent Study Sessions Section Header
                    item(key = "recent_sessions_header", contentType = "section_header") {
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

                    if (uiState.recentSessions.isEmpty()) {
                        item(key = "sessions_empty", contentType = "empty_state") {
                            EmptyStateCard(
                                iconRes = R.drawable.ic_figma_stats,
                                title = "No study sessions logged",
                                description = "Open Subjects and start a focus timer to track your study sessions.",
                                actionLabel = "Open Subjects",
                                onActionClick = onManageSubjectsClick
                            )
                        }
                    } else {
                        items(
                            items = uiState.recentSessions,
                            key = { "session_${it.session.id}" },
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
}

@Composable
private fun HomeHeaderSection(
    dynamicMessage: HomeDynamicMessage?,
    unreadNotificationsCount: Int,
    onNotificationsClick: () -> Unit,
    onMeClick: () -> Unit,
    onDynamicActionClick: (HomeMessageAction) -> Unit
) {
    val activeMessage = dynamicMessage ?: HomeDynamicMessage(
        headline = "Your effort. Your momentum.",
        supportingLine = "Pick a subject and start when you're ready.",
        category = HomeMessageCategory.MORNING_FRESH
    )

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
                letterSpacing = 0.8.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            AnimatedContent(
                targetState = activeMessage,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                },
                label = "home_dynamic_message"
            ) { msg ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = msg.headline,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 25.sp,
                            lineHeight = 31.sp
                        ),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = msg.supportingLine,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (msg.actionLabel != null && msg.action != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        val isPending = msg.category == HomeMessageCategory.PENDING_REMINDERS
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .bounceClick { onDynamicActionClick(msg.action) },
                            shape = RoundedCornerShape(999.dp),
                            color = if (isPending) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.primaryContainer
                            }
                        ) {
                            Text(
                                text = "${msg.actionLabel} →",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isPending) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        TopRightHeaderButtons(
            unreadNotificationsCount = unreadNotificationsCount,
            onNotificationsClick = onNotificationsClick,
            onMeClick = onMeClick,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun WelcomeHeaderSection(
    unreadNotificationsCount: Int,
    onNotificationsClick: () -> Unit,
    onMeClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Welcome",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            TopRightHeaderButtons(
                unreadNotificationsCount = unreadNotificationsCount,
                onNotificationsClick = onNotificationsClick,
                onMeClick = onMeClick
            )
        }
        Text(
            text = "YOUR STUDY ROUTINE STARTS HERE",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp
        )
    }
}

@Composable
private fun FirstStudyPlanCard(
    onAddSubjectClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        // Exact Figma `24:400` Study plan introduction dark green card with 96.dp centered book icon
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = ForestGreenPrimary),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_figma_book),
                        contentDescription = null,
                        tint = LimeAccent,
                        modifier = Modifier.size(92.dp)
                    )
                }
                Spacer(modifier = Modifier.height(22.dp))
                Text(
                    text = "A little focus.\nEvery day.",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 30.sp,
                        lineHeight = 38.sp
                    ),
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Build your first study plan",
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Add your subjects, choose daily minutes, and select the days you want to practise.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Exact Figma `24:406` Getting started steps card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 15.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "01   Add a subject and a daily time goal",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "02   Choose repeat days, or study just once",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        ClayButton(
            onClick = onAddSubjectClick,
            shape = RoundedCornerShape(18.dp),
            height = 56.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Add your first subject",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun TodayFocusCard(uiState: HomeUiState) {
    val sessionWord = if (uiState.todaySessionCount == 1) "session" else "sessions"
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
                    text = "${uiState.todaySessionCount} $sessionWord completed",
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
    val loggedFormatted = DateTimeUtils.formatDuration(uiState.weekMinutes)
    val targetFormatted = DateTimeUtils.formatHoursAsHoursMinutes(uiState.totalWeeklyGoalHours)
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
                text = "$loggedFormatted / $targetFormatted",
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
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.outline,
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
    onClick: () -> Unit
) {
    val containerBg = subjectPastelContainer(summary.subject.colorHex)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(scaleDown = 0.98f, onClick = onClick),
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
                    "${DateTimeUtils.formatHoursAsHoursMinutes(summary.subject.weeklyGoalHours)} weekly goal"
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

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = DateTimeUtils.formatDuration(summary.minutesThisWeek),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}
