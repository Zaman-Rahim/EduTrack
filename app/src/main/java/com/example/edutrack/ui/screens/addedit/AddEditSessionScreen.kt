package com.example.edutrack.ui.screens.addedit

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edutrack.R
import com.example.edutrack.ui.components.ClayButton
import com.example.edutrack.ui.components.ClaySegmentedControl
import com.example.edutrack.ui.components.EmptyStateCard
import com.example.edutrack.ui.components.FigmaBackButton
import com.example.edutrack.ui.theme.subjectPastelContainer
import com.example.edutrack.ui.viewmodel.AddEditSessionUiState
import com.example.edutrack.util.DateTimeUtils
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSessionScreen(
    uiState: AddEditSessionUiState,
    onBackClick: () -> Unit,
    onSubjectSelected: (Long) -> Unit,
    onDurationChanged: (String) -> Unit,
    onDateSelected: (Long) -> Unit,
    onNotesChanged: (String) -> Unit,
    onSaveClick: () -> Unit,
    onManageSubjectsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var subjectMenuExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val selectedSubject = uiState.subjects.firstOrNull { it.id == uiState.selectedSubjectId }
    val durationPresets = remember { listOf(25, 45, 60) }
    val presetLabels = remember(durationPresets) {
        durationPresets.map { DateTimeUtils.formatDuration(it) } + "Custom"
    }
    val durationFocusRequester = remember { FocusRequester() }
    val selectedPresetIndex = remember(uiState.durationText) {
        val parsed = uiState.durationText.trim().toIntOrNull()
        val matchedIdx = durationPresets.indexOf(parsed)
        if (matchedIdx >= 0) matchedIdx else durationPresets.size
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Exact Figma Header: 42dp circular back button + Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            FigmaBackButton(onClick = onBackClick)
            Text(
                text = if (uiState.isEditMode) "Edit study session" else "Log study session",
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        val notFoundError = uiState.notFoundError
        if (notFoundError != null) {
            EmptyStateCard(
                iconRes = R.drawable.ic_figma_info,
                title = "Study session not found",
                description = notFoundError,
                actionLabel = "Go Back",
                onActionClick = onBackClick
            )
            return@Column
        }

        Text(
            text = "Every focused minute counts.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (uiState.subjects.isEmpty()) {
            EmptyStateCard(
                iconRes = R.drawable.ic_figma_book,
                title = "No subjects available",
                description = "Add at least one subject before logging a study session.",
                actionLabel = "Go to Subjects",
                onActionClick = onManageSubjectsClick
            )
            return@Column
        }

        Text(
            text = "SESSION DETAILS",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.8.sp
        )

        // Subject selector dropdown (16dp rounded card with 24dp 8dp-rounded swatch)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Subject",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            ExposedDropdownMenuBox(
                expanded = subjectMenuExpanded,
                onExpandedChange = { subjectMenuExpanded = !subjectMenuExpanded }
            ) {
                OutlinedTextField(
                    value = selectedSubject?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    placeholder = { Text("Choose a subject") },
                    leadingIcon = if (selectedSubject != null) {
                        {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(subjectPastelContainer(selectedSubject.colorHex))
                            )
                        }
                    } else {
                        null
                    },
                    trailingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_figma_down),
                            contentDescription = "Select subject",
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
                    isError = uiState.subjectError != null,
                    supportingText = uiState.subjectError?.let { error ->
                        { Text(error) }
                    },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = subjectMenuExpanded,
                    onDismissRequest = { subjectMenuExpanded = false }
                ) {
                    uiState.subjects.forEach { subject ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(subjectPastelContainer(subject.colorHex))
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(subject.name)
                                }
                            },
                            onClick = {
                                onSubjectSelected(subject.id)
                                subjectMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Duration input + sliding Clay quick presets
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Duration",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            OutlinedTextField(
                value = uiState.durationText,
                onValueChange = onDurationChanged,
                placeholder = { Text("45") },
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 30.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                trailingIcon = {
                    Text(
                        text = "minutes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 18.dp)
                    )
                },
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                isError = uiState.durationError != null,
                supportingText = {
                    if (uiState.durationError != null) {
                        Text(uiState.durationError)
                    } else {
                        val preview = uiState.durationText.toIntOrNull()?.takeIf { it > 0 }
                            ?.let { "Formatted: ${DateTimeUtils.formatDuration(it)}" }
                            ?: "Enter duration between 1 and 1440 minutes"
                        Text(preview)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(durationFocusRequester)
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Quick add",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ClaySegmentedControl(
                    options = presetLabels,
                    selectedIndex = selectedPresetIndex,
                    onOptionSelected = { index ->
                        if (index in durationPresets.indices) {
                            onDurationChanged(durationPresets[index].toString())
                        } else {
                            if (uiState.durationText.trim().toIntOrNull() in durationPresets) {
                                onDurationChanged("")
                            }
                            durationFocusRequester.requestFocus()
                        }
                    }
                )
            }
        }

        // Date field using exact Figma calendar + down icons
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
                text = "Study date",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            OutlinedTextField(
                value = DateTimeUtils.formatFullDate(uiState.sessionDateEpochDay),
                onValueChange = {},
                readOnly = true,
                interactionSource = dateInteractionSource,
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_figma_calendar),
                        contentDescription = "Pick study date",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
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
                isError = uiState.dateError != null,
                supportingText = uiState.dateError?.let { error -> { Text(error) } },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Notes field
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Notes",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Required",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedTextField(
                value = uiState.notes,
                onValueChange = onNotesChanged,
                placeholder = { Text("What did you work on?") },
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                isError = uiState.notesError != null,
                supportingText = {
                    if (uiState.notesError != null) {
                        Text(uiState.notesError)
                    } else {
                        Text("Add a topic or a small win to remember.")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        ClayButton(
            onClick = onSaveClick,
            enabled = !uiState.isSaving,
            shape = RoundedCornerShape(18.dp),
            height = 56.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = when {
                    uiState.isSaving -> "Saving..."
                    uiState.isEditMode -> "Save changes"
                    else -> "Save session"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }

    if (showDatePicker) {
        val todayEpoch = DateTimeUtils.todayEpochDay()
        val todayUtcMillis = remember(todayEpoch) { DateTimeUtils.epochDayToUtcMillis(todayEpoch) }
        val maxYear = remember { LocalDate.now().year }
        val clampedInitialEpoch = uiState.sessionDateEpochDay.coerceAtMost(todayEpoch)
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateTimeUtils.epochDayToUtcMillis(clampedInitialEpoch),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis <= todayUtcMillis

                override fun isSelectableYear(year: Int): Boolean =
                    year <= maxYear
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val pickedEpoch = DateTimeUtils.utcMillisToEpochDay(millis)
                            if (pickedEpoch <= todayEpoch) {
                                onDateSelected(pickedEpoch)
                            }
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(
                        text = "Confirm",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
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
}
