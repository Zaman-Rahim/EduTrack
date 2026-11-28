package com.example.edutrack.ui.screens.subjects

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edutrack.R
import com.example.edutrack.data.local.entity.SubjectEntity
import com.example.edutrack.data.local.entity.SubjectRepeatMode
import com.example.edutrack.ui.components.ClayButton
import com.example.edutrack.ui.components.ClaySegmentedControl
import com.example.edutrack.ui.components.ConfirmDeleteDialog
import com.example.edutrack.ui.components.EmptyStateCard
import com.example.edutrack.ui.components.FigmaBackButton
import com.example.edutrack.ui.components.FigmaCustomDateTimeScreen
import com.example.edutrack.ui.components.bounceClick
import com.example.edutrack.ui.theme.ForestGreenDark
import com.example.edutrack.ui.theme.LimeAccent
import com.example.edutrack.ui.theme.SubjectTagPalette
import com.example.edutrack.ui.viewmodel.SubjectFormState
import com.example.edutrack.util.DateTimeUtils

/**
 * Add/Edit subject and schedule screen supporting
 * daily goal hours/minutes, start date, repeat mode (Once / Every day / Custom days),
 * and 7-day interactive weekday chips.
 */
@Composable
fun AddEditSubjectScheduleScreen(
    formState: SubjectFormState,
    editingSubject: SubjectEntity?,
    onBackClick: () -> Unit,
    onNameChanged: (String) -> Unit,
    onDailyHoursChanged: (String) -> Unit,
    onDailyMinutesChanged: (String) -> Unit,
    onStartDateSelected: (Long) -> Unit,
    onRepeatModeSelected: (SubjectRepeatMode) -> Unit,
    onToggleDayBit: (Int) -> Unit,
    onColorSelected: (Long) -> Unit,
    onSaveClick: () -> Unit,
    onDeleteClick: (SubjectEntity) -> Unit,
    modifier: Modifier = Modifier,
    weekStartsOn: String = "Monday"
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDatePicker) {
        BackHandler(enabled = true) {
            showDatePicker = false
        }
        FigmaCustomDateTimeScreen(
            title = "Select start date",
            confirmLabel = "Use start date",
            initialEpochDay = formState.startDateEpochDay,
            showTimeSection = false,
            allowPastDates = true,
            onBackClick = { showDatePicker = false },
            onConfirm = { epochDay, _ ->
                onStartDateSelected(epochDay)
                showDatePicker = false
            },
            weekStartsOn = weekStartsOn
        )
        return
    }

    val isEdit = formState.editingSubjectId != null

    if (formState.isLoading) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                FigmaBackButton(onClick = onBackClick)
                Text(
                    text = if (isEdit) "Edit subject" else "Add a subject",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
        return
    }

    val notFoundError = formState.notFoundError
    if (notFoundError != null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                FigmaBackButton(onClick = onBackClick)
                Text(
                    text = "Edit subject",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            EmptyStateCard(
                iconRes = R.drawable.ic_figma_info,
                title = "Subject not found",
                description = notFoundError,
                actionLabel = "Go Back",
                onActionClick = onBackClick
            )
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                FigmaBackButton(onClick = onBackClick)
                Text(
                    text = if (isEdit) "Edit subject" else "Add a subject",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (isEdit && editingSubject != null) {
                Spacer(modifier = Modifier.width(8.dp))
                ClayButton(
                    onClick = { showDeleteDialog = true },
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = RoundedCornerShape(12.dp),
                    height = 38.dp,
                    contentPadding = PaddingValues(horizontal = 14.dp)
                ) {
                    Text(
                        text = "Delete",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        Text(
            text = "Turn a learning goal into a daily habit.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Subject Name
        OutlinedTextField(
            value = formState.name,
            onValueChange = onNameChanged,
            label = { Text("Subject name") },
            placeholder = { Text("Enter subject name") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            isError = formState.nameError != null,
            supportingText = formState.nameError?.let { err -> { Text(err) } },
            modifier = Modifier.fillMaxWidth()
        )

        // Colour tag row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Colour tag",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SubjectTagPalette.forEach { colorHex ->
                    val isSelected = formState.selectedColorHex == colorHex
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(colorHex))
                            .bounceClick { onColorSelected(colorHex) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_figma_check),
                                contentDescription = "Selected",
                                tint = ForestGreenDark,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Daily study goal (Hours + Minutes side-by-side)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daily study goal",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "On each scheduled day",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = formState.dailyHoursText,
                    onValueChange = onDailyHoursChanged,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = formState.goalError != null,
                    textStyle = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 26.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    trailingIcon = {
                        Text(
                            text = "hour",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 14.dp)
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = formState.dailyMinutesText,
                    onValueChange = onDailyMinutesChanged,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = formState.goalError != null,
                    textStyle = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 26.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    trailingIcon = {
                        Text(
                            text = "min",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 14.dp)
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            if (formState.goalError != null) {
                Text(
                    text = formState.goalError,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        // Start date picker
        val dateInteractionSource = remember { MutableInteractionSource() }
        LaunchedEffect(dateInteractionSource) {
            dateInteractionSource.interactions.collect { interaction ->
                if (interaction is PressInteraction.Release) {
                    showDatePicker = true
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Start date",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            OutlinedTextField(
                value = DateTimeUtils.formatFullDate(formState.startDateEpochDay),
                onValueChange = {},
                readOnly = true,
                interactionSource = dateInteractionSource,
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_figma_calendar),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_figma_down),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Repeat selector (Once / Every day / Custom days)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Repeat",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            val repeatModes = remember {
                listOf(
                    SubjectRepeatMode.ONCE,
                    SubjectRepeatMode.EVERY_DAY,
                    SubjectRepeatMode.CUSTOM
                )
            }
            val repeatOptions = remember { listOf("Once", "Every day", "Custom days") }
            val selectedRepeatIndex = repeatModes.indexOf(formState.repeatMode).coerceAtLeast(0)
            ClaySegmentedControl(
                options = repeatOptions,
                selectedIndex = selectedRepeatIndex,
                onOptionSelected = { index ->
                    onRepeatModeSelected(repeatModes[index])
                }
            )

            // 7-day interactive weekday chips (responsive weight + weekStartsOn order)
            AnimatedVisibility(visible = formState.repeatMode == SubjectRepeatMode.CUSTOM) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val orderedDays = remember(weekStartsOn) {
                        val monToSun = listOf(
                            0 to "Mon",
                            1 to "Tue",
                            2 to "Wed",
                            3 to "Thu",
                            4 to "Fri",
                            5 to "Sat",
                            6 to "Sun"
                        )
                        if (weekStartsOn.equals("Sunday", ignoreCase = true)) {
                            listOf(monToSun.last()) + monToSun.dropLast(1)
                        } else {
                            monToSun
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        orderedDays.forEach { (bitIdx, label) ->
                            val isOn = (formState.selectedDaysMask and (1 shl bitIdx)) != 0
                            val chipBg by animateColorAsState(
                                targetValue = if (isOn) LimeAccent else MaterialTheme.colorScheme.surface,
                                label = "day_chip_$bitIdx"
                            )
                            val borderColor = if (isOn) LimeAccent else MaterialTheme.colorScheme.outline
                            val textColor = if (isOn) ForestGreenDark else MaterialTheme.colorScheme.onSurface
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(chipBg)
                                    .border(1.dp, borderColor, RoundedCornerShape(14.dp))
                                    .bounceClick { onToggleDayBit(bitIdx) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isOn) FontWeight.SemiBold else FontWeight.Normal,
                                    color = textColor,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    if (formState.scheduleError != null) {
                        Text(
                            text = formState.scheduleError,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    val offDays = orderedDays
                        .filter { (bitIdx, _) -> (formState.selectedDaysMask and (1 shl bitIdx)) == 0 }
                        .map { it.second }
                    val offSummary = when {
                        offDays.isEmpty() -> "Repeats every day of the week."
                        offDays.size == 7 -> "Select at least one day to repeat."
                        else -> "Repeats weekly · ${offDays.joinToString(", ")} off."
                    }
                    Text(
                        text = offSummary,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "Choose Once for a task on the start date only.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Calculated Weekly Target Banner
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val dailyFmt = DateTimeUtils.formatDuration(formState.dailyTotalMinutes)
                val leftText = if (formState.repeatMode == SubjectRepeatMode.ONCE) {
                    "$dailyFmt · 1 day only"
                } else {
                    "$dailyFmt × ${formState.activeDaysCount} study days"
                }
                Text(
                    text = leftText,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${DateTimeUtils.formatDuration(formState.calculatedWeeklyMinutes)} / week",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Save Subject CTA
        ClayButton(
            onClick = onSaveClick,
            enabled = !formState.isSaving,
            shape = RoundedCornerShape(18.dp),
            height = 56.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = when {
                    formState.isSaving -> "Saving..."
                    isEdit -> "Save changes"
                    else -> "Save subject"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }

    if (showDeleteDialog && editingSubject != null) {
        ConfirmDeleteDialog(
            title = "Delete \"${editingSubject.name}\"?",
            message = "Deleting this subject will also delete all study sessions associated with it.",
            onConfirm = {
                showDeleteDialog = false
                onDeleteClick(editingSubject)
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}
