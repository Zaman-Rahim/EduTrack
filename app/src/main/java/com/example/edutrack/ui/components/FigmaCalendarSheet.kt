package com.example.edutrack.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.edutrack.ui.theme.ForestGreenDark
import com.example.edutrack.ui.theme.LimeAccent
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * Exact Figma `24:1179` / `24:1189` Calendar & Time Picker view.
 * Tapping any of the 3 Time cards (Hour / Minute / AM-PM) opens the iPhone-style
 * vertical scroll wheel picker [WheelTimePickerDialog].
 */
@Composable
fun FigmaCustomDateTimeScreen(
    title: String = "Date and time",
    confirmLabel: String = "Use date & time",
    initialEpochDay: Long,
    initialTimeMinutes: Int = 9 * 60,
    showTimeSection: Boolean = true,
    allowPastDates: Boolean = false,
    onBackClick: () -> Unit,
    onConfirm: (epochDay: Long, timeMinutes: Int) -> Unit,
    modifier: Modifier = Modifier,
    weekStartsOn: String = "Monday"
) {
    val today = remember { LocalDate.now() }
    val currentMonth = remember(today) { YearMonth.from(today) }
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
    var showWheelTimePicker by remember { mutableStateOf(false) }

    val monthTitleFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()) }
    val selectedDateFormatter = remember { DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault()) }
    val startsSunday = weekStartsOn.equals("Sunday", ignoreCase = true)
    val dayHeaders = remember(startsSunday) {
        if (startsSunday) {
            listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        } else {
            listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        }
    }

    val canGoPrevMonth = allowPastDates || visibleMonth.isAfter(currentMonth)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Top Header with circular back chevron + title
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

        // Month Header + Prev / Next rounded chevron buttons
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
                MonthChevronButton(
                    symbol = "‹",
                    enabled = canGoPrevMonth,
                    onClick = { visibleMonth = visibleMonth.minusMonths(1) }
                )
                MonthChevronButton(
                    symbol = "›",
                    enabled = true,
                    onClick = { visibleMonth = visibleMonth.plusMonths(1) }
                )
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

        // Deterministic BoxWithConstraints Calendar grid with passing LimeAccent clay pill
        CalendarMonthGrid(
            visibleMonth = visibleMonth,
            selectedDate = selectedDate,
            today = today,
            allowPastDates = allowPastDates,
            weekStartsOn = weekStartsOn,
            onDateSelected = { selectedDate = it }
        )

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
                    TimeBoxCard(
                        value = String.format(Locale.getDefault(), "%02d", hour12),
                        label = "Hour",
                        onClick = { showWheelTimePicker = true },
                        modifier = Modifier.weight(1f)
                    )
                    TimeBoxCard(
                        value = String.format(Locale.getDefault(), "%02d", minute),
                        label = "Minute",
                        onClick = { showWheelTimePicker = true },
                        modifier = Modifier.weight(1f)
                    )
                    TimeBoxCard(
                        value = if (isPm) "PM" else "AM",
                        label = "AM / PM",
                        onClick = { showWheelTimePicker = true },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        ClayButton(
            onClick = {
                val hour24 = when {
                    !isPm && hour12 == 12 -> 0
                    isPm && hour12 != 12 -> hour12 + 12
                    else -> hour12
                }
                onConfirm(selectedDate.toEpochDay(), hour24 * 60 + minute)
            },
            shape = RoundedCornerShape(18.dp),
            height = 54.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = confirmLabel,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }

    if (showWheelTimePicker) {
        WheelTimePickerDialog(
            initialHour12 = hour12,
            initialMinute = minute,
            initialIsPm = isPm,
            onDismiss = { showWheelTimePicker = false },
            onConfirm = { h12, min, pm ->
                hour12 = h12
                minute = min
                isPm = pm
                showWheelTimePicker = false
            }
        )
    }
}

