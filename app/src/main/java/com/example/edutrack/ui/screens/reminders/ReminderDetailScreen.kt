package com.example.edutrack.ui.screens.reminders

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edutrack.R
import com.example.edutrack.data.local.entity.ReminderEntity
import com.example.edutrack.data.local.entity.ReminderPriority
import com.example.edutrack.ui.components.EmptyStateCard
import com.example.edutrack.ui.components.FigmaBackButton
import com.example.edutrack.ui.components.bounceClick
import com.example.edutrack.ui.theme.DeleteRedText
import com.example.edutrack.ui.theme.ForestGreenDark
import com.example.edutrack.ui.theme.ForestGreenPrimary
import com.example.edutrack.ui.theme.LimeAccent
import com.example.edutrack.ui.theme.TrackBorder
import com.example.edutrack.ui.viewmodel.RemindersViewModel
import com.example.edutrack.util.DateTimeUtils

private enum class ReminderDetailStep {
    MAIN,
    SNOOZE,
    DELETE_CONFIRM
}

/**
 * Exact Figma `24:55` ("Reminder details"), `24:85` ("Snooze reminder"),
 * and `24:95` ("Delete reminder?").
 */
@Composable
fun ReminderDetailScreen(
    reminder: ReminderEntity?,
    onBackClick: () -> Unit,
    onToggleComplete: (ReminderEntity) -> Unit,
    onSnoozeClick: (ReminderEntity, Int) -> Unit,
    onEditClick: (Long) -> Unit,
    onDeleteConfirm: (Long) -> Unit,
    onPreviewAlertClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var currentStep by remember { mutableStateOf(ReminderDetailStep.MAIN) }

    BackHandler(enabled = currentStep != ReminderDetailStep.MAIN) {
        currentStep = ReminderDetailStep.MAIN
    }

    if (reminder == null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            FigmaBackButton(onClick = onBackClick)
            Spacer(modifier = Modifier.height(16.dp))
            EmptyStateCard(
                icon = Icons.Outlined.SearchOff,
                title = "Reminder not found",
                description = "This reminder may have been deleted.",
                actionLabel = "Back to Reminders",
                onActionClick = onBackClick
            )
        }
        return
    }

    val todayEpoch = DateTimeUtils.todayEpochDay()
    val dateText = when (reminder.dueDateEpochDay) {
        todayEpoch -> "Today"
        todayEpoch + 1 -> "Tomorrow"
        todayEpoch - 1 -> "Yesterday"
        else -> DateTimeUtils.formatShortDate(reminder.dueDateEpochDay)
    }
    val dueSummary = "$dateText · ${RemindersViewModel.formatTimeOfDay(reminder.dueTimeMinutes)}"

    AnimatedContent(
        targetState = currentStep,
        transitionSpec = {
            (slideInHorizontally(
                initialOffsetX = { if (targetState == ReminderDetailStep.MAIN) -it / 3 else it / 3 },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn()) togetherWith (slideOutHorizontally(
                targetOffsetX = { if (targetState == ReminderDetailStep.MAIN) it / 3 else -it / 3 },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeOut())
        },
        modifier = modifier.fillMaxSize(),
        label = "reminder_detail_steps"
    ) { step ->
        when (step) {
            ReminderDetailStep.MAIN -> {
                ReminderDetailMainView(
                    reminder = reminder,
                    dueSummary = dueSummary,
                    onBackClick = onBackClick,
                    onToggleComplete = { onToggleComplete(reminder) },
                    onEditClick = { onEditClick(reminder.id) },
                    onOpenSnooze = { currentStep = ReminderDetailStep.SNOOZE },
                    onOpenDelete = { currentStep = ReminderDetailStep.DELETE_CONFIRM },
                    onPreviewAlertClick = onPreviewAlertClick
                )
            }

            ReminderDetailStep.SNOOZE -> {
                SnoozeReminderView(
                    reminder = reminder,
                    dueSummary = dueSummary,
                    onBackClick = { currentStep = ReminderDetailStep.MAIN },
                    onSnoozeSelected = { addMinutes ->
                        onSnoozeClick(reminder, addMinutes)
                        currentStep = ReminderDetailStep.MAIN
                    }
                )
            }

            ReminderDetailStep.DELETE_CONFIRM -> {
                DeleteReminderConfirmView(
                    reminderTitle = reminder.title,
                    onBackClick = { currentStep = ReminderDetailStep.MAIN },
                    onConfirmDelete = { onDeleteConfirm(reminder.id) }
                )
            }
        }
    }
}

@Composable
private fun ReminderDetailMainView(
    reminder: ReminderEntity,
    dueSummary: String,
    onBackClick: () -> Unit,
    onToggleComplete: () -> Unit,
    onEditClick: () -> Unit,
    onOpenSnooze: () -> Unit,
    onOpenDelete: () -> Unit,
    onPreviewAlertClick: () -> Unit
) {
    val priorityLabel = runCatching { ReminderPriority.valueOf(reminder.priority).label }.getOrDefault("Normal")
    val repeatLabel = reminder.formatRepeatSummary()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "‹  Reminders",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = ForestGreenPrimary,
            modifier = Modifier.bounceClick(onClick = onBackClick)
        )

        Column {
            Text(
                text = "Reminder details",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "A little nudge for your everyday tasks.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Exact Figma `24:55` Dark green UP NEXT hero card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = ForestGreenPrimary)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (reminder.isCompleted) "COMPLETED" else "UP NEXT",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFD4E0D5),
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = dueSummary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = LimeAccent
                )
            }
        }

        // Single white 3-row info card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                DetailInfoRow(label = "Repeat", value = repeatLabel)
                HorizontalDivider(color = TrackBorder.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))
                DetailInfoRow(label = "Priority", value = priorityLabel)
                HorizontalDivider(color = TrackBorder.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))
                DetailInfoRow(
                    label = "Notification",
                    value = if (reminder.notifyEnabled) "On · at time of reminder" else "Off · silent task"
                )
            }
        }

        if (reminder.notes.isNotBlank()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "NOTES",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = reminder.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Button(
            onClick = onToggleComplete,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ForestGreenPrimary,
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .bounceClick(onClick = onToggleComplete)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_figma_check),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (reminder.isCompleted) "Mark as active" else "Mark as completed",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .bounceClick(onClick = onEditClick),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "Edit reminder",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .bounceClick(onClick = onOpenSnooze),
                shape = RoundedCornerShape(16.dp),
                color = LimeAccent
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "Snooze",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ForestGreenDark
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onOpenDelete) {
                Text(
                    text = "Delete reminder",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = DeleteRedText
                )
            }
            TextButton(onClick = onPreviewAlertClick) {
                Text(
                    text = "Preview due alert  →",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = ForestGreenPrimary
                )
            }
        }
    }
}

