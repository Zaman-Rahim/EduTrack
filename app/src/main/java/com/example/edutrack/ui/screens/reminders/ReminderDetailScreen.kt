package com.example.edutrack.ui.screens.reminders

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.edutrack.data.local.entity.ReminderRepeatType
import com.example.edutrack.ui.components.ConfirmDeleteDialog
import com.example.edutrack.ui.components.EmptyStateCard
import com.example.edutrack.ui.components.FigmaBackButton
import com.example.edutrack.ui.components.bounceClick
import com.example.edutrack.ui.theme.CardLightGreen
import com.example.edutrack.ui.theme.DeleteRedBg
import com.example.edutrack.ui.theme.DeleteRedText
import com.example.edutrack.ui.theme.ForestGreenDark
import com.example.edutrack.ui.theme.ForestGreenPrimary
import com.example.edutrack.ui.theme.LimeAccent
import com.example.edutrack.ui.theme.TrackBorder
import com.example.edutrack.ui.viewmodel.RemindersViewModel
import com.example.edutrack.util.DateTimeUtils

@Composable
fun ReminderDetailScreen(
    reminder: ReminderEntity?,
    onBackClick: () -> Unit,
    onToggleComplete: (ReminderEntity) -> Unit,
    onSnoozeClick: (ReminderEntity, Int) -> Unit,
    onEditClick: (Long) -> Unit,
    onDeleteConfirm: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

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

    val priorityLabel = runCatching { ReminderPriority.valueOf(reminder.priority).label }.getOrDefault("Normal")
    val repeatLabel = runCatching { ReminderRepeatType.valueOf(reminder.repeatType).label }.getOrDefault("Does not repeat")

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            FigmaBackButton(onClick = onBackClick)
            Text(
                text = "Reminder details",
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardLightGreen)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (reminder.priority == ReminderPriority.HIGH.name) DeleteRedBg else Color.White
                    ) {
                        Text(
                            text = "$priorityLabel priority",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (reminder.priority == ReminderPriority.HIGH.name) DeleteRedText else ForestGreenDark,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (reminder.isCompleted) ForestGreenPrimary else LimeAccent
                    ) {
                        Text(
                            text = if (reminder.isCompleted) "Completed" else "Scheduled",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (reminder.isCompleted) Color.White else ForestGreenDark,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = ForestGreenDark
                )

                Text(
                    text = "${DateTimeUtils.formatFullDate(reminder.dueDateEpochDay)} · ${RemindersViewModel.formatTimeOfDay(reminder.dueTimeMinutes)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_figma_clock),
                        contentDescription = null,
                        tint = ForestGreenPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = repeatLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = TrackBorder)

                Text(
                    text = "Notes",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = reminder.notes.ifBlank { "No notes added for this reminder." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Snooze options
        if (!reminder.isCompleted) {
            Text(
                text = "Snooze reminder",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(15 to "+15 min", 60 to "+1 hour", 1440 to "Tomorrow").forEach { (mins, label) ->
                    OutlinedButton(
                        onClick = { onSnoozeClick(reminder, mins) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(label, fontSize = 12.sp, color = ForestGreenPrimary)
                    }
                }
            }
        }

        Button(
            onClick = { onToggleComplete(reminder) },
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = LimeAccent,
                contentColor = ForestGreenDark
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .bounceClick { onToggleComplete(reminder) }
        ) {
            Text(
                text = if (reminder.isCompleted) "Reopen reminder" else "Mark as completed",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { onEditClick(reminder.id) },
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ForestGreenPrimary,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
            ) {
                Text("Edit reminder", fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = { showDeleteDialog = true },
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DeleteRedBg,
                    contentColor = DeleteRedText
                ),
                modifier = Modifier
                    .width(96.dp)
                    .height(54.dp)
            ) {
                Text("Delete", fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (showDeleteDialog) {
        ConfirmDeleteDialog(
            title = "Delete reminder?",
            message = "This will permanently remove \"${reminder.title}\".",
            onConfirm = {
                showDeleteDialog = false
                onDeleteConfirm(reminder.id)
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}