@Composable
private fun MonthChevronButton(
    symbol: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (enabled) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    }
    val contentColor = if (enabled) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.35f)
    }

    Box(
        modifier = Modifier
            .size(width = 48.dp, height = 42.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .bounceClick(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium,
            color = contentColor
        )
    }
}

@Composable
private fun CalendarMonthGrid(
    visibleMonth: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    allowPastDates: Boolean,
    weekStartsOn: String = "Monday",
    onDateSelected: (LocalDate) -> Unit
) {
    val startsSunday = weekStartsOn.equals("Sunday", ignoreCase = true)
    val firstOfMonth = visibleMonth.atDay(1)
    val leadingEmptyCells = if (startsSunday) {
        firstOfMonth.dayOfWeek.value % 7
    } else {
        firstOfMonth.dayOfWeek.value - 1
    }
    val daysInMonth = visibleMonth.lengthOfMonth()
    val totalCells = leadingEmptyCells + daysInMonth
    val rowCount = (totalCells + 6) / 7

    val colGap = 4.dp
    val rowGap = 6.dp
    val cellHeight = 40.dp
    val density = LocalDensity.current

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cellWidth = ((maxWidth - colGap * 6) / 7f).coerceAtLeast(0.dp)
        val cellWidthPx = with(density) { cellWidth.toPx() }
        val cellHeightPx = with(density) { cellHeight.toPx() }
        val colGapPx = with(density) { colGap.toPx() }
        val rowGapPx = with(density) { rowGap.toPx() }

        val isTargetVisible = YearMonth.from(selectedDate) == visibleMonth
        val targetIndex = leadingEmptyCells + (selectedDate.dayOfMonth - 1)
        val targetRow = targetIndex / 7
        val targetCol = targetIndex % 7
        val targetXPx = if (isTargetVisible) targetCol * (cellWidthPx + colGapPx) else 0f
        val targetYPx = if (isTargetVisible) targetRow * (cellHeightPx + rowGapPx) else 0f

        val pillX = remember { Animatable(targetXPx) }
        val pillY = remember { Animatable(targetYPx) }
        val calStretchX = remember { Animatable(1f) }
        val calStretchY = remember { Animatable(1f) }
        var lastVisibleSelectionMonth by remember { mutableStateOf<YearMonth?>(null) }
        var lastSelectedDate by remember { mutableStateOf<LocalDate?>(null) }

        LaunchedEffect(selectedDate, visibleMonth, targetXPx, targetYPx, isTargetVisible) {
            if (!isTargetVisible) {
                lastVisibleSelectionMonth = null
                return@LaunchedEffect
            }

            val shouldAnimateMove =
                lastVisibleSelectionMonth == visibleMonth &&
                    lastSelectedDate != null &&
                    lastSelectedDate != selectedDate

            if (shouldAnimateMove) {
                launch {
                    pillX.animateTo(
                        targetValue = targetXPx,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                }
                launch {
                    pillY.animateTo(
                        targetValue = targetYPx,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                }
                launch {
                    calStretchX.animateTo(1.18f, animationSpec = tween(durationMillis = 80))
                    calStretchX.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                }
                launch {
                    calStretchY.animateTo(0.84f, animationSpec = tween(durationMillis = 80))
                    calStretchY.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                }
            } else {
                pillX.snapTo(targetXPx)
                pillY.snapTo(targetYPx)
                calStretchX.snapTo(1f)
                calStretchY.snapTo(1f)
            }

            lastVisibleSelectionMonth = visibleMonth
            lastSelectedDate = selectedDate
        }

        if (isTargetVisible && cellWidth > 0.dp) {
            Box(
                modifier = Modifier
                    .width(cellWidth)
                    .height(cellHeight)
                    .graphicsLayer {
                        translationX = pillX.value
                        translationY = pillY.value
                        scaleX = calStretchX.value
                        scaleY = calStretchY.value
                    }
                    .clip(RoundedCornerShape(13.dp))
                    .background(LimeAccent)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(rowGap)) {
            for (row in 0 until rowCount) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(colGap)
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
                                    .height(cellHeight)
                                    .clip(RoundedCornerShape(13.dp))
                                    .bounceClick(enabled = !isPast) {
                                        onDateSelected(cellDate)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayNum.toString(),
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = when {
                                        isSelected -> ForestGreenDark
                                        isPast -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                        else -> MaterialTheme.colorScheme.onBackground
                                    }
                                )
                            }
                        } else {
                            Spacer(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(cellHeight)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeBoxCard(
    value: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(82.dp)
            .bounceClick(onClick = onClick),
        shape = RoundedCornerShape(15.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * iPhone-style 3-column vertical scroll wheel time picker (Hour · Minute · AM/PM)
 * with smooth 120Hz snapping.
 */
@Composable
fun WheelTimePickerDialog(
    initialHour12: Int,
    initialMinute: Int,
    initialIsPm: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (hour12: Int, minute: Int, isPm: Boolean) -> Unit
) {
    val hours = remember { (1..12).toList() }
    val minutes = remember { (0..59).toList() }
    val periods = remember { listOf("AM", "PM") }

    var pickedHour by remember { mutableIntStateOf(initialHour12.coerceIn(1, 12)) }
    var pickedMinute by remember { mutableIntStateOf(initialMinute.coerceIn(0, 59)) }
    var pickedIsPm by remember { mutableStateOf(initialIsPm) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(width = 80.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.outline)
                )
                Text(
                    text = "Select time",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(176.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Center selection pill band
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    )

                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        WheelColumn(
                            items = hours.map { String.format(Locale.getDefault(), "%02d", it) },
                            initialIndex = (pickedHour - 1).coerceIn(0, 11),
                            onSelectedIndex = { idx -> pickedHour = hours[idx] },
                            modifier = Modifier.weight(1f)
                        )
                        WheelColumn(
                            items = minutes.map { String.format(Locale.getDefault(), "%02d", it) },
                            initialIndex = pickedMinute.coerceIn(0, 59),
                            onSelectedIndex = { idx -> pickedMinute = minutes[idx] },
                            modifier = Modifier.weight(1f)
                        )
                        WheelColumn(
                            items = periods,
                            initialIndex = if (pickedIsPm) 1 else 0,
                            onSelectedIndex = { idx -> pickedIsPm = idx == 1 },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ClayOutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        height = 48.dp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Cancel",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    ClayButton(
                        onClick = { onConfirm(pickedHour, pickedMinute, pickedIsPm) },
                        shape = RoundedCornerShape(14.dp),
                        height = 48.dp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Set time",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WheelColumn(
    items: List<String>,
    initialIndex: Int,
    onSelectedIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val itemHeight = 44.dp
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val scope = rememberCoroutineScope()
    val latestOnSelectedIndex by rememberUpdatedState(onSelectedIndex)

    val centeredIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
            layoutInfo.visibleItemsInfo
                .minByOrNull { kotlin.math.abs((it.offset + it.size / 2) - viewportCenter) }
                ?.index ?: initialIndex
        }
    }

    LaunchedEffect(centeredIndex) {
        if (centeredIndex in items.indices) {
            latestOnSelectedIndex(centeredIndex)
        }
    }

    LazyColumn(
        state = listState,
        flingBehavior = flingBehavior,
        contentPadding = PaddingValues(vertical = 66.dp),
        modifier = modifier.height(176.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(
            count = items.size,
            key = { index -> "$index-${items[index]}" }
        ) { index ->
            val isCentered = index == centeredIndex
            val interactionSource = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        scope.launch {
                            listState.animateScrollToItem(index)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = items[index],
                    fontSize = if (isCentered) 22.sp else 16.sp,
                    fontWeight = if (isCentered) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isCentered) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                    }
                )
            }
        }
    }
}
