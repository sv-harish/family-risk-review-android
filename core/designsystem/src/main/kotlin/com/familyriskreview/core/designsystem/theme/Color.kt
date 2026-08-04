package com.familyriskreview.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Family Risk Review colour tokens — calm clinical-meets-human system.
 * Prefer [FrrSemanticColors] in feature UI; do not hard-code raw values in screens.
 */
object FrrColors {
    val Ink = Color(0xFF122033)
    val DeepInk = Color(0xFF1B2D42)
    val Teal = Color(0xFF1F8A9A)
    val TealSoft = Color(0xFF4FB3C1)
    val Amber = Color(0xFFC9893A)
    val AmberSoft = Color(0xFFE8C48A)
    val WarmOffWhite = Color(0xFFF7F4EF)
    val SoftNeutral = Color(0xFFEEF1F4)
    val Elevated = Color(0xFFFFFFFF)
    val MutedText = Color(0xFF5A6A7A)
    val Outline = Color(0xFFD0D7DE)
    val Success = Color(0xFF2F7D57)
    val Caution = Color(0xFFB7791F)
    val BlockingError = Color(0xFFB42318)

    val MustContinue = Color(0xFF1F5C6B)
    val Adjustable = Color(0xFF3D7A8C)
    val Postponed = Color(0xFF7A8A96)
    val Contributor = Color(0xFF1F8A9A)
    val Dependant = Color(0xFFC9893A)

    val CategoryLiving = Color(0xFF3D7A8C)
    val CategoryEducation = Color(0xFF4A6FA5)
    val CategoryMarriage = Color(0xFF8B6B8F)
    val CategoryLoan = Color(0xFF6B7280)
    val CategoryParent = Color(0xFF5B8A6E)
    val CategoryCare = Color(0xFFB07A5A)
    val CategoryHouse = Color(0xFF7A6B5A)
    val CategorySpecial = Color(0xFF5A7A8F)
    val CategoryOther = Color(0xFF6A7A6A)

    // Backward-compatible aliases used by existing Phase 0 screens.
    val MidnightBlue = Ink
    val SlateNavy = DeepInk
    val TealBlue = Teal
    val SoftAqua = TealSoft
    val MutedGold = Amber
    val SoftSand = AmberSoft.copy(alpha = 0.35f)
    val CloudWhite = WarmOffWhite
    val WarmMist = SoftNeutral
    val CharcoalText = Ink
    val ContinuityLine = Teal
    val ContinuityLineFaded = TealSoft.copy(alpha = 0.45f)
    val ContinuityLineProtected = Amber
    val Surface = WarmOffWhite
    val SurfaceSubtle = SoftNeutral
    val SurfaceAccent = AmberSoft.copy(alpha = 0.28f)
    val OnSurface = Ink
    val OnPrimary = Elevated
    val Error = BlockingError
}

@Immutable
data class FrrSemanticColors(
    val surface: Color = FrrColors.WarmOffWhite,
    val elevatedSurface: Color = FrrColors.Elevated,
    val primaryAction: Color = FrrColors.Teal,
    val secondaryAction: Color = FrrColors.DeepInk,
    val success: Color = FrrColors.Success,
    val caution: Color = FrrColors.Caution,
    val blockingError: Color = FrrColors.BlockingError,
    val mustContinue: Color = FrrColors.MustContinue,
    val adjustable: Color = FrrColors.Adjustable,
    val postponed: Color = FrrColors.Postponed,
    val contributor: Color = FrrColors.Contributor,
    val dependant: Color = FrrColors.Dependant,
    val onSurface: Color = FrrColors.Ink,
    val mutedText: Color = FrrColors.MutedText,
    val outline: Color = FrrColors.Outline,
)

val LocalFrrSemanticColors = staticCompositionLocalOf { FrrSemanticColors() }

object FrrSpacing {
    val xxs: Dp = 4.dp
    val xs: Dp = 8.dp
    val sm: Dp = 12.dp
    val md: Dp = 16.dp
    val lg: Dp = 24.dp
    val xl: Dp = 32.dp
    val xxl: Dp = 40.dp
    val xxxl: Dp = 56.dp
    val contentMaxWidth: Dp = 720.dp
    val journeyMaxWidth: Dp = 960.dp
}

object FrrRadius {
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 24.dp
}

object FrrElevation {
    val none: Dp = 0.dp
    val subtle: Dp = 1.dp
    val raised: Dp = 3.dp
}

object FrrIconSize {
    val sm: Dp = 20.dp
    val md: Dp = 24.dp
    val lg: Dp = 32.dp
    val xl: Dp = 48.dp
}

object FrrTouchTarget {
    val min: Dp = 48.dp
    val comfortable: Dp = 52.dp
}

object FrrMotion {
    const val shortMs: Int = 180
    const val mediumMs: Int = 280
    const val longMs: Int = 420
}
