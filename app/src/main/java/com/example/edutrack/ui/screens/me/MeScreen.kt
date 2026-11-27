package com.example.edutrack.ui.screens.me

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.edutrack.BuildConfig
import com.example.edutrack.R
import com.example.edutrack.data.preferences.UserPreferencesState
import com.example.edutrack.ui.components.ClayButton
import com.example.edutrack.ui.components.ClayOutlinedButton
import com.example.edutrack.ui.components.ClaySegmentedControl
import com.example.edutrack.ui.components.FigmaBackButton
import com.example.edutrack.ui.components.WheelTimePickerDialog
import com.example.edutrack.ui.components.bounceClick
import com.example.edutrack.ui.theme.ForestGreenPrimary
import com.example.edutrack.ui.theme.LimeAccent
import com.example.edutrack.ui.viewmodel.RemindersViewModel
import com.example.edutrack.util.SystemSoundHelper
import com.example.edutrack.util.SystemSoundItem
import com.example.edutrack.util.SystemToneFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class SoundToneTarget(val title: String, val defaultFilter: SystemToneFilter, val ringtoneType: Int) {
    TIMER("Focus timer alarm", SystemToneFilter.ALARM, RingtoneManager.TYPE_ALARM),
    REMINDER("Reminder alert tone", SystemToneFilter.NOTIFICATION, RingtoneManager.TYPE_NOTIFICATION),
    NOTIFICATION("Notification sound", SystemToneFilter.NOTIFICATION, RingtoneManager.TYPE_NOTIFICATION)
}

