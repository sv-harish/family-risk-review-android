package com.familyriskreview.core.model.validation

import com.familyriskreview.core.model.CalculationSnapshot
import com.familyriskreview.core.model.HouseholdMember
import com.familyriskreview.core.model.Responsibility
import com.familyriskreview.core.model.Review
import com.familyriskreview.core.model.ReviewStep
import com.familyriskreview.core.model.lifecycle.ReviewModePolicy
import com.familyriskreview.core.model.result.ValidationResult

/**
 * Step-transition gates. Advancement must not persist a new step when invalid.
 */
object ProgressionGates {
    fun validateLeaving(
        step: ReviewStep,
        review: Review,
        members: List<HouseholdMember>,
        responsibilities: List<Responsibility>,
        latestSnapshot: CalculationSnapshot?,
        authoritativeCalculationVersion: String,
    ): ValidationResult = when (step) {
        ReviewStep.HOUSEHOLD_SUPPORT_MAP ->
            HouseholdRules.validateHousehold(members, review.focusedIncomeContributorId)
        ReviewStep.RESPONSIBILITIES ->
            validateLeavingResponsibilities(responsibilities)
        ReviewStep.PRIORITISATION ->
            ResponsibilityRules.validatePrioritisation(responsibilities, review.mode)
        ReviewStep.RESPONSIBILITY_DETAILS ->
            validateLeavingDetails(responsibilities, review)
        ReviewStep.TIMELINE,
        ReviewStep.INCOME_RISK_EDUCATION,
        ReviewStep.KEY_REALIZATION,
        ->
            validateInternalConsistency(review, members, responsibilities)
        ReviewStep.GROSS_RESPONSIBILITY ->
            validateFreshSummary(review, latestSnapshot, authoritativeCalculationVersion)
        ReviewStep.EDUCATIONAL_COMPARISON ->
            validateInternalConsistency(review, members, responsibilities)
        ReviewStep.AWARENESS_SUMMARY ->
            validateFreshSummary(review, latestSnapshot, authoritativeCalculationVersion)
        ReviewStep.ADVISOR_HANDOFF -> ValidationResult.Ok
    }

    private fun validateLeavingResponsibilities(
        responsibilities: List<Responsibility>,
    ): ValidationResult {
        var result = ValidationResult.Ok
        val selected = responsibilities.filter { it.isSelected }
        if (selected.isEmpty()) {
            result +=
                ValidationResult.error(
                    code = "RESP_NONE_SELECTED",
                    message = "Select at least one responsibility before prioritisation",
                )
        }
        selected.forEach { result += ResponsibilityRules.validateSelection(it) }
        return result
    }

    private fun validateLeavingDetails(
        responsibilities: List<Responsibility>,
        review: Review,
    ): ValidationResult {
        var result = ResponsibilityRules.validatePrioritisation(responsibilities, review.mode)
        responsibilities.filter { it.isSelected }.forEach { item ->
            result += ResponsibilityRules.validateDetails(item, review.mode)
        }
        return result
    }

    private fun validateInternalConsistency(
        review: Review,
        members: List<HouseholdMember>,
        responsibilities: List<Responsibility>,
    ): ValidationResult {
        var result = HouseholdRules.validateHousehold(members, review.focusedIncomeContributorId)
        result += ResponsibilityRules.validateForCalculation(responsibilities, review.mode)
        val policy = ReviewModePolicy.forMode(review.mode)
        if (!policy.allowMultipleScenarios) {
            // No extra check — scenarios are supplied at calculate time.
        }
        return result
    }

    fun validateFreshSummary(
        review: Review,
        latestSnapshot: CalculationSnapshot?,
        authoritativeCalculationVersion: String,
    ): ValidationResult {
        if (review.summaryStale) {
            return ValidationResult.error(
                code = "SUMMARY_STALE",
                message = "Summary is stale; recalculate before continuing",
            )
        }
        if (latestSnapshot == null) {
            return ValidationResult.error(
                code = "SUMMARY_MISSING",
                message = "A calculation snapshot is required",
            )
        }
        if (latestSnapshot.calculationInputRevision != review.calculationInputRevision) {
            return ValidationResult.error(
                code = "SUMMARY_INPUT_MISMATCH",
                message = "Snapshot does not match current calculation inputs",
            )
        }
        if (latestSnapshot.assumptionVersion != review.assumptionVersion) {
            return ValidationResult.error(
                code = "SUMMARY_ASSUMPTION_MISMATCH",
                message = "Snapshot assumption version does not match the review",
            )
        }
        if (latestSnapshot.calculationVersion != authoritativeCalculationVersion ||
            latestSnapshot.calculationVersion != review.calculationVersion
        ) {
            return ValidationResult.error(
                code = "SUMMARY_CALC_VERSION_MISMATCH",
                message = "Snapshot calculation version is not current",
            )
        }
        return ValidationResult.Ok
    }
}
