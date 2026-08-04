package com.familyriskreview.core.designsystem.icon

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.ChildCare
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.HomeWork
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.ui.graphics.vector.ImageVector
import com.familyriskreview.core.model.ResponsibilityCatalogue

/**
 * Consistent symbolic icon family for responsibility catalogue items.
 */
object ResponsibilityIcons {
    fun forCatalogue(catalogue: ResponsibilityCatalogue): ImageVector =
        when (catalogue) {
            ResponsibilityCatalogue.ESSENTIAL_FAMILY_LIVING_EXPENSES -> Icons.Outlined.Home
            ResponsibilityCatalogue.CHILD_HIGHER_EDUCATION -> Icons.Outlined.School
            ResponsibilityCatalogue.CHILD_MARRIAGE_SUPPORT -> Icons.Outlined.FavoriteBorder
            ResponsibilityCatalogue.SPOUSE_OR_PARTNER_SUPPORT -> Icons.Outlined.People
            ResponsibilityCatalogue.PARENT_SUPPORT -> Icons.Outlined.VolunteerActivism
            ResponsibilityCatalogue.HOME_LOAN_REPAYMENT,
            ResponsibilityCatalogue.OTHER_OUTSTANDING_LOANS,
            -> Icons.Outlined.AccountBalance
            ResponsibilityCatalogue.BUYING_OR_COMPLETING_HOUSE -> Icons.Outlined.HomeWork
            ResponsibilityCatalogue.SPECIAL_NEEDS_DEPENDANT_SUPPORT -> Icons.Outlined.VolunteerActivism
            ResponsibilityCatalogue.CHILDCARE_REPLACEMENT,
            ResponsibilityCatalogue.HOUSEHOLD_CARE_REPLACEMENT,
            -> Icons.Outlined.ChildCare
            ResponsibilityCatalogue.OTHER -> Icons.Outlined.MoreHoriz
        }
}
