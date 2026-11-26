package com.example.edutrack.ui.screens.notifications

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edutrack.R
import com.example.edutrack.data.local.entity.NotificationEntity
import com.example.edutrack.data.local.entity.NotificationType
import com.example.edutrack.ui.components.EmptyStateCard
import com.example.edutrack.ui.components.FigmaBackButton
import com.example.edutrack.ui.components.bounceClick
import com.example.edutrack.ui.theme.CardLightGreen
import com.example.edutrack.ui.theme.ForestGreenDark
import com.example.edutrack.ui.theme.ForestGreenPrimary
import com.example.edutrack.ui.viewmodel.NotificationsUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Exact Figma `24:35` ("EduTrack · Notifications") inbox showing completed focus goals and reminder alerts.
 */
@Composable
fun NotificationsScreen(
    uiState: NotificationsUiState,
    onBackClick: () -> Unit,
    onMeClick: () -> Unit,
    onMarkAllReadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        item {
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
                        text = "Notifications",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
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

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Study wins and reminders, kept together.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (uiState.unreadCount > 0) {
                    TextButton(onClick = onMarkAllReadClick) {
                        Text(
                            text = "Mark all as read",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ForestGreenPrimary
                        )
                    }
                }
            }
        }

        if (uiState.notifications.isEmpty()) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
                EmptyStateCard(
                    icon = Icons.Outlined.NotificationsNone,
                    title = "No notifications yet",
                    description = "Completed study timers and scheduled reminders will appear here."
                )
            }
        } else {
            items(uiState.notifications, key = { "notif_${it.id}" }) { notification ->
                NotificationHistoryCard(notification = notification)
            }
        }
    }
}

@Composable
private fun NotificationHistoryCard(notification: NotificationEntity) {
    val timeFormatter = SimpleDateFormat("dd MMM · h:mm a", Locale.getDefault())
    val formattedTime = timeFormatter.format(Date(notification.createdAt))
    val iconRes = when (notification.type) {
        NotificationType.FOCUS_COMPLETE.name -> R.drawable.ic_figma_check
        else -> R.drawable.ic_figma_bell
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = 1.dp,
            color = if (!notification.isRead) ForestGreenPrimary.copy(alpha = 0.45f) else Color(0xFFCCD8C4)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(CardLightGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = ForestGreenPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = formattedTime,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
