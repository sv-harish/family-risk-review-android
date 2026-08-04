package com.familyriskreview.core.model.validation

import com.familyriskreview.core.model.DomainLimits
import com.familyriskreview.core.model.QuantificationStatus
import com.familyriskreview.core.model.Responsibility
import com.familyriskreview.core.model.ResponsibilityAmountModel
import com.familyriskreview.core.model.ResponsibilityCatalogue
import com.familyriskreview.core.model.ResponsibilityPriority
import com.familyriskreview.core.model.ResponsibilityTiming
import com.familyriskreview.core.model.ReviewMode
import com.familyriskreview.core.model.TimingKind
import com.familyriskreview.core.model.lifecycle.ReviewModePolicy
import com.familyriskreview.core.model.result.ValidationResult

object ResponsibilityRules {
    fun validateSelection(responsibility: Responsibility): ValidationResult {
        var result = ValidationResult.Ok
        if (responsibility.catalogue == ResponsibilityCatalogue.OTHER &&
            responsibility.customLabel.isNullOrBlank()
        ) {
            result +=
                ValidationResult.error(
                    code = "RESP_CUSTOM_LABEL_REQUIRED",
                    message = "Custom responsibilities require a label",
                    field = "customLabel",
                )
        }
        return result
    }

    fun validatePrioritisation(
        responsibilities: List<Responsibility>,
        mode: ReviewMode,
    ): ValidationResult {
        val policy = ReviewModePolicy.forMode(mode)
        var result = ValidationResult.Ok
        val selected = responsibilities.filter { it.isSelected }
        if (selected.isEmpty()) {
            result +=
                ValidationResult.error(
                    code = "RESP_NONE_SELECTED",
                    message = "At least one responsibility must be selected before continuing",
                )
        }
        selected.forEach { item ->
            if (item.priority == null) {
                result +=
                    ValidationResult.error(
                        code = "RESP_PRIORITY_REQUIRED",
                        message = "Selected responsibilities require a priority",
                        field = "priority",
                    )
            }
            result += validateSelection(item)
        }
        val mustContinue =
            selected.count { it.priority == ResponsibilityPriority.MUST_CONTINUE }
        policy.maxMustContinue?.let { max ->
            if (mustContinue > max) {
                result +=
                    ValidationResult.error(
                        code = "RESP_QUICK_MUST_CONTINUE_LIMIT",
                        message = "This review mode allows at most $max must-continue responsibilities",
                        field = "priority",
                    )
            }
        }
        return result
    }

    /**
     * Detail completeness for progression and calculation.
     *
     * Distinguishes deliberate non-quantified lightweight Quick items from
     * must-continue / Guided items that must be quantified and calculable.
     * For mid-edit draft persistence use [validateDraftDetails] instead.
     */
    fun validateDetails(
        responsibility: Responsibility,
        mode: ReviewMode,
    ): ValidationResult {
        var result = validateSelection(responsibility)
        if (!responsibility.isSelected) return result

        val policy = ReviewModePolicy.forMode(mode)
        val needsFullDetails = requiresFullDetails(responsibility, policy)

        if (responsibility.isNonQuantified()) {
            if (!mayRemainNonQuantified(mode, responsibility.priority)) {
                val code =
                    if (mode == ReviewMode.GUIDED) {
                        "RESP_GUIDED_REQUIRES_QUANTIFICATION"
                    } else {
                        "RESP_MUST_CONTINUE_REQUIRES_QUANTIFICATION"
                    }
                result +=
                    ValidationResult.error(
                        code = code,
                        message =
                        when (mode) {
                            ReviewMode.GUIDED ->
                                "Guided Review requires all selected responsibilities to be quantified"
                            ReviewMode.QUICK ->
                                "Quick Review must-continue responsibilities must be quantified and calculable"
                        },
                        field = "quantificationStatus",
                    )
            }
            return result
        }

        if (!needsFullDetails) {
            // Quick lightweight item marked QUANTIFIED → must either have full inputs
            // or be NOT_YET_QUANTIFIED.
            val completeness = validateQuantifiedCompleteness(responsibility)
            if (!completeness.isValid) {
                result +=
                    ValidationResult.error(
                        code = "RESP_LIGHTWEIGHT_MUST_BE_NON_QUANTIFIED",
                        message =
                        "Quick Review lightweight items without complete calculation " +
                            "inputs must be marked NOT_YET_QUANTIFIED",
                        field = "quantificationStatus",
                    )
                result += completeness
            }
            return result
        }

        // Must-continue / Guided full-detail path.
        result += validateQuantifiedCompleteness(responsibility)
        return result
    }

