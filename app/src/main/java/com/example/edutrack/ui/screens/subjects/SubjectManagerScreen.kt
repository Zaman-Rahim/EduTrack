package com.example.edutrack.ui.screens.subjects

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edutrack.R
import com.example.edutrack.data.local.entity.SubjectEntity
import com.example.edutrack.ui.components.EmptyStateCard
import com.example.edutrack.ui.components.TopRightHeaderButtons
import com.example.edutrack.ui.components.bounceClick
import com.example.edutrack.ui.components.figmaSubjectIconRes
import com.example.edutrack.ui.theme.CardLightGreen
import com.example.edutrack.ui.theme.ForestGreenDark
import com.example.edutrack.ui.theme.ForestGreenPrimary
import com.example.edutrack.ui.theme.LimeAccent
import com.example.edutrack.ui.theme.subjectPastelContainer
import com.example.edutrack.ui.viewmodel.SubjectItemUi
import com.example.edutrack.ui.viewmodel.SubjectManagerUiState
import com.example.edutrack.util.DateTimeUtils

@Composable
fun SubjectManagerScreen(
    uiState: SubjectManagerUiState,
    unreadNotificationsCount: Int,
    onNotificationsClick: () -> Unit,
    onMeClick: () -> Unit,
    onAddClick: () -> Unit,
    onEditScheduleClick: (Long) -> Unit,
    onStartFocusClick: (SubjectEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val todayText = "Today · ${DateTimeUtils.formatFullDate(DateTimeUtils.todayEpochDay())}"

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Exact Figma Header with Bell + Me on top right
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Your subjects",
                            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = todayText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TopRightHeaderButtons(
                        unreadNotificationsCount = unreadNotificationsCount,
                        onNotificationsClick = onNotificationsClick,
                        onMeClick = onMeClick
                    )
                }
            }

            if (!uiState.isLoading && uiState.allSubjects.isEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    EmptyStateCard(
                        icon = Icons.AutoMirrored.Outlined.MenuBook,
                        title = "No subjects in your plan yet",
                        description = "Tap the + button in the bottom right to add a subject with your daily target minutes and repeat schedule.",
                        actionLabel = "Add First Subject",
                        onActionClick = onAddClick
                    )
                }
            } else {
                // Ready to focus section
                if (uiState.readyToday.isNotEmpty()) {
                    item {
                        Text(
                            text = "Ready to focus",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                    items(uiState.readyToday, key = { "ready_${it.subject.id}" }) { item ->
                        ReadySubjectTodoCard(
                            item = item,
                            onCardClick = { onStartFocusClick(item.subject) },
                            onEditClick = { onEditScheduleClick(item.subject.id) }
                        )
                    }
                }

                // Completed today section (crossed out & moved down!)
                item {
                    AnimatedVisibility(visible = uiState.completedToday.isNotEmpty()) {
                        Text(
                            text = "Completed today",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }
                }
                items(uiState.completedToday, key = { "done_${it.subject.id}" }) { item ->
                    CompletedSubjectTodoCard(
                        item = item,
                        onEditClick = { onEditScheduleClick(item.subject.id) },
                        onRestartFocusClick = { onStartFocusClick(item.subject) }
                    )
                }

                // Day-off / other days section
                if (uiState.offToday.isNotEmpty()) {
                    item {
                        Text(
                            text = "Scheduled on other days",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                    }
                    items(uiState.offToday, key = { "off_${it.subject.id}" }) { item ->
                        ReadySubjectTodoCard(
                            item = item,
                            isDimmed = true,
                            onCardClick = { onStartFocusClick(item.subject) },
                            onEditClick = { onEditScheduleClick(item.subject.id) }
                        )
                    }
                }
            }
        }

        // Small compact Floating Action Button on the bottom right (matching user request)
        FloatingActionButton(
            onClick = onAddClick,
            shape = RoundedCornerShape(16.dp),
            containerColor = ForestGreenPrimary,
            contentColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 22.dp, bottom = 18.dp)
                .size(52.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_figma_plus),
                contentDescription = "Add subject",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun ReadySubjectTodoCard(
    item: SubjectItemUi,
    isDimmed: Boolean = false,
    onCardClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val containerBg = subjectPastelContainer(item.subject.colorHex)
    val remainingMinutes = (item.subject.dailyGoalMinutes - item.todayMinutesLogged).coerceAtLeast(1)
    val targetText = if (item.todayMinutesLogged > 0) {
        "${DateTimeUtils.formatDuration(remainingMinutes)} left today · ${DateTimeUtils.formatDuration(item.subject.dailyGoalMinutes)} goal"
    } else {
        "${DateTimeUtils.formatDuration(item.subject.dailyGoalMinutes)} daily target"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(scaleDown = 0.98f, onClick = onCardClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDimmed) MaterialTheme.colorScheme.surface.copy(alpha = 0.78f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, Color(0xFFC2D1B8)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 13.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(containerBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = figmaSubjectIconRes(item.subject.name)),
                        contentDescription = null,
                        tint = ForestGreenDark,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.subject.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = targetText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Edit schedule circular button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.background)
                        .bounceClick(onClick = onEditClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_figma_edit_schedule),
                        contentDescription = "Edit schedule",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Start Focus Play button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(CardLightGreen)
                        .bounceClick(onClick = onCardClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_figma_play),
                        contentDescription = "Start focus",
                        tint = ForestGreenPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.subject.formatScheduleSummary(),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CompletedSubjectTodoCard(
    item: SubjectItemUi,
    onEditClick: () -> Unit,
    onRestartFocusClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(scaleDown = 0.98f, onClick = onRestartFocusClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardLightGreen),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(ForestGreenPrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_figma_check),
                    contentDescription = "Completed",
                    tint = LimeAccent,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.subject.name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        textDecoration = TextDecoration.LineThrough
                    ),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${DateTimeUtils.formatDuration(item.todayMinutesLogged)} logged · Daily goal reached",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.background)
                    .bounceClick(onClick = onEditClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_figma_edit_schedule),
                    contentDescription = "Edit schedule",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
