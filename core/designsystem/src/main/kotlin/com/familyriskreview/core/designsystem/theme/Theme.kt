package com.familyriskreview.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val FrrLightColorScheme =
    lightColorScheme(
        primary = FrrColors.Teal,
        onPrimary = FrrColors.Elevated,
        primaryContainer = FrrColors.TealSoft.copy(alpha = 0.22f),
        onPrimaryContainer = FrrColors.Ink,
        secondary = FrrColors.DeepInk,
        onSecondary = FrrColors.Elevated,
        secondaryContainer = FrrColors.SoftNeutral,
        onSecondaryContainer = FrrColors.DeepInk,
        tertiary = FrrColors.Amber,
        onTertiary = FrrColors.Ink,
        background = FrrColors.WarmOffWhite,
        onBackground = FrrColors.Ink,
        surface = FrrColors.WarmOffWhite,
        onSurface = FrrColors.Ink,
        surfaceVariant = FrrColors.SoftNeutral,
        onSurfaceVariant = FrrColors.DeepInk,
        outline = FrrColors.Outline,
        error = FrrColors.BlockingError,
    )

private val FrrShapes =
    Shapes(
        extraSmall = RoundedCornerShape(FrrRadius.xs),
        small = RoundedCornerShape(FrrRadius.sm),
        medium = RoundedCornerShape(FrrRadius.md),
        large = RoundedCornerShape(FrrRadius.lg),
        extraLarge = RoundedCornerShape(FrrRadius.xl),
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
    CompositionLocalProvider(LocalFrrSemanticColors provides FrrSemanticColors()) {
        MaterialTheme(
            colorScheme = FrrLightColorScheme,
            typography = FrrTypography,
            shapes = FrrShapes,
            content = content,
        )
    }
}

@Composable
fun frrColors(): FrrSemanticColors = LocalFrrSemanticColors.current
