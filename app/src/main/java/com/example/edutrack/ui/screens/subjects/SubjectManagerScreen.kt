package com.example.edutrack.ui.screens.subjects

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.example.edutrack.ui.theme.ForestGreenDark
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
    val todayText = remember {
        "Today · ${DateTimeUtils.formatFullDate(DateTimeUtils.todayEpochDay())}"
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Exact Figma Header with Bell + Me on top right
            item(key = "subjects_header", contentType = "header") {
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
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = todayText,
                            style = MaterialTheme.typography.bodySmall,
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
            }

            if (uiState.isLoading && uiState.allSubjects.isEmpty()) {
                item(key = "subjects_loading", contentType = "loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            } else if (!uiState.isLoading && uiState.allSubjects.isEmpty()) {
                item(key = "subjects_empty", contentType = "empty_state") {
                    Spacer(modifier = Modifier.height(8.dp))
                    EmptyStateCard(
                        iconRes = R.drawable.ic_figma_book,
                        title = "No subjects in your plan yet",
                        description = "Tap the + button in the bottom right to add a subject with your daily target minutes and repeat schedule.",
                        actionLabel = "Add First Subject",
                        onActionClick = onAddClick
                    )
                }
            } else {
                // Ready to focus section
                if (uiState.readyToday.isNotEmpty()) {
                    item(key = "ready_header", contentType = "section_header") {
                        Text(
                            text = "Ready to focus",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                    items(
                        items = uiState.readyToday,
                        key = { "ready_${it.subject.id}" },
                        contentType = { "ready_card" }
                    ) { item ->
                        ReadySubjectTodoCard(
                            item = item,
                            onCardClick = { onStartFocusClick(item.subject) },
                            onEditClick = { onEditScheduleClick(item.subject.id) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }

                // Completed today section (crossed out & moved down)
                if (uiState.completedToday.isNotEmpty()) {
                    item(key = "completed_header", contentType = "section_header") {
                        Text(
                            text = "Completed today",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }
                    items(
                        items = uiState.completedToday,
                        key = { "done_${it.subject.id}" },
                        contentType = { "completed_card" }
                    ) { item ->
                        CompletedSubjectTodoCard(
                            item = item,
                            onEditClick = { onEditScheduleClick(item.subject.id) },
                            onRestartFocusClick = { onStartFocusClick(item.subject) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }

                // Day-off / other days section
                if (uiState.offToday.isNotEmpty()) {
                    item(key = "off_header", contentType = "section_header") {
                        Text(
                            text = "Scheduled on other days",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                    }
                    items(
                        items = uiState.offToday,
                        key = { "off_${it.subject.id}" },
                        contentType = { "ready_card" }
                    ) { item ->
                        ReadySubjectTodoCard(
                            item = item,
                            isDimmed = true,
                            onCardClick = { onStartFocusClick(item.subject) },
                            onEditClick = { onEditScheduleClick(item.subject.id) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }

        // Compact Floating Action Button on the bottom right
        FloatingActionButton(
            onClick = onAddClick,
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 22.dp, bottom = 18.dp)
                .size(52.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_figma_plus),
                contentDescription = "Add subject",
                tint = MaterialTheme.colorScheme.onPrimary,
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
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tagColor = Color(item.subject.colorHex)
    val badgeBg = subjectPastelContainer(item.subject.colorHex)
    val targetText = when {
        item.todayMinutesLogged >= item.subject.dailyGoalMinutes ->
            "${DateTimeUtils.formatDuration(item.todayMinutesLogged)} logged today · ${DateTimeUtils.formatDuration(item.subject.dailyGoalMinutes)} goal"
        item.todayMinutesLogged > 0 -> {
            val remainingMinutes = item.subject.dailyGoalMinutes - item.todayMinutesLogged
            "${DateTimeUtils.formatDuration(remainingMinutes)} left today · ${DateTimeUtils.formatDuration(item.subject.dailyGoalMinutes)} goal"
        }
        else -> "${DateTimeUtils.formatDuration(item.subject.dailyGoalMinutes)} daily target"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .bounceClick(scaleDown = 0.98f, onClick = onCardClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDimmed) {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
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
                        .background(badgeBg)
                        .border(1.5.dp, tagColor, RoundedCornerShape(14.dp)),
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(tagColor)
                        )
                        Text(
                            text = item.subject.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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

                // Edit schedule circular button (>=44dp touch target)
                Box(
                    modifier = Modifier
                        .size(44.dp)
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

                // Start Focus Play button (>=44dp touch target)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .bounceClick(onClick = onCardClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_figma_play),
                        contentDescription = "Start focus",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.subject.formatScheduleSummary(),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CompletedSubjectTodoCard(
    item: SubjectItemUi,
    onEditClick: () -> Unit,
    onRestartFocusClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tagColor = Color(item.subject.colorHex)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .bounceClick(scaleDown = 0.98f, onClick = onRestartFocusClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
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
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_figma_check),
                    contentDescription = "Completed",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(tagColor)
                    )
                    Text(
                        text = item.subject.name,
                        style = MaterialTheme.typography.titleSmall.copy(
                            textDecoration = TextDecoration.LineThrough
                        ),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${DateTimeUtils.formatDuration(item.todayMinutesLogged)} logged · Daily goal reached",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
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
