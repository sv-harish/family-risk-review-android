package com.familyriskreview.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Typography foundation.
 *
 * Recommended families (to be bundled as font files under res/font):
 * - English headings: Manrope
 * - English body/UI: Inter
 * - Tamil: Noto Sans Tamil
 * - Hindi: Noto Sans Devanagari
 *
 * Phase 0 uses platform defaults with documented placeholders until font files
 * are added. See assets/fonts/README.md.
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
            letterSpacing = (-0.5).sp,
            color = FrrColors.MidnightBlue,
        ),
        displayMedium =
        TextStyle(
            fontFamily = FrrHeadingFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            color = FrrColors.MidnightBlue,
        ),
        headlineLarge =
        TextStyle(
            fontFamily = FrrHeadingFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            color = FrrColors.MidnightBlue,
        ),
        headlineMedium =
        TextStyle(
            fontFamily = FrrHeadingFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            color = FrrColors.SlateNavy,
        ),
        titleLarge =
        TextStyle(
            fontFamily = FrrFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            lineHeight = 28.sp,
            color = FrrColors.CharcoalText,
        ),
        titleMedium =
        TextStyle(
            fontFamily = FrrFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            color = FrrColors.CharcoalText,
        ),
        bodyLarge =
        TextStyle(
            fontFamily = FrrFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            color = FrrColors.CharcoalText,
        ),
        bodyMedium =
        TextStyle(
            fontFamily = FrrFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = FrrColors.CharcoalText,
        ),
        labelLarge =
        TextStyle(
            fontFamily = FrrFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = FrrColors.CharcoalText,
        ),
        labelMedium =
        TextStyle(
            fontFamily = FrrFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = FrrColors.CharcoalText,
        ),
    )
