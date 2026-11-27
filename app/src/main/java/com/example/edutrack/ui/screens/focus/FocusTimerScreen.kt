package com.example.edutrack.ui.screens.focus

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edutrack.R
import com.example.edutrack.ui.components.ClayButton
import com.example.edutrack.ui.components.FigmaBackButton
import com.example.edutrack.ui.components.bounceClick
import com.example.edutrack.ui.theme.ForestGreenPrimary
import com.example.edutrack.ui.theme.LimeAccent
import com.example.edutrack.ui.timer.FocusTimerUiState
import com.example.edutrack.ui.timer.TimerStatus
import com.example.edutrack.util.DateTimeUtils
import kotlinx.coroutines.launch

/**
 * Exact Figma `19:175` ("Focus timer") & `19:205` ("Focus complete") screen.
 * Includes the top-left Down Arrow (`ic_figma_down_arrow`) to minimize into the bottom Live Focus Bar.
 */
@Composable
fun FocusTimerScreen(
    timerState: FocusTimerUiState,
    onMinimizeClick: () -> Unit,
    onPauseResumeClick: () -> Unit,
    onEndWithoutCompletingClick: () -> Unit,
    onOpenNotificationsClick: () -> Unit,
    onBackToPlanClick: () -> Unit,
    onStopAlarmClick: () -> Unit = {},
    isBackEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val subjectName = timerState.subject?.name ?: "Study Focus"

    if (timerState.status == TimerStatus.COMPLETED) {
        FocusCompleteView(
            subjectName = subjectName,
            isAlarmRinging = timerState.isAlarmRinging,
            isBackEnabled = isBackEnabled,
            onStopAlarmClick = onStopAlarmClick,
            onBackToPlanClick = onBackToPlanClick,
            onOpenNotificationsClick = {
                onStopAlarmClick()
                onOpenNotificationsClick()
            },
            modifier = modifier
        )
        return
    }

    val (formattedCountdown, unitLabel) = DateTimeUtils.formatTimerCountdown(timerState.remainingSeconds)
    val isHoursFormat = timerState.remainingSeconds >= 3600
    val countdownFontSize = if (isHoursFormat) 50.sp else 62.sp
    val animatedSweep by animateFloatAsState(
        targetValue = 360f * timerState.progressFraction,
        label = "timer_dial_sweep"
    )

    val scope = rememberCoroutineScope()
    val dragOffsetY = remember { Animatable(0f) }
    val dismissThresholdPx = with(LocalDensity.current) { 110.dp.toPx() }
    val scrollState = rememberScrollState()
    val currentOnMinimizeClick by rememberUpdatedState(onMinimizeClick)

    val minimizeAndResetDrag = {
        scope.launch { dragOffsetY.snapTo(0f) }
        currentOnMinimizeClick()
    }

    // Back button + Swipe-down to minimize smoothly into the Live Focus Bar
    BackHandler(enabled = isBackEnabled) {
        minimizeAndResetDrag()
    }

    val dialTrackColor = MaterialTheme.colorScheme.outline
    val dialArcColor = MaterialTheme.colorScheme.primary

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                val y = dragOffsetY.value
                val progress = (y / (dismissThresholdPx * 2.4f)).coerceIn(0f, 1f)
                translationY = y
                scaleX = 1f - 0.06f * progress
                scaleY = 1f - 0.06f * progress
                alpha = 1f - 0.16f * progress
            }
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        val minViewportHeight = maxHeight
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        if (scrollState.value != 0) return@awaitEachGesture
                        var startedMinimizeDrag = false
                        val drag = awaitVerticalTouchSlopOrCancellation(down.id) { change, overSlop ->
                            if (scrollState.value == 0 && overSlop > 0f) {
                                change.consume()
                                startedMinimizeDrag = true
                                scope.launch {
                                    dragOffsetY.snapTo((dragOffsetY.value + overSlop).coerceAtLeast(0f))
                                }
                            }
                        }
                        if (drag != null && startedMinimizeDrag) {
                            val completed = verticalDrag(drag.id) { change ->
                                val delta = change.positionChange().y
                                change.consume()
                                scope.launch {
                                    dragOffsetY.snapTo((dragOffsetY.value + delta).coerceAtLeast(0f))
                                }
                            }
                            scope.launch {
                                if (completed && dragOffsetY.value >= dismissThresholdPx) {
                                    dragOffsetY.snapTo(0f)
                                    currentOnMinimizeClick()
                                } else {
                                    dragOffsetY.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                .heightIn(min = minViewportHeight)
                .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with Down Arrow Minimize button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .bounceClick(onClick = minimizeAndResetDrag),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_figma_down_arrow),
                        contentDescription = "Minimise focus timer",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = "Focus session",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (timerState.status == TimerStatus.PAUSED) "PAUSED" else "DAILY FOCUS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = subjectName,
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = DateTimeUtils.formatDuration(timerState.targetMinutes),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Circular Countdown Dial (264dp)
            Box(
                modifier = Modifier.size(264.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 14.dp.toPx()
                    drawCircle(
                        color = dialTrackColor,
                        style = Stroke(width = strokeWidth)
                    )
                    drawArc(
                        color = dialArcColor,
                        startAngle = -90f,
                        sweepAngle = animatedSweep,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formattedCountdown,
                        fontSize = countdownFontSize,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-2).sp,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = unitLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.8.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            Text(
                text = "One subject. One focused step.",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(14.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Minimise without ending your session.",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Check reminders or keep using the app.\nYour countdown stays in the live bar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Spacer(modifier = Modifier.weight(1f))

            ClayButton(
                onClick = onPauseResumeClick,
                shape = RoundedCornerShape(18.dp),
                height = 56.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (timerState.status == TimerStatus.PAUSED) "Resume session" else "Pause session",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .bounceClick(onClick = onEndWithoutCompletingClick)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "End without completing",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FocusCompleteView(
    subjectName: String,
    isAlarmRinging: Boolean,
    isBackEnabled: Boolean,
    onStopAlarmClick: () -> Unit,
    onBackToPlanClick: () -> Unit,
    onOpenNotificationsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = isBackEnabled) {
        onStopAlarmClick()
        onBackToPlanClick()
    }

    val backAndStopAlarm = {
        onStopAlarmClick()
        onBackToPlanClick()
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        val minViewportHeight = maxHeight
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = minViewportHeight)
                .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                FigmaBackButton(onClick = backAndStopAlarm)
                Text(
                    text = "Focus complete",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Notification Banner Card matching Figma 24:592
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_figma_bell),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                    Column {
                        Text(
                            text = "EDUTRACK · NOW",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subjectName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Your daily focus is complete.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // Solid Green Completion Dial (250dp)
            Box(
                modifier = Modifier
                    .size(250.dp)
                    .clip(CircleShape)
                    .background(ForestGreenPrimary),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_figma_check),
                        contentDescription = null,
                        tint = LimeAccent,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "00:00",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = "TARGET REACHED",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFD3E0D5),
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "Daily goal complete.",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(14.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .bounceClick(onClick = onOpenNotificationsClick),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Notification saved · Open inbox",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Your completed subject is waiting below.\nChoose the next one from today’s list.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Spacer(modifier = Modifier.weight(1f))

            if (isAlarmRinging) {
                ClayButton(
                    onClick = onStopAlarmClick,
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                    shape = RoundedCornerShape(18.dp),
                    height = 52.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Stop alarm",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            ClayButton(
                onClick = backAndStopAlarm,
                shape = RoundedCornerShape(18.dp),
                height = 56.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Back to today’s plan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
