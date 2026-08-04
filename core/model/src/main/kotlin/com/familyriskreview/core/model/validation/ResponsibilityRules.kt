package com.familyriskreview.core.model.validation

import com.familyriskreview.core.model.Responsibility
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
     * Detail completeness for a single responsibility, honouring mode policy.
     * Excluded (non-quantified) items skip amount/timing calculability checks.
     */
    fun validateDetails(
        responsibility: Responsibility,
        mode: ReviewMode,
    ): ValidationResult {
        var result = validateSelection(responsibility)
        if (!responsibility.isSelected) return result

        val policy = ReviewModePolicy.forMode(mode)
        if (!requiresFullDetails(responsibility, policy)) {
            return result
        }

        if (responsibility.excludedFromNumericCalculation) {
            result += validateNonQuantifiedMarker(responsibility)
            return result
        }

        result += validateTimingForCalculation(responsibility)
        result += validateCatalogueAmounts(responsibility)
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

    private fun validateNonQuantifiedMarker(responsibility: Responsibility): ValidationResult {
        var result = ValidationResult.Ok
        if (responsibility.timing == null) {
            result +=
                ValidationResult.error(
                    code = "RESP_TIMING_REQUIRED",
                    message = "Non-quantified responsibilities still require a timing kind",
                    field = "timing",
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
                                "(e.g. 10/15/20 years) or must be excluded from numeric totals",
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
                val horizon = resolveCalculableDurationYears(timing)
                if (horizon == null) {
                    result +=
                        ValidationResult.error(
                            code = "RESP_MILESTONE_HORIZON_REQUIRED",
                            message =
                            "Until-milestone requires years until milestone or a modelling " +
                                "horizon, or exclusion from numeric totals",
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
                            "Custom timing requires a calculable horizon or exclusion " +
                                "from numeric totals",
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

        fun requireMonthly(code: String = "RESP_MONTHLY_REQUIRED") {
            if (monthly <= 0L) {
                result +=
                    ValidationResult.error(
                        code = code,
                        message = "A monthly amount is required for this responsibility",
                        field = "monthlyAmount",
                    )
            }
        }

        fun requireCurrent(code: String = "RESP_CURRENT_REQUIRED") {
            if (current <= 0L) {
                result +=
                    ValidationResult.error(
                        code = code,
                        message = "A current one-time amount is required for this responsibility",
                        field = "currentAmount",
                    )
            }
        }

        fun rejectAmbiguousBoth() {
            if (current > 0L && monthly > 0L) {
                result +=
                    ValidationResult.error(
                        code = "RESP_AMBIGUOUS_AMOUNT_MODEL",
                        message =
                        "Provide either a current or monthly amount for this catalogue item, " +
                            "not both",
                        field = "amount",
                    )
            }
        }

        when (responsibility.catalogue) {
            ResponsibilityCatalogue.ESSENTIAL_FAMILY_LIVING_EXPENSES,
            ResponsibilityCatalogue.PARENT_SUPPORT,
            ResponsibilityCatalogue.SPOUSE_OR_PARTNER_SUPPORT,
            ResponsibilityCatalogue.SPECIAL_NEEDS_DEPENDANT_SUPPORT,
            ResponsibilityCatalogue.CHILDCARE_REPLACEMENT,
            ResponsibilityCatalogue.HOUSEHOLD_CARE_REPLACEMENT,
            -> {
                requireMonthly()
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
                requireCurrent()
                if (monthly > 0L) {
                    result +=
                        ValidationResult.error(
                            code = "RESP_MONTHLY_NOT_ALLOWED",
                            message = "Monthly-only input is not accepted for this catalogue item",
                            field = "monthlyAmount",
                        )
                }
            }
            ResponsibilityCatalogue.HOME_LOAN_REPAYMENT,
            ResponsibilityCatalogue.OTHER_OUTSTANDING_LOANS,
            -> {
                requireCurrent()
                if (monthly > 0L) {
                    result +=
                        ValidationResult.error(
                            code = "RESP_LOAN_MONTHLY_NOT_ALLOWED",
                            message = "Loans use outstanding amount, not monthly support calculations",
                            field = "monthlyAmount",
                        )
                }
                val kind = responsibility.timing?.kind
                if (kind != null && kind != TimingKind.CURRENT_OUTSTANDING) {
                    result +=
                        ValidationResult.error(
                            code = "RESP_LOAN_TIMING",
                            message = "Loans require current-outstanding timing",
                            field = "timing",
                        )
                }
            }
            ResponsibilityCatalogue.OTHER -> {
                if (current <= 0L && monthly <= 0L) {
                    result +=
                        ValidationResult.error(
                            code = "RESP_AMOUNT_REQUIRED",
                            message = "Custom responsibilities require an explicit amount",
                            field = "amount",
                        )
                }
                rejectAmbiguousBoth()
            }
        }
        return result
    }
}
