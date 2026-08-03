package com.familyriskreview.core.model.validation

import com.familyriskreview.core.model.ContributionStatus
import com.familyriskreview.core.model.DependencyStatus
import com.familyriskreview.core.model.FamilyMemberType
import com.familyriskreview.core.model.HouseholdMember
import com.familyriskreview.core.model.result.ValidationResult

object HouseholdRules {
    const val MIN_AGE: Int = 0
    const val MAX_AGE: Int = 120

    fun validateMember(
        member: HouseholdMember,
        existing: List<HouseholdMember>,
        focusedIncomeContributorId: String? = null,
    ): ValidationResult {
        var result = ValidationResult.Ok

        member.age?.let { age ->
            if (age !in MIN_AGE..MAX_AGE) {
                result += ValidationResult.error(
                    code = "HOUSEHOLD_AGE_RANGE",
                    message = "Age must be between $MIN_AGE and $MAX_AGE",
                    field = "age",
                )
            }
        }

        if (member.relationship == FamilyMemberType.SELF) {
            val otherSelf = existing.any { it.id != member.id && it.relationship == FamilyMemberType.SELF }
            if (otherSelf) {
                result += ValidationResult.error(
                    code = "HOUSEHOLD_DUPLICATE_SELF",
                    message = "A review cannot contain more than one Self member",
                    field = "relationship",
                )
            }
        }

        if (member.contributionStatus == ContributionStatus.PRIMARY_INCOME &&
            member.dependencyStatus == DependencyStatus.FULLY_DEPENDENT
        ) {
            result += ValidationResult.warning(
                code = "HOUSEHOLD_PRIMARY_FULLY_DEPENDENT",
                message = "A fully dependent member is unusual as a primary income contributor",
                field = "contributionStatus",
            )
        }

        if (focusedIncomeContributorId == member.id &&
            member.contributionStatus == ContributionStatus.NON_CONTRIBUTOR
        ) {
            result += ValidationResult.error(
                code = "HOUSEHOLD_FOCUS_NON_CONTRIBUTOR",
                message = "Focused income contributor cannot be a non-contributor",
                field = "contributionStatus",
            )
        }

        return result
    }

    fun validateHousehold(
        members: List<HouseholdMember>,
        focusedIncomeContributorId: String?,
    ): ValidationResult {
        var result = ValidationResult.Ok
        if (members.isEmpty()) {
            result += ValidationResult.error(
                code = "HOUSEHOLD_EMPTY",
                message = "At least one household member is required",
            )
        }

        val selfCount = members.count { it.relationship == FamilyMemberType.SELF }
        if (selfCount > 1) {
            result += ValidationResult.error(
                code = "HOUSEHOLD_DUPLICATE_SELF",
                message = "A review cannot contain more than one Self member",
            )
        }

        focusedIncomeContributorId?.let { focusId ->
            val focus = members.find { it.id == focusId }
            if (focus == null) {
                result += ValidationResult.error(
                    code = "HOUSEHOLD_FOCUS_MISSING",
                    message = "Focused income contributor must exist in this review",
                    field = "focusedIncomeContributorId",
                )
            } else if (focus.contributionStatus == ContributionStatus.NON_CONTRIBUTOR) {
                result += ValidationResult.error(
                    code = "HOUSEHOLD_FOCUS_NON_CONTRIBUTOR",
                    message = "Focused income contributor cannot be a non-contributor",
                    field = "focusedIncomeContributorId",
                )
            }
        }

        members.forEach { member ->
            result += validateMember(member, members, focusedIncomeContributorId)
        }
        return result
    }

    fun isIncomeContributor(status: ContributionStatus): Boolean = status == ContributionStatus.PRIMARY_INCOME ||
        status == ContributionStatus.SHARED_INCOME ||
        status == ContributionStatus.SUPPLEMENTARY_OR_IRREGULAR
}
