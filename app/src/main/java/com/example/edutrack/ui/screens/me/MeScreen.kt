package com.example.edutrack.ui.screens.me

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edutrack.BuildConfig
import com.example.edutrack.R
import com.example.edutrack.data.preferences.AlarmToneOption
import com.example.edutrack.data.preferences.UserPreferencesState
import com.example.edutrack.ui.components.FigmaBackButton
import com.example.edutrack.ui.components.bounceClick
import com.example.edutrack.ui.theme.CardLightGreen
import com.example.edutrack.ui.theme.ForestGreenDark
import com.example.edutrack.ui.theme.ForestGreenPrimary
import com.example.edutrack.ui.theme.LimeAccent
import com.example.edutrack.ui.theme.TrackBorder
import com.example.edutrack.ui.viewmodel.RemindersViewModel

/**
 * Exact Figma `19:129` ("EduTrack · Me") personal learning space, study preferences,
 * timer sound & reminder settings, theme switch, and About EduTrack dialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeScreen(
    currentStreakDays: Int,
    unreadNotificationsCount: Int,
    prefs: UserPreferencesState,
    isDarkTheme: Boolean,
    onBackClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onViewProgressClick: () -> Unit,
    onMySchedulesClick: () -> Unit,
    onDailyReminderToggle: (Boolean) -> Unit,
    onReminderTimeChanged: (Int) -> Unit,
    onTimerSoundToggle: (Boolean) -> Unit,
    onToggleWeekStart: () -> Unit,
    onToggleDarkTheme: (Boolean) -> Unit,
    onAlarmToneSelected: (AlarmToneOption) -> Unit = {},
    onPreviewAlarmTone: (AlarmToneOption) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showReminderTimePicker by remember { mutableStateOf(false) }
    var showAlarmToneDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    FigmaBackButton(onClick = onBackClick)
                    Text(
                        text = "Me",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .bounceClick(onClick = onNotificationsClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_figma_bell),
                        contentDescription = "Notifications inbox",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                    if (unreadNotificationsCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 8.dp, end = 8.dp)
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(ForestGreenPrimary)
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Make room for the way you learn.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Personal Learning Space Hero Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = ForestGreenPrimary)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Your learning space",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "On this device · No account needed",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFD3E0D5)
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(13.dp),
                            color = LimeAccent
                        ) {
                            Text(
                                text = "$currentStreakDays-day streak",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = ForestGreenPrimary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.bounceClick(onClick = onViewProgressClick)
                        ) {
                            Text(
                                text = "View progress",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                            Icon(
                                painter = painterResource(id = R.drawable.ic_figma_arrow),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // MY ROUTINE
        item {
            Text(
                text = "MY ROUTINE",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.8.sp
            )
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick(onClick = onMySchedulesClick),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_figma_book),
                        contentDescription = null,
                        tint = ForestGreenDark,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "My subject schedules",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Daily goals, repeat days & days off",
                            style = MaterialTheme.typography.bodySmall,
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

        // STUDY PREFERENCES
        item {
            Text(
                text = "STUDY PREFERENCES",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.8.sp
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClick { showReminderTimePicker = true }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Daily study reminder",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = RemindersViewModel.formatTimeOfDay(prefs.dailyReminderTimeMinutes),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = prefs.dailyReminderEnabled,
                            onCheckedChange = onDailyReminderToggle,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = LimeAccent,
                                checkedTrackColor = ForestGreenPrimary
                            )
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = TrackBorder)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Timer completion sound",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = prefs.timerCompletionSound,
                            onCheckedChange = onTimerSoundToggle,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = LimeAccent,
                                checkedTrackColor = ForestGreenPrimary
                            )
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = TrackBorder)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClick { showAlarmToneDialog = true }
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Alarm tone",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = AlarmToneOption.fromName(prefs.alarmTone).label,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(
                                painter = painterResource(id = R.drawable.ic_figma_arrow),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = TrackBorder)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClick(onClick = onToggleWeekStart)
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Week starts on",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = prefs.weekStartsOn,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(
                                painter = painterResource(id = R.drawable.ic_figma_arrow),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = TrackBorder)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Dark appearance",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = onToggleDarkTheme,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = LimeAccent,
                                checkedTrackColor = ForestGreenPrimary
                            )
                        )
                    }
                }
            }
        }

        // About EduTrack Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick { showAboutDialog = true },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CardLightGreen)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_figma_info),
                        contentDescription = null,
                        tint = ForestGreenPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "About EduTrack",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = ForestGreenDark,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.ic_figma_arrow),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        item {
            Text(
                text = "Your study records stay on this device.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showReminderTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = prefs.dailyReminderTimeMinutes / 60,
            initialMinute = prefs.dailyReminderTimeMinutes % 60
        )
        AlertDialog(
            onDismissRequest = { showReminderTimePicker = false },
            title = { Text("Daily study reminder", fontWeight = FontWeight.SemiBold) },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onReminderTimeChanged(timeState.hour * 60 + timeState.minute)
                        showReminderTimePicker = false
                    }
                ) {
                    Text("Save", color = ForestGreenPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReminderTimePicker = false }) { Text("Cancel") }
            }
        )
    }

    if (showAlarmToneDialog) {
        var selectedTone by remember(prefs.alarmTone) {
            mutableStateOf(AlarmToneOption.fromName(prefs.alarmTone))
        }
        AlertDialog(
            onDismissRequest = { showAlarmToneDialog = false },
            title = { Text("Alarm tone", fontWeight = FontWeight.SemiBold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    AlarmToneOption.entries.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    selectedTone = option
                                    onPreviewAlarmTone(option)
                                }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedTone == option,
                                onClick = {
                                    selectedTone = option
                                    onPreviewAlarmTone(option)
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = ForestGreenPrimary
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (selectedTone == option) FontWeight.SemiBold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onAlarmToneSelected(selectedTone)
                        showAlarmToneDialog = false
                    }
                ) {
                    Text("Save", color = ForestGreenPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAlarmToneDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About EduTrack", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "EduTrack v1.0\n\n" +
                        "An offline-first personal study routine tracker built with Kotlin, " +
                        "Jetpack Compose (Material 3), Room Database, and MVVM architecture."
                )
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Done", color = ForestGreenPrimary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
