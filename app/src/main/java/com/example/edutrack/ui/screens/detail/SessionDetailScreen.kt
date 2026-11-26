package com.example.edutrack.ui.screens.detail

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edutrack.R
import com.example.edutrack.ui.components.ConfirmDeleteDialog
import com.example.edutrack.ui.components.EmptyStateCard
import com.example.edutrack.ui.components.FigmaBackButton
import com.example.edutrack.ui.components.figmaSubjectIconRes
import com.example.edutrack.ui.theme.DeleteRedBg
import com.example.edutrack.ui.theme.DeleteRedText
import com.example.edutrack.ui.theme.ForestGreenDark
import com.example.edutrack.ui.theme.ForestGreenPrimary
import com.example.edutrack.ui.theme.TrackBorder
import com.example.edutrack.ui.theme.subjectPastelContainer
import com.example.edutrack.ui.viewmodel.SessionDetailUiState
import com.example.edutrack.util.DateTimeUtils

@Composable
fun SessionDetailScreen(
    uiState: SessionDetailUiState,
    onEditClick: (Long) -> Unit,
    onDeleteConfirm: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    val detail = uiState.sessionWithSubject
    if (!uiState.isLoading && detail == null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            FigmaBackButton(onClick = onBackClick)
            Spacer(modifier = Modifier.height(16.dp))
            EmptyStateCard(
                icon = Icons.Outlined.SearchOff,
                title = "Session not found",
                description = "This study session may have been deleted.",
                actionLabel = "Go Back",
                onActionClick = onBackClick
            )
        }
        return
    }

    if (detail == null) return

    val heroBg = subjectPastelContainer(detail.subject.colorHex)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Exact Figma Header with white circular back button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            FigmaBackButton(onClick = onBackClick)
            Text(
                text = "Session details",
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Exact Figma Hero Card (rounded 26dp, full pastel tint, white 48dp square icon + Logged pill)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(
                containerColor = heroBg
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
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = figmaSubjectIconRes(detail.subject.name)),
                            contentDescription = null,
                            tint = ForestGreenDark,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_figma_check),
                                contentDescription = null,
                                tint = ForestGreenPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Logged",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = ForestGreenPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = detail.subject.name,
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = ForestGreenDark
                )

                if (detail.session.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = detail.session.notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF6C776F),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = DateTimeUtils.formatDuration(detail.session.durationMinutes),
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = 36.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = ForestGreenDark
                )
            }
        }

        Text(
            text = "The details",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_figma_calendar),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = DateTimeUtils.formatFullDate(detail.session.sessionDateEpochDay),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = TrackBorder
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_figma_clock),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "${detail.session.durationMinutes} minutes • Goal: ${detail.subject.weeklyGoalHours} hrs/week",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Text(
            text = "Session notes",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Text(
                    text = detail.session.notes.ifBlank { "No additional notes provided for this study session." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Exact Figma bottom bar: ForestGreen "Edit session" + #F9E7E2 "Delete"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { onEditClick(detail.session.id) },
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ForestGreenPrimary,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            ) {
                Text(
                    text = "Edit session",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Button(
                onClick = { showDeleteDialog = true },
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DeleteRedBg,
                    contentColor = DeleteRedText
                ),
                modifier = Modifier
                    .width(92.dp)
                    .height(56.dp)
            ) {
                Text(
                    text = "Delete",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    if (showDeleteDialog) {
        ConfirmDeleteDialog(
            title = "Delete Study Session?",
            message = "This will permanently remove the ${DateTimeUtils.formatDuration(detail.session.durationMinutes)} session for ${detail.subject.name}.",
            onConfirm = {
                showDeleteDialog = false
                onDeleteConfirm()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}
