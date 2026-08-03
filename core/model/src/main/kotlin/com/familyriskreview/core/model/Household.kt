package com.familyriskreview.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class FamilyMemberType {
    SELF,
    SPOUSE_OR_PARTNER,
    CHILD,
    PARENT,
    OTHER_DEPENDANT,
}

@Serializable
enum class ContributionStatus {
    PRIMARY_INCOME,
    SHARED_INCOME,
    SUPPLEMENTARY_OR_IRREGULAR,
    ESSENTIAL_UNPAID_CAREGIVER,
    NON_CONTRIBUTOR,
}

@Serializable
enum class DependencyStatus {
    FULLY_DEPENDENT,
    PARTLY_DEPENDENT,
    FINANCIALLY_INDEPENDENT,
    NOT_APPLICABLE,
}

/**
 * Anonymous household member captured during the Household Support Map.
 * Customer names are never required.
 */
@Serializable
data class HouseholdMember(
    val id: String,
    val reviewId: String,
    val relationship: FamilyMemberType,
    val age: Int?,
    val contributionStatus: ContributionStatus,
    val dependencyStatus: DependencyStatus,
    val displayLabel: String? = null,
    val sortOrder: Int = 0,
)
