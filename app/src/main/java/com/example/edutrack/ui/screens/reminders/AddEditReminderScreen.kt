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
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edutrack.data.local.entity.ReminderPriority
import com.example.edutrack.data.local.entity.ReminderRepeatType
import com.example.edutrack.ui.components.FigmaCustomDateTimeScreen
import com.example.edutrack.ui.components.bounceClick
import com.example.edutrack.ui.theme.ForestGreenDark
import com.example.edutrack.ui.theme.ForestGreenPrimary
import com.example.edutrack.ui.theme.LimeAccent
import com.example.edutrack.ui.viewmodel.ReminderEditorState
import com.example.edutrack.ui.viewmodel.RemindersViewModel
import com.example.edutrack.util.DateTimeUtils

private enum class ReminderEditorStep {
    MAIN,
    DATE_TIME_PICKS,
    CUSTOM_CALENDAR,
    REPEAT_PICKER
}

/**
 * Exact Figma `24:45` ("Reminder editor"), `24:65` ("Date & time"),
 * `24:1179` ("Custom date & time Calendar"), and `24:75` ("Repeat reminder").
 */
@Composable
fun AddEditReminderScreen(
    state: ReminderEditorState,
    onBackClick: () -> Unit,
    onTitleChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    onDueDateSelected: (Long) -> Unit,
    onDueTimeSelected: (Int) -> Unit,
    onRepeatSelected: (ReminderRepeatType) -> Unit,
    onToggleRepeatDayBit: (Int) -> Unit = {},
    onRepeatDaysMaskChanged: (Int) -> Unit = {},
    onPrioritySelected: (ReminderPriority) -> Unit,
    onNotifyToggle: (Boolean) -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentStep by remember { mutableStateOf(ReminderEditorStep.MAIN) }

    BackHandler(enabled = currentStep != ReminderEditorStep.MAIN) {
        currentStep = when (currentStep) {
            ReminderEditorStep.CUSTOM_CALENDAR -> ReminderEditorStep.DATE_TIME_PICKS
            ReminderEditorStep.DATE_TIME_PICKS, ReminderEditorStep.REPEAT_PICKER -> ReminderEditorStep.MAIN
            ReminderEditorStep.MAIN -> ReminderEditorStep.MAIN
        }
    }

    val todayEpoch = DateTimeUtils.todayEpochDay()
    val dateLabel = when (state.dueDateEpochDay) {
        todayEpoch -> "Today"
        todayEpoch + 1 -> "Tomorrow"
        todayEpoch - 1 -> "Yesterday"
        else -> DateTimeUtils.formatShortDate(state.dueDateEpochDay)
    }
    val timeLabel = RemindersViewModel.formatTimeOfDay(state.dueTimeMinutes)
    val dateAndTimeSummary = "$dateLabel · $timeLabel"

    val repeatSummary = remember(state.repeatType, state.repeatDaysMask) {
        if (state.repeatType != ReminderRepeatType.CUSTOM) {
            state.repeatType.label
        } else {
            val shortDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            val active = shortDays.filterIndexed { i, _ -> (state.repeatDaysMask and (1 shl i)) != 0 }
            if (active.isEmpty()) "Does not repeat" else active.joinToString(" · ")
        }
    }

    AnimatedContent(
        targetState = currentStep,
        transitionSpec = {
            (slideInHorizontally(
                initialOffsetX = { if (targetState == ReminderEditorStep.MAIN) -it / 3 else it / 3 },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn()) togetherWith (slideOutHorizontally(
                targetOffsetX = { if (targetState == ReminderEditorStep.MAIN) it / 3 else -it / 3 },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeOut())
        },
        modifier = modifier.fillMaxSize(),
        label = "reminder_editor_steps"
    ) { step ->
        when (step) {
            ReminderEditorStep.MAIN -> {
                ReminderEditorMainView(
                    state = state,
                    dateAndTimeSummary = dateAndTimeSummary,
                    repeatSummary = repeatSummary,
                    onBackClick = onBackClick,
                    onTitleChanged = onTitleChanged,
                    onNotesChanged = onNotesChanged,
                    onOpenDateTimePicker = { currentStep = ReminderEditorStep.DATE_TIME_PICKS },
                    onOpenRepeatPicker = { currentStep = ReminderEditorStep.REPEAT_PICKER },
                    onPrioritySelected = onPrioritySelected,
                    onNotifyToggle = onNotifyToggle,
                    onSaveClick = onSaveClick
                )
            }

            ReminderEditorStep.DATE_TIME_PICKS -> {
                ReminderDateTimePicksView(
                    currentSelectionText = dateAndTimeSummary,
                    onBackClick = { currentStep = ReminderEditorStep.MAIN },
                    onQuickPickSelected = { epochDay, mins ->
                        onDueDateSelected(epochDay)
                        onDueTimeSelected(mins)
                        currentStep = ReminderEditorStep.MAIN
                    },
                    onOpenCustomCalendar = { currentStep = ReminderEditorStep.CUSTOM_CALENDAR }
                )
            }

            ReminderEditorStep.CUSTOM_CALENDAR -> {
                FigmaCustomDateTimeScreen(
                    title = "Custom date & time",
                    confirmLabel = "Use date & time",
                    initialEpochDay = state.dueDateEpochDay,
                    initialTimeMinutes = state.dueTimeMinutes,
                    showTimeSection = true,
                    onBackClick = { currentStep = ReminderEditorStep.DATE_TIME_PICKS },
                    onConfirm = { epochDay, timeMinutes ->
                        onDueDateSelected(epochDay)
                        onDueTimeSelected(timeMinutes)
                        currentStep = ReminderEditorStep.MAIN
                    }
                )
            }

            ReminderEditorStep.REPEAT_PICKER -> {
                ReminderRepeatPickerView(
                    selectedRepeatType = state.repeatType,
                    selectedDaysMask = state.repeatDaysMask,
                    repeatSummary = repeatSummary,
                    onBackClick = { currentStep = ReminderEditorStep.MAIN },
                    onRepeatSelected = { type ->
                        onRepeatSelected(type)
                        currentStep = ReminderEditorStep.MAIN
                    },
                    onToggleDayBit = onToggleRepeatDayBit,
                    onUseSelectedWeekdays = {
                        onRepeatDaysMaskChanged(state.repeatDaysMask)
                        currentStep = ReminderEditorStep.MAIN
                    }
                )
            }
        }
    }
}

@Composable
private fun ReminderEditorMainView(
    state: ReminderEditorState,
    dateAndTimeSummary: String,
    repeatSummary: String,
    onBackClick: () -> Unit,
    onTitleChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    onOpenDateTimePicker: () -> Unit,
    onOpenRepeatPicker: () -> Unit,
    onPrioritySelected: (ReminderPriority) -> Unit,
    onNotifyToggle: (Boolean) -> Unit,
    onSaveClick: () -> Unit
) {
    val isEdit = state.editingId != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
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
                text = if (isEdit) "Edit reminder" else "Add reminder",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Any task. One less thing to remember.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // TASK TITLE
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SectionEyebrow("TASK TITLE")
            OutlinedTextField(
                value = state.title,
                onValueChange = onTitleChanged,
                placeholder = { Text("Submit assignment") },
                singleLine = true,
                shape = RoundedCornerShape(15.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = ForestGreenPrimary,
                    unfocusedBorderColor = Color(0xFFCCD8C4)
                ),
                isError = state.titleError != null,
                supportingText = state.titleError?.let { err -> { Text(err) } },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // DATE & TIME (single Figma clickable box opening `24:65` Date & time picker)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SectionEyebrow("DATE & TIME")
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .bounceClick(onClick = onOpenDateTimePicker),
                shape = RoundedCornerShape(15.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, Color(0xFFCCD8C4))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = dateAndTimeSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "›",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // REPEAT (single Figma clickable box opening `24:75` Repeat reminder)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SectionEyebrow("REPEAT")
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .bounceClick(onClick = onOpenRepeatPicker),
                shape = RoundedCornerShape(15.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, Color(0xFFCCD8C4))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = repeatSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "›",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // PRIORITY (3 separate Figma pill cards: Low, Normal, High)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionEyebrow("PRIORITY")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ReminderPriority.entries.forEach { priority ->
                    val isSelected = state.priority == priority
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .bounceClick { onPrioritySelected(priority) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) ForestGreenPrimary else MaterialTheme.colorScheme.surface
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = priority.label,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
            Text(
                text = state.priority.label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Notification setting row (`24:580`)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .bounceClick { onNotifyToggle(!state.notifyEnabled) },
            shape = RoundedCornerShape(15.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Notification",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (state.notifyEnabled) "On · at time of reminder" else "Off · silent task",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (state.notifyEnabled) ForestGreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // NOTES
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SectionEyebrow("NOTES")
            OutlinedTextField(
                value = state.notes,
                onValueChange = onNotesChanged,
                placeholder = { Text("Upload the final PDF to the student portal.") },
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(15.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = ForestGreenPrimary,
                    unfocusedBorderColor = Color(0xFFCCD8C4)
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Button(
            onClick = onSaveClick,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ForestGreenPrimary,
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .bounceClick(onClick = onSaveClick)
        ) {
            Text(
                text = if (isEdit) "Save changes" else "Save reminder",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Exact Figma `24:65` ("EduTrack · Reminder date and time") Quick Picks screen.
 */
@Composable
private fun ReminderDateTimePicksView(
    currentSelectionText: String,
    onBackClick: () -> Unit,
    onQuickPickSelected: (epochDay: Long, timeMinutes: Int) -> Unit,
    onOpenCustomCalendar: () -> Unit
) {
    val today = DateTimeUtils.todayEpochDay()

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
                text = "Date & time",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Choose when you would like a nudge.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        SectionEyebrow("QUICK PICKS")

        QuickPickTile(
            title = "Today · 9:00 AM",
            subtitle = "A morning reminder",
            onClick = { onQuickPickSelected(today, 9 * 60) }
        )
        QuickPickTile(
            title = "Today · 5:00 PM",
            subtitle = "Later this afternoon",
            onClick = { onQuickPickSelected(today, 17 * 60) }
        )
        QuickPickTile(
            title = "Tomorrow · 8:00 PM",
            subtitle = "Tomorrow evening",
            onClick = { onQuickPickSelected(today + 1, 20 * 60) }
        )
        QuickPickTile(
            title = "Choose date & time",
            subtitle = "Open calendar and time picker",
            onClick = onOpenCustomCalendar
        )

        Spacer(modifier = Modifier.height(6.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = ForestGreenPrimary)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "CURRENT SELECTION",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFD4E0D5),
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = currentSelectionText,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun QuickPickTile(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(78.dp)
            .bounceClick(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Exact Figma `24:75` ("EduTrack · Repeat reminder") screen with presets and custom weekdays.
 */
@Composable
private fun ReminderRepeatPickerView(
    selectedRepeatType: ReminderRepeatType,
    selectedDaysMask: Int,
    repeatSummary: String,
    onBackClick: () -> Unit,
    onRepeatSelected: (ReminderRepeatType) -> Unit,
    onToggleDayBit: (Int) -> Unit,
    onUseSelectedWeekdays: () -> Unit
) {
    val presets = listOf(
        ReminderRepeatType.NONE,
        ReminderRepeatType.DAILY,
        ReminderRepeatType.WEEKLY,
        ReminderRepeatType.MONTHLY
    )
    val dayLetters = listOf("M", "T", "W", "T", "F", "S", "S")

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
                text = "Repeat reminder",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Build a rhythm that works for you.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        presets.forEach { preset ->
            val selected = selectedRepeatType == preset
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .bounceClick { onRepeatSelected(preset) },
                shape = RoundedCornerShape(16.dp),
                color = if (selected) ForestGreenPrimary else MaterialTheme.colorScheme.surface
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = preset.label,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selected) Color.White else MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        SectionEyebrow("CUSTOM WEEKDAYS")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            dayLetters.forEachIndexed { index, letter ->
                val active = (selectedDaysMask and (1 shl index)) != 0
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (active) ForestGreenPrimary else MaterialTheme.colorScheme.surface)
                        .bounceClick { onToggleDayBit(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = letter,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (active) Color.White else ForestGreenDark
                    )
                }
            }
        }

        Text(
            text = "Select any combination of days. Each selected day will use the reminder's chosen time.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Button(
            onClick = onUseSelectedWeekdays,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ForestGreenPrimary,
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .bounceClick(onClick = onUseSelectedWeekdays)
        ) {
            Text(
                text = "Use selected weekdays",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            SectionEyebrow("CURRENT REPEAT")
            Text(
                text = repeatSummary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun SectionEyebrow(text: String) {
    Text(
        text = text,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 0.8.sp,
        textAlign = TextAlign.Start
    )
}
