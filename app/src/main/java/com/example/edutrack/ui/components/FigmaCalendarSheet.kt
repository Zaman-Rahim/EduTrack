package com.example.edutrack.ui.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.edutrack.ui.theme.CardLightGreen
import com.example.edutrack.ui.theme.ForestGreenDark
import com.example.edutrack.ui.theme.ForestGreenPrimary
import com.example.edutrack.ui.theme.LimeAccent
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Exact Figma `24:1179` / `24:1189` Custom Calendar & Time Picker view.
 * Can be used both for Reminders (with Hour / Minute / AM-PM cards) and
 * for Subject Start Date (`showTimeSection = false`).
 */
@Composable
fun FigmaCustomDateTimeScreen(
    title: String = "Custom date & time",
    confirmLabel: String = "Use date & time",
    initialEpochDay: Long,
    initialTimeMinutes: Int = 9 * 60,
    showTimeSection: Boolean = true,
    allowPastDates: Boolean = false,
    onBackClick: () -> Unit,
    onConfirm: (epochDay: Long, timeMinutes: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { LocalDate.now() }
    val initialDate = remember(initialEpochDay) { LocalDate.ofEpochDay(initialEpochDay) }
    var selectedDate by remember(initialEpochDay) { mutableStateOf(initialDate) }
    var visibleMonth by remember(initialDate) { mutableStateOf(YearMonth.from(initialDate)) }

    val safeInitMins = initialTimeMinutes.coerceIn(0, 1439)
    val initHour24 = safeInitMins / 60
    var hour12 by remember(safeInitMins) {
        mutableIntStateOf(
            when {
                initHour24 == 0 -> 12
                initHour24 > 12 -> initHour24 - 12
                else -> initHour24
            }
        )
    }
    var minute by remember(safeInitMins) { mutableIntStateOf(safeInitMins % 60) }
    var isPm by remember(safeInitMins) { mutableStateOf(initHour24 >= 12) }

    val monthTitleFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()) }
    val selectedDateFormatter = remember { DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault()) }
    val dayHeaders = remember { listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun") }

    val firstOfMonth = visibleMonth.atDay(1)
    val leadingEmptyCells = firstOfMonth.dayOfWeek.value - 1 // Mon=1 -> 0
    val daysInMonth = visibleMonth.lengthOfMonth()
    val totalCells = leadingEmptyCells + daysInMonth
    val rowCount = (totalCells + 6) / 7

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Top Header with white circular back chevron + title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            FigmaBackButton(onClick = onBackClick)
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Month Header + Prev / Next rounded `#EAF0E7` chevron buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = visibleMonth.format(monthTitleFormatter),
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 23.sp),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(width = 48.dp, height = 42.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardLightGreen)
                        .bounceClick { visibleMonth = visibleMonth.minusMonths(1) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "‹",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Medium,
                        color = ForestGreenPrimary
                    )
                }
                Box(
                    modifier = Modifier
                        .size(width = 48.dp, height = 42.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardLightGreen)
                        .bounceClick { visibleMonth = visibleMonth.plusMonths(1) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "›",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Medium,
                        color = ForestGreenPrimary
                    )
                }
            }
        }

        // Weekday labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            dayHeaders.forEach { dayName ->
                Text(
                    text = dayName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Calendar grid
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (row in 0 until rowCount) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (col in 0..6) {
                        val cellIndex = row * 7 + col
                        val dayNum = cellIndex - leadingEmptyCells + 1
                        if (dayNum in 1..daysInMonth) {
                            val cellDate = visibleMonth.atDay(dayNum)
                            val isSelected = cellDate == selectedDate
                            val isPast = !allowPastDates && cellDate.isBefore(today)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(13.dp))
                                    .background(if (isSelected) LimeAccent else Color.Transparent)
                                    .bounceClick(enabled = !isPast) {
                                        selectedDate = cellDate
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayNum.toString(),
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = when {
                                        isSelected -> ForestGreenDark
                                        isPast -> Color(0xFFADB7A6)
                                        else -> MaterialTheme.colorScheme.onBackground
                                    }
                                )
                            }
                        } else {
                            Spacer(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                            )
                        }
                    }
                }
            }
        }

        // SELECTED DATE summary
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "SELECTED DATE",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.8.sp
            )
            Text(
                text = selectedDate.format(selectedDateFormatter),
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (showTimeSection) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "TIME",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.8.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Hour card (tap cycles +1h)
                    TimeBoxCard(
                        value = String.format(Locale.getDefault(), "%02d", hour12),
                        label = "Hour",
                        onStepUp = { hour12 = if (hour12 >= 12) 1 else hour12 + 1 },
                        onStepDown = { hour12 = if (hour12 <= 1) 12 else hour12 - 1 },
                        modifier = Modifier.weight(1f)
                    )
                    // Minute card (tap cycles +5m)
                    TimeBoxCard(
                        value = String.format(Locale.getDefault(), "%02d", minute),
                        label = "Minute",
                        onStepUp = { minute = (minute + 5) % 60 },
                        onStepDown = { minute = (minute + 55) % 60 },
                        modifier = Modifier.weight(1f)
                    )
                    // AM / PM toggle card
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(82.dp)
                            .bounceClick { isPm = !isPm },
                        shape = RoundedCornerShape(15.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, Color(0xFFCCD8C4))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isPm) "PM" else "AM",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "AM / PM",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Button(
            onClick = {
                val hour24 = when {
                    !isPm && hour12 == 12 -> 0
                    isPm && hour12 != 12 -> hour12 + 12
                    else -> hour12
                }
                val finalMinutes = hour24 * 60 + minute
                onConfirm(selectedDate.toEpochDay(), finalMinutes)
            },
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ForestGreenPrimary,
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .bounceClick {
                    val hour24 = when {
                        !isPm && hour12 == 12 -> 0
                        isPm && hour12 != 12 -> hour12 + 12
                        else -> hour12
                    }
                    onConfirm(selectedDate.toEpochDay(), hour24 * 60 + minute)
                }
        ) {
            Text(
                text = confirmLabel,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun TimeBoxCard(
    value: String,
    label: String,
    onStepUp: () -> Unit,
    onStepDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(82.dp)
            .bounceClick(onClick = onStepUp),
        shape = RoundedCornerShape(15.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, Color(0xFFCCD8C4))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "−",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ForestGreenPrimary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .bounceClick(onClick = onStepDown)
                        .padding(horizontal = 4.dp)
                )
                Text(
                    text = value,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "+",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ForestGreenPrimary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .bounceClick(onClick = onStepUp)
                        .padding(horizontal = 4.dp)
                )
            }
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