    /**
     * Draft-save validation: selection integrity only.
     * Incomplete / non-quantified drafts may be persisted while the advisor is editing;
     * [validateDetails] / [validateForCalculation] enforce readiness to advance or calculate.
     */
    fun validateDraftDetails(
        responsibility: Responsibility,
        mode: ReviewMode,
    ): ValidationResult {
        if (!responsibility.isSelected) return ValidationResult.Ok
        return validateSelection(responsibility)
    }

    /**
     * Whether [QuantificationStatus.NOT_YET_QUANTIFIED] is permitted for progression/calculation.
     * Quick lightweight priorities may remain non-quantified; Guided never may.
     */
    fun mayRemainNonQuantified(
        mode: ReviewMode,
        priority: ResponsibilityPriority?,
    ): Boolean = when (mode) {
        ReviewMode.GUIDED -> false
        ReviewMode.QUICK ->
            when (priority) {
                ResponsibilityPriority.IMPORTANT_BUT_ADJUSTABLE,
                ResponsibilityPriority.CAN_BE_POSTPONED_OR_REDUCED,
                -> true
                ResponsibilityPriority.MUST_CONTINUE,
                null,
                -> false
            }
    }

    fun validateForCalculation(
        responsibilities: List<Responsibility>,
        mode: ReviewMode,
    ): ValidationResult {
        var result = validatePrioritisation(responsibilities, mode)
        responsibilities.filter { it.isSelected }.forEach { item ->
            result += validateDetails(item, mode)
        }
        return result
    }

    fun requiresFullDetails(
        responsibility: Responsibility,
        policy: ReviewModePolicy,
    ): Boolean {
        if (!responsibility.isSelected) return false
        return when (responsibility.priority) {
            ResponsibilityPriority.MUST_CONTINUE -> true
            ResponsibilityPriority.IMPORTANT_BUT_ADJUSTABLE -> policy.requireDetailsForAdjustable
            ResponsibilityPriority.CAN_BE_POSTPONED_OR_REDUCED -> policy.requireDetailsForPostponed
            null -> true
        }
    }

    fun resolveCalculableDurationYears(timing: ResponsibilityTiming): Int? = when (timing.kind) {
        TimingKind.RECURRING_DURATION -> timing.durationYears?.takeIf { it > 0 }
        TimingKind.AS_LONG_AS_REQUIRED -> timing.modellingDurationYears?.takeIf { it > 0 }
        TimingKind.UNTIL_MILESTONE ->
            timing.yearsUntilRequired?.takeIf { it >= 0 }
                ?: timing.modellingDurationYears?.takeIf { it > 0 }
        TimingKind.CUSTOM ->
            timing.durationYears?.takeIf { it > 0 }
                ?: timing.modellingDurationYears?.takeIf { it > 0 }
                ?: timing.yearsUntilRequired?.takeIf { it >= 0 }
        TimingKind.ONE_TIME_IN_YEARS -> timing.yearsUntilRequired?.takeIf { it >= 0 }
        TimingKind.CURRENT_OUTSTANDING -> 0
    }

    private fun validateQuantifiedCompleteness(responsibility: Responsibility): ValidationResult {
        var result = ValidationResult.Ok
        result +=
            CatalogueTimingRules.validate(
                catalogue = responsibility.catalogue,
                timingKind = responsibility.timing?.kind,
                amountModel = responsibility.amountModel,
            )
        if (!result.isValid) return result

        result += validateTimingForCalculation(responsibility)
        result += validateCatalogueAmounts(responsibility)
        result += validateDomainLimits(responsibility)
        if (responsibility.catalogue == ResponsibilityCatalogue.OTHER &&
            responsibility.explicitInflationBps == null
        ) {
            result +=
                ValidationResult.error(
                    code = "RESP_EXPLICIT_INFLATION_REQUIRED",
                    message = "Custom responsibilities require an explicit inflation rate",
                    field = "explicitInflationBps",
                )
        }
        return result
    }

