package com.example.edutrack.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edutrack.ui.theme.DarkBorderFigma
import com.example.edutrack.ui.theme.ForestGreenPrimary
import com.example.edutrack.ui.theme.LimeAccent
import com.example.edutrack.ui.theme.MutedText
import com.example.edutrack.ui.theme.TrackBorder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * Tab descriptor for [ClayBottomNavBar].
 */
data class ClayNavTab(
    val label: String,
    @DrawableRes val iconRes: Int
)

/**
 * Custom rubbery, clay-textured animated bottom navigation bar for EduTrack's 5 top-level tabs.
 *
 * Tracks [selectedIndex] + [pageOffsetFraction] smoothly as the user swipes across a
 * `HorizontalPager` and applies a bouncy rubber-band stretch to the sliding [LimeAccent] clay pill
 * along with a jelly squash-and-stretch bounce on the selected tab icon.
 */
@Composable
fun ClayBottomNavBar(
    tabs: List<ClayNavTab>,
    selectedIndex: Int,
    pageOffsetProvider: () -> Float,
    isDarkTheme: Boolean,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (tabs.isEmpty()) return

    val safeSelectedIndex = selectedIndex.coerceIn(0, tabs.lastIndex)
    val dividerColor = if (isDarkTheme) DarkBorderFigma else TrackBorder
    val unselectedColor = if (isDarkTheme) MaterialTheme.colorScheme.onSurfaceVariant else MutedText

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(color = dividerColor, thickness = 1.dp)

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .height(76.dp)
                    .padding(horizontal = 8.dp)
            ) {
                val tabWidth = maxWidth / tabs.size
                val pillWidth = 62.dp
                val pillHeight = 40.dp
                val pillTopOffset = 7.dp

                // Zero-recomposition GPU-driven sliding 3D clay-textured LimeAccent pill
                ClayIndicatorPill(
                    isDarkTheme = isDarkTheme,
                    modifier = Modifier
                        .offset {
                            val clampedOffset = pageOffsetProvider().coerceIn(-1f, 1f)
                            val rawPos = (safeSelectedIndex + clampedOffset)
                                .coerceIn(0f, tabs.lastIndex.toFloat())
                            val offsetX = tabWidth * rawPos + (tabWidth - pillWidth) / 2f
                            IntOffset(
                                x = offsetX.roundToPx(),
                                y = pillTopOffset.roundToPx()
                            )
                        }
                        .graphicsLayer {
                            val clampedOffset = pageOffsetProvider().coerceIn(-1f, 1f)
                            val stretch = sin(abs(clampedOffset) * PI).toFloat()
                            scaleX = 1f + 0.36f * stretch
                            scaleY = 1f - 0.12f * stretch
                        }
                        .size(width = pillWidth, height = pillHeight)
                )

                // Tab items (isolated sub-composables to prevent unselected tab recompositions)
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.Top
                ) {
                    tabs.forEachIndexed { index, tab ->
                        ClayNavTabItem(
                            tab = tab,
                            isSelected = index == safeSelectedIndex,
                            unselectedColor = unselectedColor,
                            isDarkTheme = isDarkTheme,
                            pillHeight = pillHeight,
                            pillTopOffset = pillTopOffset,
                            onTabSelected = { onTabSelected(index) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.ClayNavTabItem(
    tab: ClayNavTab,
    isSelected: Boolean,
    unselectedColor: Color,
    isDarkTheme: Boolean,
    pillHeight: Dp,
    pillTopOffset: Dp,
    onTabSelected: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    val iconScaleX by animateFloatAsState(
        targetValue = if (isSelected) 1.14f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "clayIconScaleX"
    )
    val iconScaleY by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "clayIconScaleY"
    )

    val iconTint by animateColorAsState(
        targetValue = if (isSelected) ForestGreenPrimary else unselectedColor,
        label = "clayIconTint"
    )
    val labelColor by animateColorAsState(
        targetValue = if (isSelected) {
            if (isDarkTheme) LimeAccent else ForestGreenPrimary
        } else {
            unselectedColor
        },
        label = "clayLabelColor"
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onTabSelected
            )
            .padding(top = pillTopOffset),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Box(
            modifier = Modifier
                .height(pillHeight)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = tab.iconRes),
                contentDescription = tab.label,
                tint = iconTint,
                modifier = Modifier
                    .size(21.dp)
                    .graphicsLayer {
                        scaleX = iconScaleX
                        scaleY = iconScaleY
                    }
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = tab.label,
            color = labelColor,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

/**
 * Flat Figma LimeAccent sliding pill indicator with continuous rubber-band stretch
 * during pager swipe transitions (no 3D bevels or heavy drop shadows).
 */
@Composable
private fun ClayIndicatorPill(
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val pillShape = RoundedCornerShape(50)
    val pillColor = if (isDarkTheme) LimeAccent else LimeAccent

    Box(
        modifier = modifier
            .clip(pillShape)
            .background(pillColor)
    )
}