/**
 * Exact Figma `19:129` ("EduTrack · Me") personal learning space, study preferences,
 * timer sound & reminder settings, theme switch, and About EduTrack dialog.
 */
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
    onTimerToneSelected: (uri: String, label: String) -> Unit = { _, _ -> },
    onReminderToneSelected: (uri: String, label: String) -> Unit = { _, _ -> },
    onNotificationToneSelected: (uri: String, label: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var showReminderTimePicker by remember { mutableStateOf(false) }
    var activeToneTarget by remember { mutableStateOf<SoundToneTarget?>(null) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val switchColors = SwitchDefaults.colors(
        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
        checkedTrackColor = MaterialTheme.colorScheme.primary,
        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
        uncheckedBorderColor = MaterialTheme.colorScheme.outline
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item(key = "header", contentType = "header") {
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
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
        }

        item(key = "subtitle", contentType = "subtitle") {
            Text(
                text = "Make room for the way you learn.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Personal Learning Space Hero Card
        item(key = "hero_card", contentType = "hero_card") {
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
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .bounceClick(onClick = onViewProgressClick)
                                .padding(horizontal = 6.dp, vertical = 4.dp)
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
        item(key = "routine_label", contentType = "section_label") {
            Text(
                text = "MY ROUTINE",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.8.sp
            )
        }

        item(key = "my_schedules", contentType = "nav_card") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
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
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
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
        item(key = "prefs_label", contentType = "section_label") {
            Text(
                text = "STUDY PREFERENCES",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.8.sp
            )
        }

        item(key = "prefs_card", contentType = "settings_card") {
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
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Switch(
                            checked = prefs.dailyReminderEnabled,
                            onCheckedChange = onDailyReminderToggle,
                            colors = switchColors
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline
                    )

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
                            colors = switchColors
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Week starts on",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val weekStartOptions = remember { listOf("Mon", "Sun") }
                        val selectedWeekIndex = if (prefs.weekStartsOn.equals("Sunday", ignoreCase = true)) 1 else 0
                        ClaySegmentedControl(
                            options = weekStartOptions,
                            selectedIndex = selectedWeekIndex,
                            onOptionSelected = { index ->
                                if (index != selectedWeekIndex) {
                                    onToggleWeekStart()
                                }
                            },
                            modifier = Modifier
                                .width(132.dp)
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline
                    )

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
                            colors = switchColors
                        )
                    }
                }
            }
        }

        // SOUND & ALERT TONES (3 separate customizable system sound sections)
        item(key = "tones_label", contentType = "section_label") {
            Text(
                text = "SOUND & ALERT TONES",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.8.sp
            )
        }

        item(key = "tones_card", contentType = "tones_card") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SoundToneRow(
                        title = "Focus timer alarm",
                        subtitle = "Plays when study countdown hits 00:00",
                        selectedLabel = prefs.timerToneLabel,
                        onClick = { activeToneTarget = SoundToneTarget.TIMER }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline
                    )
                    SoundToneRow(
                        title = "Reminder alert tone",
                        subtitle = "Plays for scheduled task reminders",
                        selectedLabel = prefs.reminderToneLabel,
                        onClick = { activeToneTarget = SoundToneTarget.REMINDER }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline
                    )
                    SoundToneRow(
                        title = "Notification sound",
                        subtitle = "Plays for daily study nudges & alerts",
                        selectedLabel = prefs.notificationToneLabel,
                        onClick = { activeToneTarget = SoundToneTarget.NOTIFICATION }
                    )
                }
            }
        }

        // About EduTrack Card
        item(key = "about_card", contentType = "about_card") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .bounceClick { showAboutDialog = true },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
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
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "About EduTrack",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
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

        item(key = "privacy_footer", contentType = "footer") {
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
        var useCustomClockPicker by remember { mutableStateOf(false) }
        if (useCustomClockPicker) {
            val initHour24 = (prefs.dailyReminderTimeMinutes / 60).coerceIn(0, 23)
            val initHour12 = when {
                initHour24 == 0 -> 12
                initHour24 > 12 -> initHour24 - 12
                else -> initHour24
            }
            WheelTimePickerDialog(
                initialHour12 = initHour12,
                initialMinute = prefs.dailyReminderTimeMinutes % 60,
                initialIsPm = initHour24 >= 12,
                onDismiss = { useCustomClockPicker = false },
                onConfirm = { h12, min, isPm ->
                    val hour24 = when {
                        !isPm && h12 == 12 -> 0
                        isPm && h12 != 12 -> h12 + 12
                        else -> h12
                    }
                    onReminderTimeChanged(hour24 * 60 + min)
                    useCustomClockPicker = false
                    showReminderTimePicker = false
                }
            )
        } else {
            // Exact Figma `19:215` Study reminder preset dialog
            Dialog(
                onDismissRequest = { showReminderTimePicker = false }
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .width(100.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.outline)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Study reminder",
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Choose a gentle nudge to get started.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        val presets = remember {
                            listOf(
                                7 * 60 to "7:00 AM",
                                19 * 60 to "7:00 PM",
                                21 * 60 to "9:00 PM"
                            )
                        }
                        presets.forEach { (mins, label) ->
                            val isSelected = prefs.dailyReminderTimeMinutes == mins
                            val rowBg by animateColorAsState(
                                targetValue = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                label = "preset_bg_$mins"
                            )
                            val rowBorder by animateColorAsState(
                                targetValue = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline
                                },
                                label = "preset_border_$mins"
                            )
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .bounceClick {
                                        onReminderTimeChanged(mins)
                                        showReminderTimePicker = false
                                    },
                                shape = RoundedCornerShape(14.dp),
                                color = rowBg,
                                border = BorderStroke(1.dp, rowBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_figma_clock),
                                        contentDescription = null,
                                        tint = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.primary
                                        },
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isSelected) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_figma_check),
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    } else {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_figma_arrow),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        val isCustomTime = presets.none { it.first == prefs.dailyReminderTimeMinutes }
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .bounceClick { useCustomClockPicker = true },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isCustomTime) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            border = BorderStroke(
                                1.dp,
                                if (isCustomTime) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Custom time...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isCustomTime) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    }
                                )
                                if (isCustomTime) {
                                    Text(
                                        text = RemindersViewModel.formatTimeOfDay(prefs.dailyReminderTimeMinutes),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        ClayButton(
                            onClick = { showReminderTimePicker = false },
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            shape = RoundedCornerShape(16.dp),
                            height = 48.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Cancel",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }

    activeToneTarget?.let { target ->
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val soundLoadState by produceState<Pair<Boolean, List<SystemSoundItem>>>(
            initialValue = true to emptyList(),
            key1 = context
        ) {
            val loaded = withContext(Dispatchers.IO) {
                SystemSoundHelper.loadDeviceSounds(context.applicationContext)
            }
            value = false to loaded
        }
        val isSoundsLoading = soundLoadState.first
        val deviceSounds = soundLoadState.second

        val initialUri = when (target) {
            SoundToneTarget.TIMER -> prefs.timerToneUri
            SoundToneTarget.REMINDER -> prefs.reminderToneUri
            SoundToneTarget.NOTIFICATION -> prefs.notificationToneUri
        }
        val initialLabel = when (target) {
            SoundToneTarget.TIMER -> prefs.timerToneLabel
            SoundToneTarget.REMINDER -> prefs.reminderToneLabel
            SoundToneTarget.NOTIFICATION -> prefs.notificationToneLabel
        }

        var selectedUri by remember(target) { mutableStateOf(initialUri) }
        var selectedLabel by remember(target) { mutableStateOf(initialLabel) }
        var activeFilter by remember(target) { mutableStateOf(target.defaultFilter) }

        val systemPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val pickedUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                }
                if (pickedUri != null) {
                    val title = runCatching {
                        RingtoneManager.getRingtone(context, pickedUri)?.getTitle(context)
                    }.getOrNull() ?: "Custom phone tone"
                    when (target) {
                        SoundToneTarget.TIMER -> onTimerToneSelected(pickedUri.toString(), title)
                        SoundToneTarget.REMINDER -> onReminderToneSelected(pickedUri.toString(), title)
                        SoundToneTarget.NOTIFICATION -> onNotificationToneSelected(pickedUri.toString(), title)
                    }
                    SystemSoundHelper.stopPreview()
                    activeToneTarget = null
                }
            }
        }

        DisposableEffect(target, lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                    SystemSoundHelper.stopPreview()
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                SystemSoundHelper.stopPreview()
            }
        }

        val filteredSounds = remember(deviceSounds, activeFilter) {
            if (activeFilter == SystemToneFilter.ALL) deviceSounds
            else deviceSounds.filter { it.filter == activeFilter }
        }

        AlertDialog(
            onDismissRequest = {
                SystemSoundHelper.stopPreview()
                activeToneTarget = null
            },
            title = {
                Text(
                    text = target.title,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SystemToneFilter.entries.forEach { filter ->
                            val isFilterSelected = activeFilter == filter
                            val chipBg by animateColorAsState(
                                targetValue = if (isFilterSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                label = "tone_filter_bg_${filter.name}"
                            )
                            val chipTextColor by animateColorAsState(
                                targetValue = if (isFilterSelected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                label = "tone_filter_text_${filter.name}"
                            )
                            val chipScale by animateFloatAsState(
                                targetValue = if (isFilterSelected) 1.03f else 1f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                label = "tone_filter_scale_${filter.name}"
                            )
                            Surface(
                                modifier = Modifier
                                    .graphicsLayer {
                                        scaleX = chipScale
                                        scaleY = chipScale
                                    }
                                    .clip(RoundedCornerShape(14.dp))
                                    .bounceClick { activeFilter = filter },
                                shape = RoundedCornerShape(14.dp),
                                color = chipBg,
                                border = BorderStroke(
                                    1.dp,
                                    if (isFilterSelected) Color.Transparent else MaterialTheme.colorScheme.outline
                                )
                            ) {
                                Text(
                                    text = filter.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isFilterSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = chipTextColor,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }

                    if (isSoundsLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    } else if (filteredSounds.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No tones found in this category.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(
                                items = filteredSounds,
                                key = { it.uriString },
                                contentType = { "sound_item" }
                            ) { sound ->
                                val isSelected = selectedUri == sound.uriString
                                val rowBg by animateColorAsState(
                                    targetValue = if (isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    },
                                    label = "sound_row_bg"
                                )
                                val rowBorder by animateColorAsState(
                                    targetValue = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                                    },
                                    label = "sound_row_border"
                                )

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .bounceClick(scaleDown = 0.97f) {
                                            selectedUri = sound.uriString
                                            selectedLabel = sound.title
                                            SystemSoundHelper.playPreview(
                                                context = context,
                                                uriString = sound.uriString,
                                                useAlarmUsage = target == SoundToneTarget.TIMER
                                            )
                                        },
                                    shape = RoundedCornerShape(14.dp),
                                    color = rowBg,
                                    border = BorderStroke(1.dp, rowBorder)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = null,
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = MaterialTheme.colorScheme.primary,
                                                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = sound.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                                color = if (isSelected) {
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface
                                                },
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = sound.filter.label,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    ClayOutlinedButton(
                        onClick = {
                            SystemSoundHelper.stopPreview()
                            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, target.title)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                if (selectedUri.isNotBlank()) {
                                    runCatching { Uri.parse(selectedUri) }.getOrNull()?.let { existingUri ->
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existingUri)
                                    }
                                }
                            }
                            runCatching { systemPickerLauncher.launch(intent) }
                        },
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                        borderColor = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(14.dp),
                        height = 42.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Open phone system picker...",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {
                ClayButton(
                    onClick = {
                        SystemSoundHelper.stopPreview()
                        when (target) {
                            SoundToneTarget.TIMER -> onTimerToneSelected(selectedUri, selectedLabel)
                            SoundToneTarget.REMINDER -> onReminderToneSelected(selectedUri, selectedLabel)
                            SoundToneTarget.NOTIFICATION -> onNotificationToneSelected(selectedUri, selectedLabel)
                        }
                        activeToneTarget = null
                    },
                    height = 40.dp,
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                ClayOutlinedButton(
                    onClick = {
                        SystemSoundHelper.stopPreview()
                        activeToneTarget = null
                    },
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    borderColor = MaterialTheme.colorScheme.outline,
                    height = 40.dp,
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAboutDialog) {
        // Exact Figma `19:216` About EduTrack sheet/dialog
        Dialog(
            onDismissRequest = { showAboutDialog = false }
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(100.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.outline)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ForestGreenPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                                contentDescription = "EduTrack logo",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                        Text(
                            text = "EduTrack",
                            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Text(
                        text = "Study Progress Tracker · Version ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Build a steady study habit. Set daily goals, focus on one subject at a time, and keep your progress close — on your own device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        lineHeight = 19.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    ClayButton(
                        onClick = { showAboutDialog = false },
                        shape = RoundedCornerShape(16.dp),
                        height = 50.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Got it",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SoundToneRow(
    title: String,
    subtitle: String,
    selectedLabel: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = selectedLabel,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_figma_arrow),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
