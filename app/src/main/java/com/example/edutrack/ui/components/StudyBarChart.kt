package com.example.edutrack.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edutrack.ui.viewmodel.DailyStudyBar
import com.example.edutrack.util.DateTimeUtils
import kotlin.math.roundToInt

private val CHART_GRID_FRACTIONS = listOf(0f, 1f / 3f, 2f / 3f)
private val CHART_TOP_INSET = 8.dp
private val CHART_CANVAS_HEIGHT = 140.dp

/**
 * Custom Jetpack Compose Canvas bar chart rendering daily study durations over the last 7 days.
 * Dynamically scales its Y-axis ceiling and ticks (`1h / 40m / 20m`, `1h 30m / 1h / 30m`,
 * `3h / 2h / 1h`, or multiples of `90m`), animates bar heights smoothly, and supports
 * tap-to-inspect daily duration pills.
 */
@Composable
fun StudyBarChart(
    bars: List<DailyStudyBar>,
    modifier: Modifier = Modifier,
    highlightedBarColor: Color = MaterialTheme.colorScheme.primary,
    barColor: Color = MaterialTheme.colorScheme.primaryContainer,
    gridColor: Color = MaterialTheme.colorScheme.outline,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    highlightedLabelColor: Color = MaterialTheme.colorScheme.primary,
    badgeContainerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    badgeContentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    if (bars.isEmpty()) return

    val peakMinutes = bars.maxOfOrNull { it.minutes } ?: 0
    val maxMinutes = computeMaxChartMinutes(peakMinutes)
    val maxBarIndex = bars.indexOfMaxMinutes()

    val tickLabels = remember(maxMinutes) {
        listOf(
            DateTimeUtils.formatDuration(maxMinutes),
            DateTimeUtils.formatDuration((maxMinutes * 2) / 3),
            DateTimeUtils.formatDuration(maxMinutes / 3)
        )
    }

    var selectedBarIndex by remember { mutableStateOf<Int?>(null) }
    val safeSelectedIndex = selectedBarIndex?.takeIf { it in bars.indices }
    val selectedBar = safeSelectedIndex?.let { bars[it] }

    val animatedFractions = bars.mapIndexed { index, bar ->
        val rawFraction = if (bar.minutes > 0) {
            maxOf(0.05f, bar.minutes.toFloat() / maxMinutes.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
        val animated by animateFloatAsState(
            targetValue = rawFraction,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "study_bar_fraction_$index"
        )
        animated.coerceIn(0f, 1f)
    }

    val density = LocalDensity.current
    val barCornerRadius = remember(density) {
        val radius = with(density) { 7.dp.toPx() }
        CornerRadius(radius, radius)
    }
    val zeroBarCornerRadius = remember(density) {
        val radius = with(density) { 1.5.dp.toPx() }
        CornerRadius(radius, radius)
    }
    val maxBarWidthPx = remember(density) { with(density) { 24.dp.toPx() } }
    val zeroBarHeightPx = remember(density) { with(density) { 3.dp.toPx() } }
    val gridStrokeWidthPx = remember(density) { with(density) { 1.dp.toPx() } }

    Column(modifier = modifier.fillMaxWidth()) {
        AnimatedVisibility(
            visible = selectedBar != null,
            enter = fadeIn(animationSpec = tween(150)) + expandVertically(),
            exit = fadeOut(animationSpec = tween(150)) + shrinkVertically()
        ) {
            selectedBar?.let { bar ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = badgeContainerColor,
                        contentColor = badgeContentColor
                    ) {
                        Text(
                            text = "${bar.dayLabel} · ${DateTimeUtils.formatDuration(bar.minutes)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Y-axis tick labels aligned mathematically with Canvas gridlines (0f, 1/3, 2/3)
            Layout(
                content = {
                    tickLabels.forEach { label ->
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            color = labelColor,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                },
                modifier = Modifier
                    .widthIn(min = 30.dp)
                    .height(CHART_CANVAS_HEIGHT)
                    .padding(top = CHART_TOP_INSET)
            ) { measurables, constraints ->
                val placeables = measurables.map {
                    it.measure(constraints.copy(minWidth = 0, minHeight = 0))
                }
                val totalHeight = constraints.maxHeight
                val width = (placeables.maxOfOrNull { it.width } ?: 0)
                    .coerceAtLeast(constraints.minWidth)
                layout(width, totalHeight) {
                    CHART_GRID_FRACTIONS.forEachIndexed { index, fraction ->
                        val placeable = placeables.getOrNull(index) ?: return@forEachIndexed
                        val centerY = (totalHeight * fraction).roundToInt()
                        val y = (centerY - placeable.height / 2)
                            .coerceIn(-placeable.height / 2, totalHeight - placeable.height / 2)
                        placeable.placeRelative(x = width - placeable.width, y = y)
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(CHART_CANVAS_HEIGHT)
                        .padding(top = CHART_TOP_INSET)
                        .pointerInput(bars.size) {
                            detectTapGestures { tapOffset ->
                                val slotWidth = size.width.toFloat() / bars.size
                                if (slotWidth > 0f) {
                                    val tappedIndex = (tapOffset.x / slotWidth)
                                        .toInt()
                                        .coerceIn(0, bars.lastIndex)
                                    selectedBarIndex = if (selectedBarIndex == tappedIndex) null else tappedIndex
                                }
                            }
                        }
                ) {
                    val drawableHeight = size.height
                    for (fraction in CHART_GRID_FRACTIONS) {
                        val y = drawableHeight * fraction
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = gridStrokeWidthPx
                        )
                    }

                    val slotWidth = size.width / bars.size
                    val barWidth = (slotWidth * 0.56f).coerceAtMost(maxBarWidthPx)

                    bars.forEachIndexed { index, bar ->
                        val left = index * slotWidth + (slotWidth - barWidth) / 2f
                        val isHighlighted = if (safeSelectedIndex != null) {
                            index == safeSelectedIndex
                        } else {
                            index == maxBarIndex || (index == bars.lastIndex && bar.minutes > 0)
                        }

                        if (bar.minutes > 0) {
                            val activeHeight = (drawableHeight * animatedFractions[index])
                                .coerceAtLeast(zeroBarHeightPx)
                            drawRoundRect(
                                color = if (isHighlighted) highlightedBarColor else barColor,
                                topLeft = Offset(left, drawableHeight - activeHeight),
                                size = Size(barWidth, activeHeight),
                                cornerRadius = barCornerRadius
                            )
                        } else {
                            drawRoundRect(
                                color = if (safeSelectedIndex == index) highlightedBarColor else gridColor,
                                topLeft = Offset(left, drawableHeight - zeroBarHeightPx),
                                size = Size(barWidth, zeroBarHeightPx),
                                cornerRadius = zeroBarCornerRadius
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    bars.forEachIndexed { index, bar ->
                        val isHighlighted = if (safeSelectedIndex != null) {
                            index == safeSelectedIndex
                        } else {
                            index == maxBarIndex || (index == bars.lastIndex && bar.minutes > 0)
                        }
                        val labelInteraction = remember { MutableInteractionSource() }
                        Text(
                            text = bar.dayLabel.take(1),
                            fontSize = 11.sp,
                            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
                            color = if (isHighlighted) highlightedLabelColor else labelColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clickable(
                                    interactionSource = labelInteraction,
                                    indication = null
                                ) {
                                    selectedBarIndex = if (selectedBarIndex == index) null else index
                                }
                        )
                    }
                }
            }
        }
    }
}

private fun computeMaxChartMinutes(peakMinutes: Int): Int = when {
    peakMinutes <= 60 -> 60
    peakMinutes <= 90 -> 90
    peakMinutes <= 180 -> 180
    else -> ((peakMinutes + 89) / 90) * 90
}

private fun List<DailyStudyBar>.indexOfMaxMinutes(): Int {
    var maxIdx = -1
    var maxVal = 0
    forEachIndexed { index, bar ->
        if (bar.minutes > maxVal) {
            maxVal = bar.minutes
            maxIdx = index
        }
    }
    return maxIdx
}
