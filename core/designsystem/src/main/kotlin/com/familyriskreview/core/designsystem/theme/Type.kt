package com.familyriskreview.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Typography foundation for Family Risk Review.
 *
 * Intended families (bundle under res/font when assets are available):
 * - Display / headings: Manrope
 * - Body / UI: Source Sans 3
 * - Tamil: Noto Sans Tamil
 * - Hindi: Noto Sans Devanagari
 *
 * Until font files ship, we use a purposeful weight/size/letter-spacing scale
 * rather than Material defaults alone. See assets/fonts/README.md.
 */
val FrrFontFamily = FontFamily.SansSerif

val FrrHeadingFontFamily = FontFamily.SansSerif

val FrrTypography =
    Typography(
        displayLarge =
            TextStyle(
                fontFamily = FrrHeadingFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 40.sp,
                lineHeight = 48.sp,
                letterSpacing = (-0.4).sp,
                color = FrrColors.Ink,
            ),
        displayMedium =
            TextStyle(
                fontFamily = FrrHeadingFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 32.sp,
                lineHeight = 40.sp,
                letterSpacing = (-0.2).sp,
                color = FrrColors.Ink,
            ),
        headlineLarge =
            TextStyle(
                fontFamily = FrrHeadingFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 28.sp,
                lineHeight = 36.sp,
                color = FrrColors.Ink,
            ),
        headlineMedium =
            TextStyle(
                fontFamily = FrrHeadingFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 24.sp,
                lineHeight = 32.sp,
                color = FrrColors.DeepInk,
            ),
        titleLarge =
            TextStyle(
                fontFamily = FrrFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                lineHeight = 28.sp,
                color = FrrColors.Ink,
            ),
        titleMedium =
            TextStyle(
                fontFamily = FrrFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                color = FrrColors.Ink,
            ),
        bodyLarge =
            TextStyle(
                fontFamily = FrrFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 26.sp,
                color = FrrColors.Ink,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = FrrFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                color = FrrColors.Ink,
            ),
        labelLarge =
            TextStyle(
                fontFamily = FrrFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = FrrColors.Ink,
            ),
        labelMedium =
            TextStyle(
                fontFamily = FrrFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = FrrColors.MutedText,
            ),
    )