    private fun validateTimingForCalculation(responsibility: Responsibility): ValidationResult {
        var result = ValidationResult.Ok
        val timing = responsibility.timing
        if (timing == null) {
            return ValidationResult.error(
                code = "RESP_TIMING_REQUIRED",
                message = "Selected responsibilities require timing",
                field = "timing",
            )
        }
        when (timing.kind) {
            TimingKind.ONE_TIME_IN_YEARS -> {
                if (timing.yearsUntilRequired == null || timing.yearsUntilRequired < 0) {
                    result +=
                        ValidationResult.error(
                            code = "RESP_YEARS_REQUIRED",
                            message = "Years until required must be provided",
                            field = "yearsUntilRequired",
                        )
                }
            }
            TimingKind.RECURRING_DURATION -> {
                if (timing.durationYears == null || timing.durationYears <= 0) {
                    result +=
                        ValidationResult.error(
                            code = "RESP_DURATION_REQUIRED",
                            message = "Duration years must be a positive value",
                            field = "durationYears",
                        )
                }
            }
            TimingKind.CURRENT_OUTSTANDING -> Unit
            TimingKind.AS_LONG_AS_REQUIRED -> {
                if (timing.modellingDurationYears == null || timing.modellingDurationYears <= 0) {
                    result +=
                        ValidationResult.error(
                            code = "RESP_MODELLING_HORIZON_REQUIRED",
                            message =
                            "“As long as required” needs an indicative modelling duration " +
                                "or must be NOT_YET_QUANTIFIED",
                            field = "modellingDurationYears",
                        )
                }
            }
            TimingKind.UNTIL_MILESTONE -> {
                if (timing.milestoneLabel.isNullOrBlank()) {
                    result +=
                        ValidationResult.error(
                            code = "RESP_MILESTONE_REQUIRED",
                            message = "Milestone label is required",
                            field = "milestoneLabel",
                        )
                }
                if (resolveCalculableDurationYears(timing) == null) {
                    result +=
                        ValidationResult.error(
                            code = "RESP_MILESTONE_HORIZON_REQUIRED",
                            message =
                            "Until-milestone requires a calculable horizon " +
                                "or must be NOT_YET_QUANTIFIED",
                            field = "yearsUntilRequired",
                        )
                }
            }
            TimingKind.CUSTOM -> {
                if (timing.customNote.isNullOrBlank()) {
                    result +=
                        ValidationResult.error(
                            code = "RESP_CUSTOM_TIMING_REQUIRED",
                            message = "Custom timing requires a note",
                            field = "customNote",
                        )
                }
                if (resolveCalculableDurationYears(timing) == null) {
                    result +=
                        ValidationResult.error(
                            code = "RESP_CUSTOM_HORIZON_REQUIRED",
                            message =
                            "Custom timing requires a calculable horizon " +
                                "or must be NOT_YET_QUANTIFIED",
                            field = "modellingDurationYears",
                        )
                }
            }
        }
        return result
    }

    private fun validateCatalogueAmounts(responsibility: Responsibility): ValidationResult {
        val current = responsibility.currentAmount?.amountRupees ?: 0L
        val monthly = responsibility.monthlyAmount?.amountRupees ?: 0L
        var result = ValidationResult.Ok

        when (responsibility.catalogue) {
            ResponsibilityCatalogue.ESSENTIAL_FAMILY_LIVING_EXPENSES,
            ResponsibilityCatalogue.PARENT_SUPPORT,
            ResponsibilityCatalogue.SPOUSE_OR_PARTNER_SUPPORT,
            ResponsibilityCatalogue.SPECIAL_NEEDS_DEPENDANT_SUPPORT,
            ResponsibilityCatalogue.CHILDCARE_REPLACEMENT,
            ResponsibilityCatalogue.HOUSEHOLD_CARE_REPLACEMENT,
            -> {
                if (monthly <= 0L) {
                    result +=
                        ValidationResult.error(
                            code = "RESP_MONTHLY_REQUIRED",
                            message = "A monthly amount is required for this responsibility",
                            field = "monthlyAmount",
                        )
                }
                if (current > 0L) {
                    result +=
                        ValidationResult.error(
                            code = "RESP_CURRENT_NOT_PRIMARY",
                            message = "Use monthly amount as the primary input for this item",
                            field = "currentAmount",
                        )
                }
            }
            ResponsibilityCatalogue.CHILD_HIGHER_EDUCATION,
            ResponsibilityCatalogue.CHILD_MARRIAGE_SUPPORT,
            ResponsibilityCatalogue.BUYING_OR_COMPLETING_HOUSE,
            -> {
                if (current <= 0L) {
                    result +=
                        ValidationResult.error(
                            code = "RESP_CURRENT_REQUIRED",
                            message = "A current one-time amount is required",
                            field = "currentAmount",
                        )
                }
                if (monthly > 0L) {
                    result +=
                        ValidationResult.error(
                            code = "RESP_MONTHLY_NOT_ALLOWED",
                            message = "Monthly input is not accepted for this catalogue item",
                            field = "monthlyAmount",
                        )
                }
            }
            ResponsibilityCatalogue.HOME_LOAN_REPAYMENT,
            ResponsibilityCatalogue.OTHER_OUTSTANDING_LOANS,
            -> {
                if (current <= 0L) {
                    result +=
                        ValidationResult.error(
                            code = "RESP_CURRENT_REQUIRED",
                            message = "Outstanding loan amount is required",
                            field = "currentAmount",
                        )
                }
                if (monthly > 0L) {
                    result +=
                        ValidationResult.error(
                            code = "RESP_LOAN_MONTHLY_NOT_ALLOWED",
                            message = "Loans use outstanding amount, not monthly support",
                            field = "monthlyAmount",
                        )
                }
            }
            ResponsibilityCatalogue.OTHER -> {
                when (responsibility.amountModel) {
                    ResponsibilityAmountModel.ONE_TIME -> {
                        if (current <= 0L) {
                            result +=
                                ValidationResult.error(
                                    code = "RESP_CURRENT_REQUIRED",
                                    message = "One-time custom items require a current amount",
                                    field = "currentAmount",
                                )
                        }
                        if (monthly > 0L) {
                            result +=
                                ValidationResult.error(
                                    code = "RESP_AMBIGUOUS_AMOUNT_MODEL",
                                    message = "ONE_TIME amount model must not include a monthly amount",
                                    field = "monthlyAmount",
                                )
                        }
                    }
                    ResponsibilityAmountModel.RECURRING -> {
                        if (monthly <= 0L) {
                            result +=
                                ValidationResult.error(
                                    code = "RESP_MONTHLY_REQUIRED",
                                    message = "Recurring custom items require a monthly amount",
                                    field = "monthlyAmount",
                                )
                        }
                        if (current > 0L) {
                            result +=
                                ValidationResult.error(
                                    code = "RESP_AMBIGUOUS_AMOUNT_MODEL",
                                    message = "RECURRING amount model must not include a current amount",
                                    field = "currentAmount",
                                )
                        }
                    }
                    null ->
                        result +=
                            ValidationResult.error(
                                code = "RESP_AMOUNT_MODEL_REQUIRED",
                                message = "Custom responsibilities require an explicit amount model",
                                field = "amountModel",
                            )
                }
            }
        }
        return result
    }

