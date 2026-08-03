package com.familyriskreview.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * Family Risk Review colour tokens.
 * Continuity Line visual system — predominantly light experience.
 */
object FrrColors {
    val MidnightBlue = Color(0xFF122033)
    val SlateNavy = Color(0xFF1B2D42)
    val TealBlue = Color(0xFF2D9CDB)
    val SoftAqua = Color(0xFF56C6D6)
    val MutedGold = Color(0xFFD6A85F)
    val SoftSand = Color(0xFFF5EBDD)
    val CloudWhite = Color(0xFFF8FAFC)
    val WarmMist = Color(0xFFEEF2F6)
    val CharcoalText = Color(0xFF243244)

    val ContinuityLine = TealBlue
    val ContinuityLineFaded = SoftAqua.copy(alpha = 0.45f)
    val ContinuityLineProtected = MutedGold
    val Surface = CloudWhite
    val SurfaceSubtle = WarmMist
    val SurfaceAccent = SoftSand
    val OnSurface = CharcoalText
    val OnPrimary = CloudWhite
    val Error = Color(0xFFB42318)
    val Outline = Color(0xFFCBD5E1)
}
