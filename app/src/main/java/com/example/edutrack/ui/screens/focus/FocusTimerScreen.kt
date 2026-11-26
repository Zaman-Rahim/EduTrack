package com.example.edutrack.ui.screens.focus

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edutrack.R
import com.example.edutrack.ui.components.FigmaBackButton
import com.example.edutrack.ui.components.bounceClick
import com.example.edutrack.ui.theme.CardLightGreen
import com.example.edutrack.ui.theme.ForestGreenDark
import com.example.edutrack.ui.theme.ForestGreenPrimary
import com.example.edutrack.ui.theme.LimeAccent
import com.example.edutrack.ui.theme.TrackBorder
import com.example.edutrack.ui.timer.FocusTimerUiState
import com.example.edutrack.ui.timer.TimerStatus
import com.example.edutrack.util.DateTimeUtils
import java.util.Locale

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
    modifier: Modifier = Modifier
) {
    val subjectName = timerState.subject?.name ?: "Study Focus"

    if (timerState.status == TimerStatus.COMPLETED) {
        FocusCompleteView(
            subjectName = subjectName,
            onBackClick = onBackToPlanClick,
            onOpenNotificationsClick = onOpenNotificationsClick,
            modifier = modifier
        )
        return
    }

    val mins = timerState.remainingSeconds / 60
    val secs = timerState.remainingSeconds % 60
    val formattedCountdown = String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
    val animatedSweep by animateFloatAsState(
        targetValue = 360f * timerState.progressFraction,
        label = "timer_dial_sweep"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
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
                    .bounceClick(onClick = onMinimizeClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_figma_down_arrow),
                    contentDescription = "Minimise focus timer",
                    tint = ForestGreenDark,
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

        // Circular Countdown Dial (270dp)
        Box(
            modifier = Modifier.size(264.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 14.dp.toPx()
                drawCircle(
                    color = TrackBorder,
                    style = Stroke(width = strokeWidth)
                )
                drawArc(
                    color = ForestGreenPrimary,
                    startAngle = -90f,
                    sweepAngle = animatedSweep,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = formattedCountdown,
                    fontSize = 62.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-2).sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "MINUTES : SECONDS",
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

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onPauseResumeClick,
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ForestGreenPrimary,
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .bounceClick(onClick = onPauseResumeClick)
        ) {
            Text(
                text = if (timerState.status == TimerStatus.PAUSED) "Resume session" else "Pause session",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        TextButton(onClick = onEndWithoutCompletingClick) {
            Text(
                text = "End without completing",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FocusCompleteView(
    subjectName: String,
    onBackClick: () -> Unit,
    onOpenNotificationsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            FigmaBackButton(onClick = onBackClick)
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
            colors = CardDefaults.cardColors(containerColor = CardLightGreen),
            border = BorderStroke(1.dp, Color(0xFFC2D1B8))
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
                    tint = ForestGreenPrimary,
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
                        color = ForestGreenDark
                    )
                    Text(
                        text = "Your daily focus is complete.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ForestGreenDark
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
                .bounceClick(onClick = onOpenNotificationsClick),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardLightGreen)
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
                    color = ForestGreenDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Your completed subject is waiting below.\nChoose the next one from today’s list.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onBackClick,
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ForestGreenPrimary,
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .bounceClick(onClick = onBackClick)
        ) {
            Text(
                text = "Back to today’s plan",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
