package com.familyriskreview.core.model.suggestion

import com.familyriskreview.core.model.FamilyMemberType
import com.familyriskreview.core.model.HouseholdMember
import com.familyriskreview.core.model.ResponsibilityCatalogue

/**
 * Conditional family suggestion logic.
 *
 * Suggestions are advisory only — they must never auto-select responsibilities.
 */
object ResponsibilitySuggestionEngine {
    data class Suggestion(
        val catalogue: ResponsibilityCatalogue,
        val reasonCode: String,
        val reason: String,
    )

    fun suggest(members: List<HouseholdMember>): List<Suggestion> {
        val suggestions = linkedMapOf<ResponsibilityCatalogue, Suggestion>()

        fun add(
            catalogue: ResponsibilityCatalogue,
            reasonCode: String,
            reason: String,
        ) {
            suggestions.putIfAbsent(
                catalogue,
                Suggestion(catalogue, reasonCode, reason),
            )
        }

        add(
            ResponsibilityCatalogue.ESSENTIAL_FAMILY_LIVING_EXPENSES,
            "ALWAYS_LIVING",
            "Essential living expenses usually continue for the household",
        )

        val types = members.map { it.relationship }.toSet()

        if (FamilyMemberType.CHILD in types) {
            add(
                ResponsibilityCatalogue.CHILD_HIGHER_EDUCATION,
                "HAS_CHILD",
                "Household includes a child — education support may apply",
            )
            add(
                ResponsibilityCatalogue.CHILD_MARRIAGE_SUPPORT,
                "HAS_CHILD",
                "Household includes a child — marriage support may apply",
            )
            add(
                ResponsibilityCatalogue.CHILDCARE_REPLACEMENT,
                "HAS_CHILD",
                "Household includes a child — childcare replacement may apply",
            )
        }

        if (FamilyMemberType.SPOUSE_OR_PARTNER in types) {
            add(
                ResponsibilityCatalogue.SPOUSE_OR_PARTNER_SUPPORT,
                "HAS_SPOUSE",
                "Household includes a spouse/partner — support may apply",
            )
            add(
                ResponsibilityCatalogue.HOUSEHOLD_CARE_REPLACEMENT,
                "HAS_SPOUSE",
                "Household care replacement may apply with a spouse/partner",
            )
        }

        if (FamilyMemberType.PARENT in types) {
            add(
                ResponsibilityCatalogue.PARENT_SUPPORT,
                "HAS_PARENT",
                "Household includes a parent — parent support may apply",
            )
        }

        if (FamilyMemberType.OTHER_DEPENDANT in types) {
            add(
                ResponsibilityCatalogue.SPECIAL_NEEDS_DEPENDANT_SUPPORT,
                "HAS_OTHER_DEPENDANT",
                "Household includes another dependant — special-needs support may apply",
            )
        }

        // Optional continuity items — suggested lightly, never auto-selected.
        add(
            ResponsibilityCatalogue.HOME_LOAN_REPAYMENT,
            "OPTIONAL_LOAN",
            "Consider outstanding home-loan continuity if relevant",
        )
        add(
            ResponsibilityCatalogue.OTHER_OUTSTANDING_LOANS,
            "OPTIONAL_LOAN",
            "Consider other outstanding loans if relevant",
        )
        add(
            ResponsibilityCatalogue.BUYING_OR_COMPLETING_HOUSE,
            "OPTIONAL_HOUSE",
            "Consider house purchase/completion plans if relevant",
        )

        return suggestions.values.toList()
    }
}
