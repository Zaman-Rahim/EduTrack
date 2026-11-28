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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edutrack.R
import com.example.edutrack.data.local.relation.SessionWithSubject
import com.example.edutrack.ui.theme.ForestGreenDark
import com.example.edutrack.ui.theme.ForestGreenPrimary
import com.example.edutrack.ui.theme.LimeAccent
import com.example.edutrack.ui.theme.subjectPastelContainer
import com.example.edutrack.ui.timer.FocusTimerUiState
import com.example.edutrack.ui.timer.TimerStatus
import com.example.edutrack.util.DateTimeUtils
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * Applies a zero-recomposition GPU-layer clay squash-and-stretch spring press animation across
 * buttons, cards, and interactive tiles.
 */
fun Modifier.bounceClick(
    enabled: Boolean = true,
    scaleDown: Float = 0.96f,
    onClick: () -> Unit
): Modifier = this
    .then(BounceScaleElement(enabled = enabled, scaleDown = scaleDown))
    .clickable(
        interactionSource = null,
        indication = null,
        enabled = enabled,
        onClick = onClick
    )

private data class BounceScaleElement(
    val enabled: Boolean,
    val scaleDown: Float
) : ModifierNodeElement<BounceScaleNode>() {
    override fun create(): BounceScaleNode = BounceScaleNode(enabled = enabled, scaleDown = scaleDown)

    override fun update(node: BounceScaleNode) {
        node.enabled = enabled
        node.scaleDown = scaleDown
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "bounceScale"
        properties["enabled"] = enabled
        properties["scaleDown"] = scaleDown
    }
}

private class BounceScaleNode(
    var enabled: Boolean,
    var scaleDown: Float
) : Modifier.Node(), PointerInputModifierNode, LayoutModifierNode {

    private val squashY = Animatable(1f)
    private val stretchX = Animatable(1f)
    private var animJob: kotlinx.coroutines.Job? = null

    private fun animateToPressed(pressed: Boolean) {
        animJob?.cancel()
        animJob = coroutineScope.launch {
            val targetY = if (pressed && enabled) (scaleDown - 0.03f).coerceAtLeast(0.86f) else 1f
            val targetX = if (pressed && enabled) (scaleDown + (1f - scaleDown) * 1.35f).coerceAtMost(1.03f) else 1f
            val spec = spring<Float>(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
            launch { squashY.animateTo(targetY, spec) }
            launch { stretchX.animateTo(targetX, spec) }
        }
    }

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) {
        if (!enabled) return
        val changes = pointerEvent.changes
        if (pass == PointerEventPass.Initial) {
            if (changes.any { it.changedToDownIgnoreConsumed() }) {
                animateToPressed(true)
            } else if (changes.all { !it.pressed } || changes.any { it.changedToUpIgnoreConsumed() }) {
                if (squashY.targetValue != 1f) {
                    animateToPressed(false)
                }
            }
        } else if (pass == PointerEventPass.Final && squashY.targetValue != 1f) {
            if (changes.any { it.isConsumed }) {
                animateToPressed(false)
            }
        }
    }

    override fun onCancelPointerInput() {
        if (squashY.targetValue != 1f) {
            animateToPressed(false)
        }
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0) {
                scaleX = stretchX.value
                scaleY = squashY.value
            }
        }
    }
}

/**
 * Reusable clay-pressed primary button built on [Surface] + [Modifier.bounceClick].
 */