/**
 * Exact Figma `24:85` ("EduTrack · Snooze reminder").
 */
@Composable
private fun SnoozeReminderView(
    reminder: ReminderEntity,
    dueSummary: String,
    onBackClick: () -> Unit,
    onSnoozeSelected: (Int) -> Unit
) {
    val tomorrowMinutesOffset = run {
        val nextDayMorning = 9 * 60
        (24 * 60 - reminder.dueTimeMinutes) + nextDayMorning
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "‹  Reminder",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = ForestGreenPrimary,
            modifier = Modifier.bounceClick(onClick = onBackClick)
        )

        Column {
            Text(
                text = "Snooze reminder",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "A little later is okay.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = ForestGreenPrimary)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = dueSummary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = LimeAccent
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        listOf(
            10 to "In 10 minutes",
            30 to "In 30 minutes",
            60 to "In 1 hour",
            tomorrowMinutesOffset to "Tomorrow morning"
        ).forEach { (mins, label) ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .bounceClick { onSnoozeSelected(mins) },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = label,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "The reminder stays active until you complete it.\nChoose Edit to change the full schedule.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Exact Figma `24:95` ("EduTrack · Delete reminder?").
 */
@Composable
private fun DeleteReminderConfirmView(
    reminderTitle: String,
    onBackClick: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "‹  Reminder",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = ForestGreenPrimary,
            modifier = Modifier.bounceClick(onClick = onBackClick)
        )

        Column {
            Text(
                text = "Delete reminder?",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "This removes the task from your reminders.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "PLEASE CONFIRM",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DeleteRedText,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = reminderTitle,
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 23.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "You can always add a new reminder later.\nYour study subjects will not be affected.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onConfirmDelete,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = DeleteRedText,
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .bounceClick(onClick = onConfirmDelete)
        ) {
            Text(
                text = "Delete reminder",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .bounceClick(onClick = onBackClick),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "Keep reminder",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

@Composable
private fun DetailInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
