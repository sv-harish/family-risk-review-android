package com.familyriskreview.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val FrrLightColorScheme = lightColorScheme(
    primary = FrrColors.TealBlue,
    onPrimary = FrrColors.OnPrimary,
    primaryContainer = FrrColors.SoftAqua.copy(alpha = 0.25f),
    onPrimaryContainer = FrrColors.MidnightBlue,
    secondary = FrrColors.SlateNavy,
    onSecondary = FrrColors.CloudWhite,
    secondaryContainer = FrrColors.WarmMist,
    onSecondaryContainer = FrrColors.SlateNavy,
    tertiary = FrrColors.MutedGold,
    onTertiary = FrrColors.MidnightBlue,
    background = FrrColors.CloudWhite,
    onBackground = FrrColors.CharcoalText,
    surface = FrrColors.CloudWhite,
    onSurface = FrrColors.CharcoalText,
    surfaceVariant = FrrColors.WarmMist,
    onSurfaceVariant = FrrColors.SlateNavy,
    outline = FrrColors.Outline,
    error = FrrColors.Error,
)

private val FrrShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

/**
 * Family Risk Review Material 3 theme.
 * Dark mode is intentionally not the default product look; the experience is light.
 */
@Composable
fun FamilyRiskReviewTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // Product direction: predominantly light. Ignore system dark for customer flow.
    MaterialTheme(
        colorScheme = FrrLightColorScheme,
        typography = FrrTypography,
        shapes = FrrShapes,
        content = content,
    )
}
