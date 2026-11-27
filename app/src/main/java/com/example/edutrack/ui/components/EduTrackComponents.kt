package com.example.edutrack.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edutrack.R
import com.example.edutrack.data.local.relation.SessionWithSubject
import com.example.edutrack.ui.theme.CardLightGreen
import com.example.edutrack.ui.theme.ForestGreenDark
import com.example.edutrack.ui.theme.ForestGreenPrimary
import com.example.edutrack.ui.theme.LimeAccent
import com.example.edutrack.ui.theme.subjectPastelContainer
import com.example.edutrack.ui.timer.FocusTimerUiState
import com.example.edutrack.ui.timer.TimerStatus
import com.example.edutrack.util.DateTimeUtils
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.launch

/**
 * Applies a rubbery clay squash-and-stretch spring press animation across buttons, cards, and interactive tiles.
 */
fun Modifier.bounceClick(
    enabled: Boolean = true,
    scaleDown: Float = 0.96f,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val squashY by animateFloatAsState(
        targetValue = if (isPressed && enabled) (scaleDown - 0.03f).coerceAtLeast(0.86f) else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "clay_squash_y"
    )
    val stretchX by animateFloatAsState(
        targetValue = if (isPressed && enabled) (scaleDown + (1f - scaleDown) * 1.35f).coerceAtMost(1.03f) else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "clay_stretch_x"
    )
    this
        .graphicsLayer {
            scaleX = stretchX
            scaleY = squashY
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

/**
 * Reusable 3D clay segmented control with a smoothly sliding, rubbery-stretching selected pill.
 */
@Composable
fun ClaySegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (options.isEmpty()) return

    val clampedIndex = selectedIndex.coerceIn(0, options.lastIndex)
    val containerShape = RoundedCornerShape(16.dp)
    val pillShape = RoundedCornerShape(13.dp)

    val stretchX = remember { Animatable(1f) }
    val stretchY = remember { Animatable(1f) }
    var initialized by remember { mutableStateOf(false) }

    LaunchedEffect(clampedIndex) {
        if (!initialized) {
            initialized = true
            return@LaunchedEffect
        }
        launch {
            stretchX.animateTo(1.10f, animationSpec = tween(durationMillis = 85))
            stretchX.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
        launch {
            stretchY.animateTo(0.90f, animationSpec = tween(durationMillis = 85))
            stretchY.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(containerShape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(4.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val segmentWidth = maxWidth / options.size
            val pillOffsetX by animateDpAsState(
                targetValue = segmentWidth * clampedIndex,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "clay_pill_offset"
            )

            // Flat Figma dark green pill driven by GPU translation & rubbery squash/stretch
            Box(
                modifier = Modifier
                    .width(segmentWidth)
                    .fillMaxHeight()
                    .graphicsLayer {
                        translationX = pillOffsetX.toPx()
                        scaleX = stretchX.value
                        scaleY = stretchY.value
                    }
                    .clip(pillShape)
                    .background(ForestGreenPrimary)
            )

            Row(modifier = Modifier.fillMaxSize()) {
                options.forEachIndexed { index, label ->
                    val isSelected = index == clampedIndex
                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onBackground,
                        label = "clay_segment_text"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(pillShape)
                            .bounceClick(scaleDown = 0.95f) { onOptionSelected(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@DrawableRes
fun figmaSubjectIconRes(subjectName: String): Int {
    val lower = subjectName.lowercase()
    return if (lower.contains("math") ||
        lower.contains("algorithm") ||
        lower.contains("data") ||
        lower.contains("stat") ||
        lower.contains("calc")
    ) {
        R.drawable.ic_figma_stats
    } else {
        R.drawable.ic_figma_book
    }
}

/**
 * Top-right Figma Notifications Bell + Me circular buttons shared across main screens.
 */
@Composable
fun TopRightHeaderButtons(
    unreadNotificationsCount: Int,
    onNotificationsClick: () -> Unit,
    onMeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(CardLightGreen)
                .bounceClick(onClick = onMeClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_figma_me),
                contentDescription = "Open Me",
                tint = ForestGreenDark,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * Exact Figma `47:62` Uber/Foodpanda-style persistent bottom Live Focus Bar when timer is minimized.
 * Supports horizontal swipe left or right to slide off-screen and dismiss.
 */
@Composable
fun LiveFocusBar(
    timerState: FocusTimerUiState,
    onExpandClick: () -> Unit,
    onPauseResumeOrDismissClick: () -> Unit,
    modifier: Modifier = Modifier,
    onSwipeDismiss: () -> Unit = {}
) {
    val subjectName = timerState.subject?.name ?: "Focus session"
    val mins = timerState.remainingSeconds / 60
    val secs = timerState.remainingSeconds % 60
    val timeString = String.format(Locale.getDefault(), "%02d:%02d", mins, secs)

    val subtitle = when (timerState.status) {
        TimerStatus.RUNNING -> "Focusing · Tap to expand"
        TimerStatus.PAUSED -> "Paused · Tap to expand"
        TimerStatus.COMPLETED -> "Goal complete · Tap to view"
        TimerStatus.IDLE -> ""
    }

    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var barWidthPx by remember { mutableFloatStateOf(0f) }
    val minDismissPx = with(LocalDensity.current) { 96.dp.toPx() }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .onSizeChanged { barWidthPx = it.width.toFloat() }
            .graphicsLayer {
                translationX = offsetX.value
                val fadeDistance = (if (barWidthPx > 0f) barWidthPx else 800f) * 0.85f
                alpha = (1f - (abs(offsetX.value) / fadeDistance)).coerceIn(0f, 1f)
            }
            .pointerInput(onSwipeDismiss) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            offsetX.snapTo(offsetX.value + dragAmount)
                        }
                    },
                    onDragEnd = {
                        val threshold = maxOf(minDismissPx, barWidthPx * 0.30f)
                        if (abs(offsetX.value) >= threshold) {
                            val offscreenTarget = (if (barWidthPx > 0f) barWidthPx else 1000f) * 1.2f
                            val targetX = if (offsetX.value >= 0f) offscreenTarget else -offscreenTarget
                            scope.launch {
                                offsetX.animateTo(targetX, animationSpec = tween(durationMillis = 180))
                                onSwipeDismiss()
                            }
                        } else {
                            scope.launch {
                                offsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                )
                            }
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            offsetX.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                        }
                    }
                )
            }
            .bounceClick(scaleDown = 0.98f, onClick = onExpandClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ForestGreenPrimary),
        border = BorderStroke(1.dp, Color(0xFF2F6552)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 13.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF315F4C)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_figma_clock),
                    contentDescription = null,
                    tint = LimeAccent,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subjectName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = Color(0xFFD3E0D5),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 10.dp)
            ) {
                Text(
                    text = timeString,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LimeAccent
                )
                Icon(
                    painter = painterResource(id = R.drawable.ic_figma_up),
                    contentDescription = "Expand",
                    tint = Color(0xFFD3E0D5),
                    modifier = Modifier.size(18.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF315F4C))
                    .bounceClick(onClick = onPauseResumeOrDismissClick),
                contentAlignment = Alignment.Center
            ) {
                val iconRes = when (timerState.status) {
                    TimerStatus.RUNNING -> R.drawable.ic_figma_pause
                    TimerStatus.PAUSED -> R.drawable.ic_figma_play
                    TimerStatus.COMPLETED -> R.drawable.ic_figma_check
                    TimerStatus.IDLE -> R.drawable.ic_figma_pause
                }
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = "Toggle timer",
                    tint = LimeAccent,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun SubjectColorDot(
    colorHex: Long,
    size: Dp = 12.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(colorHex))
    )
}

@Composable
fun SubjectTagChip(
    name: String,
    colorHex: Long,
    modifier: Modifier = Modifier
) {
    val bg = subjectPastelContainer(colorHex)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = bg
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SubjectColorDot(colorHex = colorHex, size = 8.dp)
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium,
                color = ForestGreenDark,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun FigmaBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .bounceClick(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_figma_back),
            contentDescription = "Back",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun SessionListCard(
    item: SessionWithSubject,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerBg = subjectPastelContainer(item.subject.colorHex)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .bounceClick(scaleDown = 0.98f, onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 16.dp),
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
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.session.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.session.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = DateTimeUtils.formatShortDate(item.session.sessionDateEpochDay),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = DateTimeUtils.formatDuration(item.session.durationMinutes),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
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
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    description: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                tint = ForestGreenPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onActionClick != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onActionClick,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ForestGreenPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    confirmText: String = "Delete",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, fontWeight = FontWeight.Bold) },
        text = { Text(text = message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
