package com.example.edutrack.ui.screens.stats

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edutrack.R
import com.example.edutrack.ui.components.EmptyStateCard
import com.example.edutrack.ui.components.StudyBarChart
import com.example.edutrack.ui.components.SubjectColorDot
import com.example.edutrack.ui.theme.ForestGreenDark
import com.example.edutrack.ui.theme.ForestGreenPrimary
import com.example.edutrack.ui.theme.LimeAccent
import com.example.edutrack.ui.theme.TrackBorder
import com.example.edutrack.ui.viewmodel.StatsUiState
import com.example.edutrack.ui.viewmodel.SubjectStatsItem
import com.example.edutrack.util.DateTimeUtils
import java.util.Locale

@Composable
fun StatsScreen(
    uiState: StatsUiState,
    onLogSessionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (weekStart, weekEnd) = DateTimeUtils.currentWeekBounds()
    val weekRangeText = "${DateTimeUtils.formatShortDate(weekStart)} – ${DateTimeUtils.formatShortDate(weekEnd)}"

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Exact Figma Header
        item {
            Column {
                Text(
                    text = "Your progress",
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Look how far your focus has taken you.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Exact Figma Week selector pill (22dp rounded) with Figma back & arrow icons
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_figma_back),
                        contentDescription = "Previous week",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = weekRangeText,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.ic_figma_arrow),
                        contentDescription = "Next week",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Figma Main Weekly Study Chart Card (Forest Green, 24dp rounded)
        item {
            val totalGoalHours = uiState.subjectStats.sumOf { it.subject.weeklyGoalHours }
            val completedHours = uiState.totalWeeklyMinutes / 60.0
            val percentage = if (totalGoalHours > 0) {
                ((completedHours / totalGoalHours) * 100).toInt()
            } else 0

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = ForestGreenPrimary
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "STUDY TIME THIS WEEK",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFD3E0D5),
                            letterSpacing = 0.8.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(15.dp),
                            color = LimeAccent
                        ) {
                            Text(
                                text = "$percentage% of goal",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = ForestGreenPrimary,
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = DateTimeUtils.formatDuration(uiState.totalWeeklyMinutes),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontSize = 36.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    StudyBarChart(bars = uiState.dailyBars)
                }
            }
        }

        // Side-by-Side Metric Cards: Left Lime (Streak), Right White (Per active session)
        item {
            val averageMinutes = if (uiState.totalSessionsCount > 0) {
                uiState.totalAllTimeMinutes / uiState.totalSessionsCount
            } else 0

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = LimeAccent
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "${uiState.currentStreakDays} days",
                            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 25.sp),
                            fontWeight = FontWeight.SemiBold,
                            color = ForestGreenDark
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Current streak",
                            style = MaterialTheme.typography.bodySmall,
                            color = ForestGreenDark
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = DateTimeUtils.formatDuration(averageMinutes),
                            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 25.sp),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Per active session",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Exact Figma "Time by subject" flat clean rows
        item {
            Text(
                text = "Time by subject",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (!uiState.isLoading && uiState.subjectStats.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Outlined.BarChart,
                    title = "No progress data yet",
                    description = "Log study sessions to visualize subject-level breakdowns and streaks.",
                    actionLabel = "Log Session",
                    onActionClick = onLogSessionClick
                )
            }
        } else {
            items(uiState.subjectStats, key = { "stats_subject_${it.subject.id}" }) { item ->
                FigmaSubjectProgressRow(item = item)
            }
        }
    }
}

@Composable
private fun FigmaSubjectProgressRow(item: SubjectStatsItem) {
    val totalHours = item.allTimeMinutes / 60.0
    val targetHours = maxOf(0.1, item.subject.weeklyGoalHours)
    val fraction = (item.weeklyHours / targetHours).coerceIn(0.0, 1.0).toFloat()

    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        label = "subject_progress_row"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SubjectColorDot(colorHex = item.subject.colorHex, size = 10.dp)
                Text(
                    text = item.subject.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = String.format(Locale.getDefault(), "%.1fh", totalHours),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { animatedFraction },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp)
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = ForestGreenPrimary,
            trackColor = TrackBorder,
            strokeCap = StrokeCap.Round
        )
    }
}