@Composable
fun ClayButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    shape: Shape = RoundedCornerShape(16.dp),
    height: Dp = 48.dp,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp),
    content: @Composable RowScope.() -> Unit
) {
    val resolvedContainerColor = if (enabled) {
        containerColor
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    }
    val resolvedContentColor = if (enabled) {
        contentColor
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    }
    val sizedModifier = if (height != Dp.Unspecified) modifier.height(height) else modifier

    Surface(
        modifier = sizedModifier
            .semantics { role = Role.Button }
            .clip(shape)
            .bounceClick(enabled = enabled, onClick = onClick),
        shape = shape,
        color = resolvedContainerColor,
        contentColor = resolvedContentColor,
        border = border,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        ProvideTextStyle(
            value = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
        ) {
            Row(
                modifier = Modifier.padding(contentPadding),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }
}

/**
 * Reusable clay-pressed outlined secondary button delegating to [ClayButton].
 */
@Composable
fun ClayOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    shape: Shape = RoundedCornerShape(16.dp),
    height: Dp = 48.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp),
    content: @Composable RowScope.() -> Unit
) {
    val resolvedBorderColor = if (enabled) borderColor else borderColor.copy(alpha = 0.38f)
    ClayButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        containerColor = containerColor,
        contentColor = contentColor,
        shape = shape,
        height = height,
        border = BorderStroke(1.dp, resolvedBorderColor),
        contentPadding = contentPadding,
        content = content
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

            // Active clay pill driven by GPU translation & rubbery squash/stretch
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
                    .background(MaterialTheme.colorScheme.primary)
            )

            Row(modifier = Modifier.fillMaxSize()) {
                options.forEachIndexed { index, label ->
                    val isSelected = index == clampedIndex
                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
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

/**
 * Horizontally scrollable chip strip where a themed clay pill smoothly glides and stretches
 * behind the selected chip in [drawBehind] (zero per-frame relayout churn) and auto-scrolls into view.
 */
@Composable
fun ClayScrollableChipRow(
    options: List<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    leadingContent: (@Composable () -> Unit)? = null
) {
    if (options.isEmpty()) return

    val safeIndex = selectedIndex.coerceIn(0, options.lastIndex)
    val chipBounds = remember { mutableStateMapOf<Int, Pair<Float, Float>>() }
    LaunchedEffect(options.size) {
        val staleKeys = chipBounds.keys.filter { it >= options.size }
        staleKeys.forEach { chipBounds.remove(it) }
    }

    val scrollState = rememberScrollState()
    var viewportWidthPx by remember { mutableIntStateOf(0) }
    var chipsRowOffsetX by remember { mutableFloatStateOf(0f) }

    val selectedBounds = chipBounds[safeIndex]
    val pillX = remember { Animatable(0f) }
    val pillWidth = remember { Animatable(0f) }
    var pillInitialized by remember { mutableStateOf(false) }
    var prevSelectedIndex by remember { mutableIntStateOf(safeIndex) }

    val stretchX = remember { Animatable(1f) }
    val stretchY = remember { Animatable(1f) }

    LaunchedEffect(safeIndex, selectedBounds) {
        val bounds = selectedBounds ?: return@LaunchedEffect
        if (!pillInitialized) {
            pillX.snapTo(bounds.first)
            pillWidth.snapTo(bounds.second)
            pillInitialized = true
            prevSelectedIndex = safeIndex
        } else if (prevSelectedIndex != safeIndex) {
            prevSelectedIndex = safeIndex
            launch {
                pillX.animateTo(
                    targetValue = bounds.first,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
            launch {
                pillWidth.animateTo(
                    targetValue = bounds.second,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
            launch {
                stretchX.animateTo(1.12f, animationSpec = tween(durationMillis = 80))
                stretchX.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
            launch {
                stretchY.animateTo(0.88f, animationSpec = tween(durationMillis = 80))
                stretchY.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
        } else {
            pillX.snapTo(bounds.first)
            pillWidth.snapTo(bounds.second)
        }
    }

    LaunchedEffect(safeIndex, selectedBounds, viewportWidthPx, chipsRowOffsetX) {
        val bounds = selectedBounds ?: return@LaunchedEffect
        if (viewportWidthPx <= 0 || scrollState.maxValue <= 0) return@LaunchedEffect
        val chipCenter = chipsRowOffsetX + bounds.first + bounds.second / 2f
        val targetScroll = (chipCenter - viewportWidthPx / 2f)
            .roundToInt()
            .coerceIn(0, scrollState.maxValue)
        scrollState.animateScrollTo(targetScroll)
    }

    val density = LocalDensity.current
    val chipCornerRadius = remember(density) {
        val radius = with(density) { 16.dp.toPx() }
        CornerRadius(radius, radius)
    }
    val surfaceColor = MaterialTheme.colorScheme.surface
    val activePillColor = MaterialTheme.colorScheme.primary
    val activeTextColor = MaterialTheme.colorScheme.onPrimary
    val inactiveTextColor = MaterialTheme.colorScheme.onSurface

    Row(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { viewportWidthPx = it.width }
            .horizontalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingContent != null) {
            leadingContent()
        }

        Box(
            modifier = Modifier
                .height(32.dp)
                .onGloballyPositioned { coords ->
                    val x = coords.positionInParent().x
                    if (chipsRowOffsetX != x) {
                        chipsRowOffsetX = x
                    }
                }
                .drawBehind {
                    for (index in options.indices) {
                        val bounds = chipBounds[index] ?: continue
                        drawRoundRect(
                            color = surfaceColor,
                            topLeft = Offset(bounds.first, 0f),
                            size = Size(bounds.second, size.height),
                            cornerRadius = chipCornerRadius
                        )
                    }
                    val currentWidth = pillWidth.value
                    if (currentWidth > 0f) {
                        val currentX = pillX.value
                        val pivot = Offset(currentX + currentWidth / 2f, size.height / 2f)
                        withTransform({
                            scale(
                                scaleX = stretchX.value,
                                scaleY = stretchY.value,
                                pivot = pivot
                            )
                        }) {
                            drawRoundRect(
                                color = activePillColor,
                                topLeft = Offset(currentX, 0f),
                                size = Size(currentWidth, size.height),
                                cornerRadius = chipCornerRadius
                            )
                        }
                    }
                }
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                options.forEachIndexed { index, label ->
                    val isSelected = index == safeIndex
                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) activeTextColor else inactiveTextColor,
                        label = "clay_chip_text"
                    )
                    Box(
                        modifier = Modifier
                            .height(32.dp)
                            .onGloballyPositioned { coords ->
                                val bounds = coords.positionInParent().x to coords.size.width.toFloat()
                                if (chipBounds[index] != bounds) {
                                    chipBounds[index] = bounds
                                }
                            }
                            .clip(RoundedCornerShape(16.dp))
                            .bounceClick(scaleDown = 0.92f) { onOptionSelected(index) }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = textColor,
                            maxLines = 1
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
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .bounceClick(onClick = onMeClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_figma_me),
                contentDescription = "Open Me",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

private fun formatCountdownTime(totalSeconds: Int): String {
    val safeSeconds = totalSeconds.coerceAtLeast(0)
    val hours = safeSeconds / 3600
    val mins = (safeSeconds % 3600) / 60
    val secs = safeSeconds % 60
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, mins, secs)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
    }
}

/**
 * Persistent bottom Live Focus Bar when timer is minimized.
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
    val timeString = formatCountdownTime(timerState.remainingSeconds)

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
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(timerState.isBarHidden, timerState.status) {
        if (!timerState.isBarHidden && timerState.status != TimerStatus.IDLE) {
            offsetX.snapTo(0f)
        }
    }

    val cardInteractionSource = remember { MutableInteractionSource() }
    val isCardPressed by cardInteractionSource.collectIsPressedAsState()
    val cardSquashY by animateFloatAsState(
        targetValue = if (isCardPressed && !isDragging) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "live_bar_squash_y"
    )
    val cardStretchX by animateFloatAsState(
        targetValue = if (isCardPressed && !isDragging) 1.007f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "live_bar_stretch_x"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .onSizeChanged { barWidthPx = it.width.toFloat() }
            .graphicsLayer {
                translationX = offsetX.value
                scaleX = if (isDragging) 1f else cardStretchX
                scaleY = if (isDragging) 1f else cardSquashY
                val fadeDistance = (if (barWidthPx > 0f) barWidthPx else 800f) * 0.85f
                alpha = (1f - (abs(offsetX.value) / fadeDistance)).coerceIn(0f, 1f)
            }
            .pointerInput(onSwipeDismiss) {
                detectHorizontalDragGestures(
                    onDragStart = { isDragging = true },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            offsetX.snapTo(offsetX.value + dragAmount)
                        }
                    },
                    onDragEnd = {
                        isDragging = false
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
                        isDragging = false
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
            .clickable(
                interactionSource = cardInteractionSource,
                indication = null,
                enabled = !isDragging,
                onClick = onExpandClick
            ),
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
                    color = LimeAccent,
                    maxLines = 1
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
    @DrawableRes iconRes: Int,
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
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                tint = MaterialTheme.colorScheme.primary
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
                ClayButton(
                    onClick = onActionClick,
                    height = 44.dp
                ) {
                    Text(actionLabel)
                }
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
                tint = MaterialTheme.colorScheme.primary
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
                ClayButton(
                    onClick = onActionClick,
                    height = 44.dp
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
            ClayButton(
                onClick = onConfirm,
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
                height = 40.dp,
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 18.dp)
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            ClayOutlinedButton(
                onClick = onDismiss,
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
