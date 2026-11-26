package com.example.edutrack.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = ForestGreenPrimary,
    onPrimary = SurfaceWhite,
    primaryContainer = CardLightGreen,
    onPrimaryContainer = ForestGreenDark,
    secondary = LimeAccent,
    onSecondary = ForestGreenDark,
    secondaryContainer = LimeLight,
    onSecondaryContainer = ForestGreenDark,
    tertiary = PastelLavender,
    onTertiary = ForestGreenDark,
    tertiaryContainer = PastelPeach,
    onTertiaryContainer = ForestGreenDark,
    background = LinenBackground,
    onBackground = ForestGreenDark,
    surface = SurfaceWhite,
    onSurface = ForestGreenDark,
    surfaceVariant = LinenBackground,
    onSurfaceVariant = MutedText,
    outline = TrackBorder
)

private val DarkColors = darkColorScheme(
    primary = LimeAccent,
    onPrimary = ForestGreenDark,
    primaryContainer = DarkSurfaceVariantFigma,
    onPrimaryContainer = LimeAccent,
    secondary = PastelTeal,
    onSecondary = ForestGreenDark,
    secondaryContainer = DarkSurfaceVariantFigma,
    onSecondaryContainer = SurfaceWhite,
    tertiary = PastelLavender,
    onTertiary = ForestGreenDark,
    tertiaryContainer = DarkSurfaceFigma,
    onTertiaryContainer = SurfaceWhite,
    background = DarkBackgroundFigma,
    onBackground = SurfaceWhite,
    surface = DarkSurfaceFigma,
    onSurface = SurfaceWhite,
    surfaceVariant = DarkSurfaceVariantFigma,
    onSurfaceVariant = Color(0xFFA5B8B1),
    outline = DarkBorderFigma
)

@Composable
fun EduTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = EduTrackTypography,
        content = content
    )
}
