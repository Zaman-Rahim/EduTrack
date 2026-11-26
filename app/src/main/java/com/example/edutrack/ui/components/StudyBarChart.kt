package com.example.edutrack.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edutrack.ui.theme.ChartGridGreen
import com.example.edutrack.ui.theme.ChartMutedBarGreen
import com.example.edutrack.ui.theme.LimeAccent
import com.example.edutrack.ui.viewmodel.DailyStudyBar

/**
 * Custom Jetpack Compose Canvas bar chart rendering daily study hours over the last 7 days.
 * Exactly matches Figma `5:22` ("3h / 2h / 1h" gridlines, muted green `#82A38C` bars,
 * and Lime `#D8F478` highlighted day).
 */
@Composable
fun StudyBarChart(
    bars: List<DailyStudyBar>,
    modifier: Modifier = Modifier
) {
    if (bars.isEmpty()) return

    val maxHours = maxOf(3f, bars.maxOf { it.hours })
    val maxBarIndex = bars.indexOfMax()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Left Y-axis labels matching Figma 3h, 2h, 1h
            Column(
                modifier = Modifier
                    .width(26.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("3h", "2h", "1h", "").forEach { label ->
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        color = Color(0xFFD3E0D5)
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // Grid lines matching 3h, 2h, 1h
                listOf(0.06f, 0.36f, 0.66f).forEach { yFraction ->
                    val y = size.height * yFraction
                    drawLine(
                        color = ChartGridGreen,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 2f
                    )
                }

                val slotWidth = size.width / bars.size
                val barWidth = (slotWidth * 0.56f).coerceAtMost(24.dp.toPx())
                val corner = CornerRadius(7.dp.toPx(), 7.dp.toPx())

                bars.forEachIndexed { index, bar ->
                    val left = index * slotWidth + (slotWidth - barWidth) / 2f
                    val fraction = (bar.hours / maxHours).coerceIn(0f, 1f)

                    if (bar.minutes > 0) {
                        val activeHeight = maxOf(14f, size.height * 0.94f * fraction)
                        val barColor = if (index == maxBarIndex || (index == bars.lastIndex && bar.minutes > 0)) {
                            LimeAccent
                        } else {
                            ChartMutedBarGreen
                        }
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(left, size.height - activeHeight),
                            size = Size(barWidth, activeHeight),
                            cornerRadius = corner
                        )
                    } else {
                        drawRoundRect(
                            color = ChartGridGreen,
                            topLeft = Offset(left, size.height - 3.dp.toPx()),
                            size = Size(barWidth, 3.dp.toPx()),
                            cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            bars.forEachIndexed { index, bar ->
                val isHighlighted = index == maxBarIndex || (index == bars.lastIndex && bar.minutes > 0)
                Text(
                    text = bar.dayLabel.take(1),
                    fontSize = 11.sp,
                    fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
                    color = if (isHighlighted) LimeAccent else Color(0xFFD3E0D5),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun List<DailyStudyBar>.indexOfMax(): Int {
    var maxIdx = -1
    var maxVal = 0f
    forEachIndexed { index, bar ->
        if (bar.hours > maxVal) {
            maxVal = bar.hours
            maxIdx = index
        }
    }
    return maxIdx
}
