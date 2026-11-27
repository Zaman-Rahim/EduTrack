package com.example.edutrack.ui.screens.reminders

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edutrack.R
import com.example.edutrack.data.local.entity.ReminderEntity
import com.example.edutrack.data.local.entity.ReminderPriority
import com.example.edutrack.data.local.entity.ReminderRepeatType
import com.example.edutrack.ui.components.ClaySegmentedControl
import com.example.edutrack.ui.components.TopRightHeaderButtons
import com.example.edutrack.ui.components.bounceClick
import com.example.edutrack.ui.theme.DeleteRedBg
import com.example.edutrack.ui.theme.DeleteRedText
import com.example.edutrack.ui.theme.ForestGreenDark
import com.example.edutrack.ui.theme.ForestGreenPrimary
import com.example.edutrack.ui.theme.LimeAccent
import com.example.edutrack.ui.viewmodel.ReminderTabFilter
import com.example.edutrack.ui.viewmodel.RemindersUiState
import com.example.edutrack.ui.viewmodel.RemindersViewModel
import com.example.edutrack.util.DateTimeUtils

@Composable
fun RemindersScreen(
    uiState: RemindersUiState,
    unreadNotificationsCount: Int,
    onNotificationsClick: () -> Unit,
    onMeClick: () -> Unit,
    onAddReminderClick: () -> Unit,
    onSelectTab: (ReminderTabFilter) -> Unit,
    onToggleComplete: (ReminderEntity) -> Unit,
    onReminderClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Reminders",
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Life beyond your study plan.",
                        style = MaterialTheme.typography.bodyMedium,
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

        // Exact Figma Lime "+ Add reminder" CTA
        item {
            Button(
                onClick = onAddReminderClick,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LimeAccent,
                    contentColor = ForestGreenDark
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .bounceClick(onClick = onAddReminderClick)
            ) {
                Text(
                    text = "+  Add reminder",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // 3-Way Segmented Filter Bar (Today | Upcoming | Completed) matching Figma 24:25
        item {
            val tabs = ReminderTabFilter.entries
            ClaySegmentedControl(
                options = tabs.map { it.label },
                selectedIndex = tabs.indexOf(uiState.selectedTab).coerceAtLeast(0),
                onOptionSelected = { index ->
                    onSelectTab(tabs[index])
                }
            )
        }

        item {
            val sectionHeader = when (uiState.selectedTab) {
                ReminderTabFilter.TODAY -> "TODAY’S REMINDERS"
                ReminderTabFilter.UPCOMING -> "UPCOMING REMINDERS"
                ReminderTabFilter.COMPLETED -> "COMPLETED REMINDERS"
            }
            Text(
                text = sectionHeader,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (!uiState.isLoading && uiState.visibleReminders.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.NotificationsNone,
                            contentDescription = null,
                            modifier = Modifier.size(44.dp),
                            tint = ForestGreenPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No reminders here",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap '+ Add reminder' above to schedule an assignment, exam, or daily personal task.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onAddReminderClick,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ForestGreenPrimary,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.bounceClick(onClick = onAddReminderClick)
                        ) {
                            Text("Add Reminder")
                        }
                    }
                }
            }
        } else {
            items(uiState.visibleReminders, key = { "rem_${it.id}" }) { reminder ->
                ReminderListCard(
                    reminder = reminder,
                    onToggleComplete = { onToggleComplete(reminder) },
                    onClick = { onReminderClick(reminder.id) }
                )
            }
        }
    }
}

@Composable
private fun ReminderListCard(
    reminder: ReminderEntity,
    onToggleComplete: () -> Unit,
    onClick: () -> Unit
) {
    val todayEpoch = DateTimeUtils.todayEpochDay()
    val dateLabel = when (reminder.dueDateEpochDay) {
        todayEpoch -> "Today"
        todayEpoch + 1 -> "Tomorrow"
        todayEpoch - 1 -> "Yesterday"
        else -> DateTimeUtils.formatShortDate(reminder.dueDateEpochDay)
    }
    val timeLabel = RemindersViewModel.formatTimeOfDay(reminder.dueTimeMinutes)
    val repeatLabel = reminder.formatRepeatSummary()
    val isHighPriority = reminder.priority == ReminderPriority.HIGH.name

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(scaleDown = 0.98f, onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color(0xFFCCD8C4)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (reminder.isCompleted) ForestGreenPrimary else MaterialTheme.colorScheme.background)
                    .border(
                        width = 1.dp,
                        color = if (reminder.isCompleted) ForestGreenPrimary else Color(0xFFCCD8C4),
                        shape = CircleShape
                    )
                    .bounceClick(onClick = onToggleComplete),
                contentAlignment = Alignment.Center
            ) {
                if (reminder.isCompleted) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_figma_check),
                        contentDescription = "Completed",
                        tint = LimeAccent,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(13.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = reminder.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            textDecoration = if (reminder.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        fontWeight = FontWeight.SemiBold,
                        color = if (reminder.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isHighPriority && !reminder.isCompleted) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DeleteRedBg
                        ) {
                            Text(
                                text = "High",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DeleteRedText,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "$dateLabel · $timeLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = repeatLabel,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                painter = painterResource(id = R.drawable.ic_figma_arrow),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