    private fun validateDomainLimits(responsibility: Responsibility): ValidationResult {
        var result = ValidationResult.Ok
        responsibility.currentAmount?.amountRupees?.let { amount ->
            if (amount > DomainLimits.MAX_ONE_TIME_RUPEES) {
                result +=
                    ValidationResult.error(
                        code = "RESP_AMOUNT_LIMIT",
                        message =
                        "One-time amount exceeds supported maximum " +
                            "(${DomainLimits.MAX_ONE_TIME_RUPEES})",
                        field = "currentAmount",
                    )
            }
        }
        responsibility.monthlyAmount?.amountRupees?.let { amount ->
            if (amount > DomainLimits.MAX_MONTHLY_RUPEES) {
                result +=
                    ValidationResult.error(
                        code = "RESP_MONTHLY_LIMIT",
                        message =
                        "Monthly amount exceeds supported maximum " +
                            "(${DomainLimits.MAX_MONTHLY_RUPEES})",
                        field = "monthlyAmount",
                    )
            }
        }
        val timing = responsibility.timing ?: return result
        timing.yearsUntilRequired?.let { years ->
            if (years !in DomainLimits.MIN_YEARS..DomainLimits.MAX_YEARS_UNTIL_REQUIRED) {
                result +=
                    ValidationResult.error(
                        code = "RESP_YEARS_LIMIT",
                        message =
                        "Years until required must be between " +
                            "${DomainLimits.MIN_YEARS} and ${DomainLimits.MAX_YEARS_UNTIL_REQUIRED}",
                        field = "yearsUntilRequired",
                    )
            }
        }
        timing.durationYears?.let { years ->
            if (years !in 1..DomainLimits.MAX_DURATION_YEARS) {
                result +=
                    ValidationResult.error(
                        code = "RESP_DURATION_LIMIT",
                        message =
                        "Duration years must be between 1 and ${DomainLimits.MAX_DURATION_YEARS}",
                        field = "durationYears",
                    )
            }
        }
        timing.modellingDurationYears?.let { years ->
            if (years !in 1..DomainLimits.MAX_MODELLING_HORIZON_YEARS) {
                result +=
                    ValidationResult.error(
                        code = "RESP_HORIZON_LIMIT",
                        message =
                        "Modelling horizon must be between 1 and " +
                            "${DomainLimits.MAX_MODELLING_HORIZON_YEARS}",
                        field = "modellingDurationYears",
                    )
            }
        }
        return result
    }
}
