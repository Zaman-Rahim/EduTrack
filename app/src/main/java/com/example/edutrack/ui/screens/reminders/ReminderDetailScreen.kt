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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.edutrack.data.local.entity.ReminderEntity
import com.example.edutrack.data.local.entity.ReminderPriority
import com.example.edutrack.ui.components.ClayButton
import com.example.edutrack.ui.components.ClayOutlinedButton
import com.example.edutrack.ui.components.EmptyStateCard
import com.example.edutrack.ui.components.FigmaBackButton
import com.example.edutrack.ui.components.bounceClick
import com.example.edutrack.ui.theme.ForestGreenDark
import com.example.edutrack.ui.theme.ForestGreenPrimary
import com.example.edutrack.ui.theme.LimeAccent
import com.example.edutrack.ui.viewmodel.RemindersViewModel
import com.example.edutrack.util.DateTimeUtils

private enum class ReminderDetailStep {
    MAIN,
    SNOOZE,
    DELETE_CONFIRM
}

/**
 * Reminder details and snooze reminder screens,
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
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    onSnoozeTomorrowClick: (ReminderEntity) -> Unit = { onSnoozeClick(it, 12 * 60) }
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
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                EmptyStateCard(
                    iconRes = R.drawable.ic_figma_info,
                    title = "Reminder not found",
                    description = "This reminder may have been deleted.",
                    actionLabel = "Back to Reminders",
                    onActionClick = onBackClick
                )
            }
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
                    },
                    onSnoozeTomorrow = {
                        onSnoozeTomorrowClick(reminder)
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
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "‹  Reminders",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
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

        // UP NEXT reminder hero card
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

        // Single white/surface 3-row info card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                DetailInfoRow(label = "Repeat", value = repeatLabel)
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                DetailInfoRow(label = "Priority", value = priorityLabel)
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
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

        ClayButton(
            onClick = onToggleComplete,
            shape = RoundedCornerShape(16.dp),
            height = 52.dp,
            modifier = Modifier.fillMaxWidth()
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
            ClayOutlinedButton(
                onClick = onEditClick,
                shape = RoundedCornerShape(16.dp),
                height = 52.dp,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Edit reminder",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            ClayButton(
                onClick = onOpenSnooze,
                containerColor = LimeAccent,
                contentColor = ForestGreenDark,
                shape = RoundedCornerShape(16.dp),
                height = 52.dp,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Snooze",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .bounceClick(onClick = onOpenDelete)
                    .padding(horizontal = 10.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Delete reminder",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .bounceClick(onClick = onPreviewAlertClick)
                    .padding(horizontal = 10.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Preview due alert  →",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Snooze reminder dialog.
 */
@Composable
private fun SnoozeReminderView(
    reminder: ReminderEntity,
    dueSummary: String,
    onBackClick: () -> Unit,
    onSnoozeSelected: (Int) -> Unit,
    onSnoozeTomorrow: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "‹  Reminder",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
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

        val snoozeOptions: List<Pair<String, () -> Unit>> = listOf(
            "In 10 minutes" to { onSnoozeSelected(10) },
            "In 30 minutes" to { onSnoozeSelected(30) },
            "In 1 hour" to { onSnoozeSelected(60) },
            "Tomorrow morning" to onSnoozeTomorrow
        )

        snoozeOptions.forEach { (label, onClick) ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .bounceClick(onClick = onClick),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = label,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.ic_figma_arrow),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
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
 * Delete reminder confirmation dialog.
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
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "‹  Reminder",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
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
                    color = MaterialTheme.colorScheme.error,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = reminderTitle,
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 23.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "You can always add a new reminder later.\nYour study subjects will not be affected.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        ClayButton(
            onClick = onConfirmDelete,
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
            shape = RoundedCornerShape(16.dp),
            height = 52.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Delete reminder",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        ClayOutlinedButton(
            onClick = onBackClick,
            shape = RoundedCornerShape(16.dp),
            height = 52.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Keep reminder",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
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
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
