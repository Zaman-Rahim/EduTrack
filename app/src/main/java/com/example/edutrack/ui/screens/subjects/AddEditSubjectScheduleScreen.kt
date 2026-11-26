package com.example.edutrack.ui.screens.subjects

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edutrack.R
import com.example.edutrack.data.local.entity.SubjectEntity
import com.example.edutrack.data.local.entity.SubjectRepeatMode
import com.example.edutrack.ui.components.ConfirmDeleteDialog
import com.example.edutrack.ui.components.FigmaBackButton
import com.example.edutrack.ui.components.bounceClick
import com.example.edutrack.ui.theme.DeleteRedBg
import com.example.edutrack.ui.theme.DeleteRedText
import com.example.edutrack.ui.theme.ForestGreenDark
import com.example.edutrack.ui.theme.ForestGreenPrimary
import com.example.edutrack.ui.theme.LimeAccent
import com.example.edutrack.ui.theme.SubjectTagPalette
import com.example.edutrack.ui.viewmodel.SubjectFormState
import com.example.edutrack.util.DateTimeUtils

/**
 * Exact Figma `15:61` ("EduTrack · Add subject and schedule") screen supporting
 * daily goal hours/minutes, start date, repeat mode (Once / Every day / Custom days),
 * and 7-day interactive weekday chips.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val isEdit = formState.editingSubjectId != null

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFEDF2E8))
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                FigmaBackButton(onClick = onBackClick)
                Text(
                    text = if (isEdit) "Edit subject" else "Add a subject",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = ForestGreenDark
                )
            }
            if (isEdit && editingSubject != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(DeleteRedBg)
                        .bounceClick { showDeleteDialog = true }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Delete",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = DeleteRedText
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
            placeholder = { Text("Chemistry") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFF7FAF3),
                unfocusedContainerColor = Color(0xFFF7FAF3),
                focusedBorderColor = ForestGreenPrimary,
                unfocusedBorderColor = Color(0xFFCCD8C4)
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
                color = ForestGreenDark
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SubjectTagPalette.forEach { colorHex ->
                    val isSelected = formState.selectedColorHex == colorHex
                    Box(
                        modifier = Modifier
                            .size(34.dp)
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
                    color = ForestGreenDark
                )
                Text(
                    text = "On each scheduled day",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        focusedContainerColor = Color(0xFFE3EED5),
                        unfocusedContainerColor = Color(0xFFE3EED5),
                        focusedBorderColor = ForestGreenPrimary,
                        unfocusedBorderColor = Color(0xFFB9CDA7)
                    ),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = formState.dailyMinutesText,
                    onValueChange = onDailyMinutesChanged,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
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
                        focusedContainerColor = Color(0xFFE3EED5),
                        unfocusedContainerColor = Color(0xFFE3EED5),
                        focusedBorderColor = ForestGreenPrimary,
                        unfocusedBorderColor = Color(0xFFB9CDA7)
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
                color = ForestGreenDark
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
                        tint = ForestGreenDark,
                        modifier = Modifier.size(20.dp)
                    )
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF7FAF3),
                    unfocusedContainerColor = Color(0xFFF7FAF3),
                    focusedBorderColor = ForestGreenPrimary,
                    unfocusedBorderColor = Color(0xFFCCD8C4)
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
                color = ForestGreenDark
            )

            Surface(
                shape = RoundedCornerShape(15.dp),
                color = Color(0xFFDEE8D4),
                border = BorderStroke(1.dp, Color(0xFFC5D5B7)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val modes = listOf(
                        SubjectRepeatMode.ONCE to "Once",
                        SubjectRepeatMode.EVERY_DAY to "Every day",
                        SubjectRepeatMode.CUSTOM to "Custom days"
                    )
                    modes.forEach { (mode, label) ->
                        val selected = formState.repeatMode == mode
                        val bg by animateColorAsState(
                            targetValue = if (selected) ForestGreenPrimary else Color.Transparent,
                            label = "repeat_bg"
                        )
                        val textColor by animateColorAsState(
                            targetValue = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            label = "repeat_text"
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(bg)
                                .bounceClick { onRepeatModeSelected(mode) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                color = textColor
                            )
                        }
                    }
                }
            }

            // 7-day interactive weekday chips
            AnimatedVisibility(visible = formState.repeatMode == SubjectRepeatMode.CUSTOM) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        dayLabels.forEachIndexed { idx, label ->
                            val isOn = (formState.selectedDaysMask and (1 shl idx)) != 0
                            val chipBg by animateColorAsState(
                                targetValue = if (isOn) LimeAccent else Color(0xFFF2F6EC),
                                label = "day_chip_$idx"
                            )
                            Box(
                                modifier = Modifier
                                    .size(width = 44.dp, height = 42.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(chipBg)
                                    .border(1.dp, Color(0xFFC9D6BF), RoundedCornerShape(14.dp))
                                    .bounceClick { onToggleDayBit(idx) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isOn) FontWeight.SemiBold else FontWeight.Normal,
                                    color = ForestGreenDark
                                )
                            }
                        }
                    }

                    val offDays = dayLabels.filterIndexed { idx, _ ->
                        (formState.selectedDaysMask and (1 shl idx)) == 0
                    }
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

        // Calculated Weekly Target Banner (#DEEACF)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFDEEACF),
            border = BorderStroke(1.dp, Color(0xFFC2D5AE)),
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
                    color = ForestGreenDark
                )
                Text(
                    text = "${DateTimeUtils.formatDuration(formState.calculatedWeeklyMinutes)} / week",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = ForestGreenPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Save Subject CTA
        Button(
            onClick = onSaveClick,
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ForestGreenPrimary,
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .bounceClick(onClick = onSaveClick)
        ) {
            Text(
                text = if (isEdit) "Save changes" else "Save subject",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateTimeUtils.epochDayToUtcMillis(formState.startDateEpochDay)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onStartDateSelected(DateTimeUtils.utcMillisToEpochDay(millis))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Confirm", color = ForestGreenPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
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
