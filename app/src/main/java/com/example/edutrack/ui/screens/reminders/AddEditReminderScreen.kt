package com.example.edutrack.ui.screens.reminders

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edutrack.R
import com.example.edutrack.data.local.entity.ReminderPriority
import com.example.edutrack.data.local.entity.ReminderRepeatType
import com.example.edutrack.ui.components.ClayButton
import com.example.edutrack.ui.components.ClayOutlinedButton
import com.example.edutrack.ui.components.EmptyStateCard
import com.example.edutrack.ui.components.FigmaCustomDateTimeScreen
import com.example.edutrack.ui.components.bounceClick
import com.example.edutrack.ui.viewmodel.ReminderEditorState
import com.example.edutrack.ui.viewmodel.RemindersViewModel
import com.example.edutrack.util.DateTimeUtils
import kotlinx.coroutines.launch

private enum class ReminderEditorStep {
    MAIN,
    CUSTOM_CALENDAR,
    REPEAT_PICKER
}

/**
 * Reminder editor and date & time configuration screen,
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
    onDismissPastTimeError: () -> Unit = {},
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
    weekStartsOn: String = "Monday"
) {
    val notFoundMessage = state.notFoundError
    if (notFoundMessage != null) {
        Column(
            modifier = modifier
                .fillMaxSize()
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
            EmptyStateCard(
                iconRes = R.drawable.ic_figma_info,
                title = "Reminder not found",
                description = notFoundMessage,
                actionLabel = "Back to Reminders",
                onActionClick = onBackClick
            )
        }
        return
    }

    if (state.isLoading) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "‹  Reminders",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.bounceClick(onClick = onBackClick)
            )
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
        return
    }

    var currentStep by remember { mutableStateOf(ReminderEditorStep.MAIN) }

    BackHandler(enabled = currentStep != ReminderEditorStep.MAIN) {
        currentStep = ReminderEditorStep.MAIN
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
                    onOpenDateTimePicker = { currentStep = ReminderEditorStep.CUSTOM_CALENDAR },
                    onOpenRepeatPicker = { currentStep = ReminderEditorStep.REPEAT_PICKER },
                    onPrioritySelected = onPrioritySelected,
                    onNotifyToggle = onNotifyToggle,
                    onSaveClick = onSaveClick
                )
            }

            ReminderEditorStep.CUSTOM_CALENDAR -> {
                FigmaCustomDateTimeScreen(
                    title = "Date and time",
                    confirmLabel = "Use date & time",
                    initialEpochDay = state.dueDateEpochDay,
                    initialTimeMinutes = state.dueTimeMinutes,
                    showTimeSection = true,
                    weekStartsOn = weekStartsOn,
                    onBackClick = { currentStep = ReminderEditorStep.MAIN },
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
                    onToggleDayBit = { dayIndex ->
                        val nextMask = if (state.repeatType == ReminderRepeatType.CUSTOM) {
                            state.repeatDaysMask xor (1 shl dayIndex)
                        } else {
                            1 shl dayIndex
                        }
                        onRepeatDaysMaskChanged(nextMask)
                    },
                    onUseSelectedWeekdays = {
                        onRepeatDaysMaskChanged(state.repeatDaysMask)
                        currentStep = ReminderEditorStep.MAIN
                    }
                )
            }
        }
    }

    state.pastTimeError?.let { message ->
        AlertDialog(
            onDismissRequest = onDismissPastTimeError,
            title = {
                Text(
                    text = "Time has already passed",
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = { Text(message) },
            confirmButton = {
                ClayButton(
                    onClick = {
                        onDismissPastTimeError()
                        currentStep = ReminderEditorStep.CUSTOM_CALENDAR
                    },
                    height = 40.dp,
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    Text("Change time")
                }
            },
            dismissButton = {
                ClayOutlinedButton(
                    onClick = onDismissPastTimeError,
                    containerColor = Color.Transparent,
                    borderColor = MaterialTheme.colorScheme.outline,
                    height = 40.dp,
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    Text("OK")
                }
            }
        )
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
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
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
                placeholder = { Text("Enter reminder name") },
                singleLine = true,
                shape = RoundedCornerShape(15.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
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
                    .clip(RoundedCornerShape(15.dp))
                    .bounceClick(onClick = onOpenDateTimePicker),
                shape = RoundedCornerShape(15.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
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
                        color = MaterialTheme.colorScheme.onSurface
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
                    .clip(RoundedCornerShape(15.dp))
                    .bounceClick(onClick = onOpenRepeatPicker),
                shape = RoundedCornerShape(15.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
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
                        color = MaterialTheme.colorScheme.onSurface
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

        // PRIORITY (3 separate Figma pill cards with passing clay pill animation)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionEyebrow("PRIORITY")
            val priorities = ReminderPriority.entries
            val selectedPriorityIndex = priorities.indexOf(state.priority).coerceAtLeast(0)
            val stretchX = remember { Animatable(1f) }
            val stretchY = remember { Animatable(1f) }
            var priorityInit by remember { mutableStateOf(false) }

            LaunchedEffect(selectedPriorityIndex) {
                if (!priorityInit) {
                    priorityInit = true
                    return@LaunchedEffect
                }
                launch {
                    stretchX.animateTo(1.12f, animationSpec = tween(durationMillis = 80))
                    stretchX.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                }
                launch {
                    stretchY.animateTo(0.88f, animationSpec = tween(durationMillis = 80))
                    stretchY.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                }
            }

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val gap = 10.dp
                val pillWidth = (maxWidth - gap * 2) / priorities.size
                val targetX = (pillWidth + gap) * selectedPriorityIndex
                val animatedX by animateDpAsState(
                    targetValue = targetX,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "priority_clay_x"
                )

                // Card backgrounds
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gap)
                ) {
                    priorities.forEach { _ ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                        )
                    }
                }

                // Sliding primary clay pill
                Box(
                    modifier = Modifier
                        .width(pillWidth)
                        .height(52.dp)
                        .graphicsLayer {
                            translationX = animatedX.toPx()
                            scaleX = stretchX.value
                            scaleY = stretchY.value
                        }
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )

                // Interactive labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gap)
                ) {
                    priorities.forEach { priority ->
                        val isSelected = state.priority == priority
                        val textColor by animateColorAsState(
                            targetValue = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            label = "priority_text_color"
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .bounceClick { onPrioritySelected(priority) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = priority.label,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textColor
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
                .clip(RoundedCornerShape(15.dp))
                .bounceClick { onNotifyToggle(!state.notifyEnabled) },
            shape = RoundedCornerShape(15.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
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
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (state.notifyEnabled) "On · at time of reminder" else "Off · silent task",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (state.notifyEnabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }

        // NOTES
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SectionEyebrow("NOTES")
            OutlinedTextField(
                value = state.notes,
                onValueChange = onNotesChanged,
                placeholder = { Text("Add notes (optional)") },
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(15.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        ClayButton(
            onClick = onSaveClick,
            enabled = !state.isSaving && state.notFoundError == null,
            shape = RoundedCornerShape(16.dp),
            height = 52.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
            Text(
                text = when {
                    state.isSaving -> "Saving..."
                    isEdit -> "Save changes"
                    else -> "Save reminder"
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Repeat reminder screen with presets and custom weekdays.
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
    val presets = remember {
        listOf(
            ReminderRepeatType.NONE,
            ReminderRepeatType.DAILY,
            ReminderRepeatType.WEEKDAYS,
            ReminderRepeatType.WEEKLY,
            ReminderRepeatType.MONTHLY
        )
    }
    val dayLetters = remember { listOf("M", "T", "W", "T", "F", "S", "S") }
    val isCustomActive = selectedRepeatType == ReminderRepeatType.CUSTOM

    val selectedPresetIndex = presets.indexOf(selectedRepeatType)
    val hasPresetSelected = selectedPresetIndex >= 0
    val itemHeight = 52.dp
    val itemGap = 10.dp

    val pillAlpha by animateFloatAsState(
        targetValue = if (hasPresetSelected) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "repeat_preset_pill_alpha"
    )
    val pillOffsetY by animateDpAsState(
        targetValue = if (hasPresetSelected) (itemHeight + itemGap) * selectedPresetIndex else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "repeat_preset_pill_y"
    )
    val pillStretchX = remember { Animatable(1f) }
    val pillStretchY = remember { Animatable(1f) }
    var presetInit by remember { mutableStateOf(false) }

    LaunchedEffect(selectedPresetIndex) {
        if (!presetInit) {
            presetInit = true
            return@LaunchedEffect
        }
        if (selectedPresetIndex < 0) return@LaunchedEffect
        launch {
            pillStretchX.animateTo(0.95f, animationSpec = tween(durationMillis = 75))
            pillStretchX.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
        launch {
            pillStretchY.animateTo(1.10f, animationSpec = tween(durationMillis = 75))
            pillStretchY.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    val customRowAlpha by animateFloatAsState(
        targetValue = if (isCustomActive) 1f else 0.58f,
        animationSpec = tween(durationMillis = 200),
        label = "custom_days_row_alpha"
    )

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

        // Animated clay preset selector stack
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(itemGap)) {
                presets.forEach { preset ->
                    val selected = selectedRepeatType == preset
                    val borderColor by animateColorAsState(
                        targetValue = if (selected) Color.Transparent else MaterialTheme.colorScheme.outline,
                        label = "preset_border_${preset.name}"
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(itemHeight)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                    )
                }
            }

            if (pillAlpha > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .graphicsLayer {
                            translationY = pillOffsetY.toPx()
                            scaleX = pillStretchX.value
                            scaleY = pillStretchY.value
                            alpha = pillAlpha
                        }
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(itemGap)) {
                presets.forEach { preset ->
                    val selected = selectedRepeatType == preset
                    val textColor by animateColorAsState(
                        targetValue = if (selected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        label = "preset_text_${preset.name}"
                    )
                    val labelScale by animateFloatAsState(
                        targetValue = if (selected) 1.02f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "preset_scale_${preset.name}"
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(itemHeight)
                            .clip(RoundedCornerShape(16.dp))
                            .bounceClick { onRepeatSelected(preset) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = preset.label,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor,
                            modifier = Modifier.graphicsLayer {
                                scaleX = labelScale
                                scaleY = labelScale
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        SectionEyebrow("CUSTOM WEEKDAYS")

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(customRowAlpha),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            dayLetters.forEachIndexed { index, letter ->
                val active = isCustomActive && (selectedDaysMask and (1 shl index)) != 0
                val circleBg by animateColorAsState(
                    targetValue = if (active) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    label = "weekday_bg_$index"
                )
                val circleText by animateColorAsState(
                    targetValue = if (active) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    label = "weekday_text_$index"
                )
                val circleBorder by animateColorAsState(
                    targetValue = if (active) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                    label = "weekday_border_$index"
                )
                val circleScale by animateFloatAsState(
                    targetValue = if (active) 1.06f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "weekday_scale_$index"
                )

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .graphicsLayer {
                            scaleX = circleScale
                            scaleY = circleScale
                        }
                        .clip(CircleShape)
                        .background(circleBg)
                        .border(1.dp, circleBorder, CircleShape)
                        .bounceClick { onToggleDayBit(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = letter,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = circleText
                    )
                }
            }
        }

        Text(
            text = if (isCustomActive) {
                "Each selected day will use the reminder's chosen time."
            } else {
                "Tap any weekday above to set a custom weekly schedule."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        ClayButton(
            onClick = onUseSelectedWeekdays,
            enabled = isCustomActive && selectedDaysMask != 0,
            shape = RoundedCornerShape(16.dp),
            height = 52.dp,
            modifier = Modifier.fillMaxWidth()
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
