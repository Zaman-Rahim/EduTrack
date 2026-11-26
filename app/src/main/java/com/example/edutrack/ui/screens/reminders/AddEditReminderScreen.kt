package com.example.edutrack.ui.screens.reminders

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edutrack.data.local.entity.ReminderPriority
import com.example.edutrack.data.local.entity.ReminderRepeatType
import com.example.edutrack.ui.components.bounceClick
import com.example.edutrack.ui.theme.ForestGreenDark
import com.example.edutrack.ui.theme.ForestGreenPrimary
import com.example.edutrack.ui.theme.LimeAccent
import com.example.edutrack.ui.viewmodel.ReminderEditorState
import com.example.edutrack.ui.viewmodel.RemindersViewModel
import com.example.edutrack.util.DateTimeUtils

/**
 * Exact Figma `24:45` ("Reminder editor") screen for creating or editing a reminder.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditReminderScreen(
    state: ReminderEditorState,
    onBackClick: () -> Unit,
    onTitleChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    onDueDateSelected: (Long) -> Unit,
    onDueTimeSelected: (Int) -> Unit,
    onRepeatSelected: (ReminderRepeatType) -> Unit,
    onPrioritySelected: (ReminderPriority) -> Unit,
    onNotifyToggle: (Boolean) -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showRepeatMenu by remember { mutableStateOf(false) }

    val isEdit = state.editingId != null

    Column(
        modifier = modifier
            .fillMaxSize()
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

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "TASK TITLE",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.8.sp
            )
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

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "DATE & TIME",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.8.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(15.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, Color(0xFFCCD8C4)),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .bounceClick { showDatePicker = true }
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 15.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = DateTimeUtils.formatShortDate(state.dueDateEpochDay),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(15.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, Color(0xFFCCD8C4)),
                    modifier = Modifier
                        .weight(0.8f)
                        .height(54.dp)
                        .bounceClick { showTimePicker = true }
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 15.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = RemindersViewModel.formatTimeOfDay(state.dueTimeMinutes),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Repeat selector
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "REPEAT",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.8.sp
            )
            Box {
                Surface(
                    shape = RoundedCornerShape(15.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, Color(0xFFCCD8C4)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .bounceClick { showRepeatMenu = true }
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 15.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = state.repeatType.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                DropdownMenu(
                    expanded = showRepeatMenu,
                    onDismissRequest = { showRepeatMenu = false }
                ) {
                    ReminderRepeatType.entries.forEach { repeat ->
                        DropdownMenuItem(
                            text = { Text(repeat.label) },
                            onClick = {
                                onRepeatSelected(repeat)
                                showRepeatMenu = false
                            }
                        )
                    }
                }
            }
        }

        // Priority buttons (Low | Normal | High)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "PRIORITY",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.8.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ReminderPriority.entries.forEach { priority ->
                    val selected = state.priority == priority
                    val bg by animateColorAsState(
                        targetValue = if (selected) ForestGreenPrimary else MaterialTheme.colorScheme.surface,
                        label = "priority_bg"
                    )
                    val txt by animateColorAsState(
                        targetValue = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                        label = "priority_txt"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(bg)
                            .bounceClick { onPrioritySelected(priority) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = priority.label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = txt
                        )
                    }
                }
            }
        }

        // Notification toggle row
        Surface(
            shape = RoundedCornerShape(15.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Notification",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (state.notifyEnabled) "On · at time of reminder" else "Muted",
                        style = MaterialTheme.typography.labelSmall,
                        color = ForestGreenPrimary
                    )
                }
                Switch(
                    checked = state.notifyEnabled,
                    onCheckedChange = onNotifyToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = LimeAccent,
                        checkedTrackColor = ForestGreenPrimary
                    )
                )
            }
        }

        // Notes
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "NOTES",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.8.sp
            )
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
                text = if (isEdit) "Save changes" else "Save reminder",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateTimeUtils.epochDayToUtcMillis(state.dueDateEpochDay)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let {
                            onDueDateSelected(DateTimeUtils.utcMillisToEpochDay(it))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Confirm", color = ForestGreenPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = state.dueTimeMinutes / 60,
            initialMinute = state.dueTimeMinutes % 60
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Reminder time", fontWeight = FontWeight.SemiBold) },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDueTimeSelected(timeState.hour * 60 + timeState.minute)
                        showTimePicker = false
                    }
                ) {
                    Text("Confirm", color = ForestGreenPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            }
        )
    }
}
