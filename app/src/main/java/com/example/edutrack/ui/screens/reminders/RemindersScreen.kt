package com.example.edutrack.ui.screens.reminders

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.edutrack.R
import com.example.edutrack.data.local.entity.ReminderEntity
import com.example.edutrack.data.local.entity.ReminderPriority
import com.example.edutrack.ui.components.ClayButton
import com.example.edutrack.ui.components.ClaySegmentedControl
import com.example.edutrack.ui.components.EmptyStateCard
import com.example.edutrack.ui.components.TopRightHeaderButtons
import com.example.edutrack.ui.components.bounceClick
import com.example.edutrack.ui.theme.ForestGreenDark
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
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var canScheduleExact by remember { mutableStateOf(checkCanScheduleExactAlarms(context)) }
    var dismissedExactAlarmNotice by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                canScheduleExact = checkCanScheduleExactAlarms(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val showExactAlarmNotice = !canScheduleExact &&
        !dismissedExactAlarmNotice &&
        uiState.selectedTab != ReminderTabFilter.COMPLETED &&
        uiState.visibleReminders.any { !it.isCompleted && it.notifyEnabled }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item(key = "header", contentType = "header") {
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
        item(key = "add_cta", contentType = "add_cta") {
            ClayButton(
                onClick = onAddReminderClick,
                containerColor = LimeAccent,
                contentColor = ForestGreenDark,
                shape = RoundedCornerShape(16.dp),
                height = 52.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "+  Add reminder",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // 3-Way Segmented Filter Bar (Today | Upcoming | Completed (N)) matching Figma 24:25
        item(key = "filter_bar", contentType = "filter_bar") {
            val tabs = ReminderTabFilter.entries
            val options = tabs.map { tab ->
                if (tab == ReminderTabFilter.COMPLETED) {
                    "${tab.label} (${uiState.completedTodayCount})"
                } else {
                    tab.label
                }
            }
            ClaySegmentedControl(
                options = options,
                selectedIndex = tabs.indexOf(uiState.selectedTab).coerceAtLeast(0),
                onOptionSelected = { index ->
                    onSelectTab(tabs[index])
                }
            )
        }

        if (showExactAlarmNotice) {
            item(key = "exact_alarm_guidance", contentType = "guidance_banner") {
                ExactAlarmGuidanceCard(
                    onAllowClick = { openExactAlarmSettings(context) },
                    onDismissClick = { dismissedExactAlarmNotice = true }
                )
            }
        }

        item(key = "section_header", contentType = "section_header") {
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
            item(key = "empty_state", contentType = "empty_state") {
                EmptyStateCard(
                    iconRes = R.drawable.ic_figma_bell,
                    title = "No reminders here",
                    description = "Tap '+ Add reminder' above to schedule an assignment, exam, or daily personal task.",
                    actionLabel = "Add Reminder",
                    onActionClick = onAddReminderClick
                )
            }
        } else {
            items(
                items = uiState.visibleReminders,
                key = { "rem_${it.id}" },
                contentType = { "reminder_card" }
            ) { reminder ->
                ReminderListCard(
                    reminder = reminder,
                    onToggleComplete = { onToggleComplete(reminder) },
                    onClick = { onReminderClick(reminder.id) },
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
}

@Composable
private fun ExactAlarmGuidanceCard(
    onAllowClick: () -> Unit,
    onDismissClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Exact alarm permission is off",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "Allow exact alarms so scheduled reminders ring on time even in battery saver mode.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .bounceClick(onClick = onDismissClick)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Dismiss",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                ClayButton(
                    onClick = onAllowClick,
                    height = 36.dp,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp)
                ) {
                    Text(
                        text = "Allow exact",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun ReminderListCard(
    reminder: ReminderEntity,
    onToggleComplete: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
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
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .bounceClick(scaleDown = 0.98f, onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 15.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 48.dp minimum accessible touch target wrapping the 24.dp visual checkbox
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .semantics {
                        role = Role.Checkbox
                        stateDescription = if (reminder.isCompleted) "Completed" else "Not completed"
                    }
                    .bounceClick(onClick = onToggleComplete),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            if (reminder.isCompleted) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.background
                        )
                        .border(
                            width = 1.5.dp,
                            color = if (reminder.isCompleted) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (reminder.isCompleted) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_figma_check),
                            contentDescription = "Completed",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(2.dp))

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
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = "High",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
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

private fun checkCanScheduleExactAlarms(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return true
    return runCatching { alarmManager.canScheduleExactAlarms() }.getOrDefault(true)
}

private fun openExactAlarmSettings(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val packageIntent = Intent(
        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
        Uri.parse("package:${context.packageName}")
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(packageIntent) }
        .onFailure {
            val fallbackIntent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(fallbackIntent) }
        }
}
