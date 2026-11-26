package com.example.edutrack.ui.screens.subjects

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edutrack.R
import com.example.edutrack.data.local.entity.SubjectEntity
import com.example.edutrack.ui.components.ConfirmDeleteDialog
import com.example.edutrack.ui.components.EmptyStateCard
import com.example.edutrack.ui.components.figmaSubjectIconRes
import com.example.edutrack.ui.theme.DeleteRedBg
import com.example.edutrack.ui.theme.DeleteRedText
import com.example.edutrack.ui.theme.ForestGreenDark
import com.example.edutrack.ui.theme.ForestGreenPrimary
import com.example.edutrack.ui.theme.SubjectTagPalette
import com.example.edutrack.ui.theme.TrackBorder
import com.example.edutrack.ui.theme.subjectPastelContainer
import com.example.edutrack.ui.viewmodel.SubjectFormState
import com.example.edutrack.ui.viewmodel.SubjectItemUi
import com.example.edutrack.ui.viewmodel.SubjectManagerUiState
import java.util.Locale

@Composable
fun SubjectManagerScreen(
    uiState: SubjectManagerUiState,
    onEditClick: (SubjectEntity) -> Unit,
    onDeleteSubject: (SubjectEntity) -> Unit,
    onDismissDialog: () -> Unit,
    onNameChanged: (String) -> Unit,
    onWeeklyGoalChanged: (String) -> Unit,
    onColorSelected: (Long) -> Unit,
    onSaveSubject: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pendingDelete by remember { mutableStateOf<SubjectEntity?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Exact Figma Header
        item {
            Column(modifier = Modifier.padding(bottom = 6.dp)) {
                Text(
                    text = "Your subjects",
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Make space for what you want to learn.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (!uiState.isLoading && uiState.subjects.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.AutoMirrored.Outlined.MenuBook,
                    title = "No subjects yet",
                    description = "Use the form below to add your first course with a color tag and weekly goal."
                )
            }
        } else {
            items(uiState.subjects, key = { "manager_subject_${it.subject.id}" }) { item ->
                FigmaSubjectRowCard(
                    item = item,
                    onEditClick = { onEditClick(item.subject) },
                    onDeleteClick = { pendingDelete = item.subject }
                )
            }
        }

        // Exact Figma Inline "Add a subject" Section (when not editing in modal)
        if (!uiState.formState.isVisible) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                InlineAddSubjectSection(
                    formState = uiState.formState,
                    onNameChanged = onNameChanged,
                    onWeeklyGoalChanged = onWeeklyGoalChanged,
                    onColorSelected = onColorSelected,
                    onAddClick = onSaveSubject
                )
            }
        }
    }

    if (uiState.formState.isVisible) {
        SubjectFormDialog(
            formState = uiState.formState,
            onNameChanged = onNameChanged,
            onWeeklyGoalChanged = onWeeklyGoalChanged,
            onColorSelected = onColorSelected,
            onConfirm = onSaveSubject,
            onDismiss = onDismissDialog
        )
    }

    pendingDelete?.let { subject ->
        ConfirmDeleteDialog(
            title = "Delete \"${subject.name}\"?",
            message = "Deleting this subject will also delete all study sessions associated with it.",
            onConfirm = {
                pendingDelete = null
                onDeleteSubject(subject)
            },
            onDismiss = { pendingDelete = null }
        )
    }
}

@Composable
private fun FigmaSubjectRowCard(
    item: SubjectItemUi,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val containerBg = subjectPastelContainer(item.subject.colorHex)
    val weeklyTargetHours = maxOf(0.1, item.subject.weeklyGoalHours)
    val loggedHours = item.totalMinutesLogged / 60.0
    val progressFraction = (loggedHours / weeklyTargetHours).coerceIn(0.0, 1.0).toFloat()

    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        label = "subject_card_progress"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(containerBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = figmaSubjectIconRes(item.subject.name)),
                        contentDescription = null,
                        tint = ForestGreenDark,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.subject.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = String.format(
                            Locale.getDefault(),
                            "%.1fh / %.1fh goal • %d session(s)",
                            loggedHours,
                            item.subject.weeklyGoalHours,
                            item.sessionCount
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Exact Figma 36dp circular Edit button on #F6F7F2 background
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.background)
                        .clickable(onClick = onEditClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_figma_edit),
                        contentDescription = "Edit subject",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(DeleteRedBg)
                        .clickable(onClick = onDeleteClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete subject",
                        tint = DeleteRedText,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 60.dp, end = 6.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = ForestGreenPrimary,
                trackColor = TrackBorder,
                strokeCap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun InlineAddSubjectSection(
    formState: SubjectFormState,
    onNameChanged: (String) -> Unit,
    onWeeklyGoalChanged: (String) -> Unit,
    onColorSelected: (Long) -> Unit,
    onAddClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column {
            Text(
                text = "Add a subject",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Give your next goal a home.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        OutlinedTextField(
            value = formState.name,
            onValueChange = onNameChanged,
            label = { Text("Subject name") },
            placeholder = { Text("Chemistry") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = ForestGreenPrimary,
                unfocusedBorderColor = Color.Transparent
            ),
            isError = formState.nameError != null,
            supportingText = formState.nameError?.let { error -> { Text(error) } },
            modifier = Modifier.fillMaxWidth()
        )

        // Exact Figma Colour tag row with 5 pastel swatches
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
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(colorHex))
                            .clickable { onColorSelected(colorHex) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_figma_check),
                                contentDescription = "Selected color",
                                tint = ForestGreenDark,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            value = formState.weeklyGoalHoursText,
            onValueChange = onWeeklyGoalChanged,
            label = { Text("Weekly goal") },
            placeholder = { Text("4") },
            trailingIcon = {
                Text(
                    text = "hrs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 16.dp)
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = ForestGreenPrimary,
                unfocusedBorderColor = Color.Transparent
            ),
            isError = formState.goalError != null,
            supportingText = formState.goalError?.let { error -> { Text(error) } },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = onAddClick,
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ForestGreenPrimary,
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_figma_plus),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Add subject",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubjectFormDialog(
    formState: SubjectFormState,
    onNameChanged: (String) -> Unit,
    onWeeklyGoalChanged: (String) -> Unit,
    onColorSelected: (Long) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (formState.editingSubjectId != null) "Edit Subject" else "Add a subject",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = formState.name,
                    onValueChange = onNameChanged,
                    label = { Text("Subject Name *") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = ForestGreenPrimary,
                        unfocusedBorderColor = TrackBorder
                    ),
                    isError = formState.nameError != null,
                    supportingText = formState.nameError?.let { error -> { Text(error) } },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = formState.weeklyGoalHoursText,
                    onValueChange = onWeeklyGoalChanged,
                    label = { Text("Weekly Goal (Hours) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = ForestGreenPrimary,
                        unfocusedBorderColor = TrackBorder
                    ),
                    isError = formState.goalError != null,
                    supportingText = formState.goalError?.let { error -> { Text(error) } },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Colour tag",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SubjectTagPalette.forEach { colorHex ->
                        val isSelected = formState.selectedColorHex == colorHex
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(colorHex))
                                .clickable { onColorSelected(colorHex) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_figma_check),
                                    contentDescription = "Selected color",
                                    tint = ForestGreenDark,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ForestGreenPrimary,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = if (formState.editingSubjectId != null) "Update" else "Add subject",
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
