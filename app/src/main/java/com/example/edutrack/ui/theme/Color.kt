package com.example.edutrack.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

// EduTrack Figma Design System Colors
val ForestGreenPrimary = Color(0xFF194D3E)
val ForestGreenDark = Color(0xFF183D33)
val LimeAccent = Color(0xFFD8F478)
val LimeLight = Color(0xFFE9F8B5)

val LinenBackground = Color(0xFFF6F7F2)
val SurfaceWhite = Color(0xFFFFFFFF)
val MutedText = Color(0xFF6C776F)
val TrackBorder = Color(0xFFE2E7DD)
val CardLightGreen = Color(0xFFEAF0E7)
val ChartGridGreen = Color(0xFF3B6658)
val ChartMutedBarGreen = Color(0xFF82A38C)

// Soft Pastel Accent Palette for Subjects & Cards (Exact Figma Swatches)
val PastelLavender = Color(0xFFE9E2F7)
val PastelPeach = Color(0xFFF7E8D5)
val PastelTeal = Color(0xFFDCEBF2)

// Destructive / Warning actions (Exact Figma Session Details Delete Pill)
val DeleteRedBg = Color(0xFFF9E7E2)
val DeleteRedText = Color(0xFFA14336)
val DeleteRedBgDark = Color(0xFF42221F)
val DeleteRedTextDark = Color(0xFFF4AEA4)

// Dark Theme Variants
val DarkBackgroundFigma = Color(0xFF0F1E19)
val DarkSurfaceFigma = Color(0xFF162D26)
val DarkSurfaceVariantFigma = Color(0xFF1E3B33)
val DarkBorderFigma = Color(0xFF284D42)

/**
 * Exact 5 curated pastel color swatches from Figma EduTrack design.
 */
val SubjectTagPalette: List<Long> = listOf(
    0xFFE9E2F7, // Lavender (Option 1)
    0xFFD8F478, // Lime (Option 2)
    0xFFF7E8D5, // Peach / Sand (Option 3)
    0xFFF0DDEA, // Soft Lilac (Option 4)
    0xFFDCEBF2  // Pale Sky / Teal (Option 5)
)

/**
 * Returns the exact pastel container color for a subject tag.
 * If the stored color is already a bright Figma pastel, it is returned directly;
 * otherwise it is tinted toward white for harmonious pastel consistency.
 */
fun subjectPastelContainer(colorHex: Long): Color {
    val raw = Color(colorHex)
    return if (raw.luminance() > 0.55f) raw else lerp(Color.White, raw, 0.25f)
}
