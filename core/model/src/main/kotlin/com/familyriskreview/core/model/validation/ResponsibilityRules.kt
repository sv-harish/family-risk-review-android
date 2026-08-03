package com.familyriskreview.core.model.validation

import com.familyriskreview.core.model.Responsibility
import com.familyriskreview.core.model.ResponsibilityCatalogue
import com.familyriskreview.core.model.ResponsibilityPriority
import com.familyriskreview.core.model.ReviewMode
import com.familyriskreview.core.model.TimingKind
import com.familyriskreview.core.model.lifecycle.QuickReviewRules
import com.familyriskreview.core.model.result.ValidationResult

object ResponsibilityRules {
    fun validateSelection(responsibility: Responsibility): ValidationResult {
        var result = ValidationResult.Ok
        if (responsibility.catalogue == ResponsibilityCatalogue.OTHER) {
            if (responsibility.customLabel.isNullOrBlank()) {
                result += ValidationResult.error(
                    code = "RESP_CUSTOM_LABEL_REQUIRED",
                    message = "Custom responsibilities require a label",
                    field = "customLabel",
                )
            }
        }
        return result
    }

    fun validatePrioritisation(
        responsibilities: List<Responsibility>,
        mode: ReviewMode,
    ): ValidationResult {
        var result = ValidationResult.Ok
        val selected = responsibilities.filter { it.isSelected }
        selected.forEach { item ->
            if (item.priority == null) {
                result += ValidationResult.error(
                    code = "RESP_PRIORITY_REQUIRED",
                    message = "Selected responsibilities require a priority",
                    field = "priority",
                )
            }
            result += validateSelection(item)
        }

        val mustContinue =
            selected.count { it.priority == ResponsibilityPriority.MUST_CONTINUE }
        if (mode == ReviewMode.QUICK && mustContinue > QuickReviewRules.MAX_MUST_CONTINUE) {
            result += ValidationResult.error(
                code = "RESP_QUICK_MUST_CONTINUE_LIMIT",
                message =
                "Quick Review allows at most ${QuickReviewRules.MAX_MUST_CONTINUE} " +
                    "must-continue responsibilities",
                field = "priority",
            )
        }
        if (selected.isEmpty()) {
            result += ValidationResult.warning(
                code = "RESP_NONE_SELECTED",
                message = "No responsibilities are selected yet",
            )
        }
        return result
    }

    fun validateDetails(responsibility: Responsibility): ValidationResult {
        var result = validateSelection(responsibility)
        if (!responsibility.isSelected) return result

        val timing = responsibility.timing
        if (timing == null) {
            result += ValidationResult.error(
                code = "RESP_TIMING_REQUIRED",
                message = "Selected responsibilities require timing",
                field = "timing",
            )
        } else {
            when (timing.kind) {
                TimingKind.ONE_TIME_IN_YEARS -> {
                    if (timing.yearsUntilRequired == null || timing.yearsUntilRequired < 0) {
                        result += ValidationResult.error(
                            code = "RESP_YEARS_REQUIRED",
                            message = "Years until required must be provided",
                            field = "yearsUntilRequired",
                        )
                    }
                }
                TimingKind.RECURRING_DURATION -> {
                    if (timing.durationYears == null || timing.durationYears <= 0) {
                        result += ValidationResult.error(
                            code = "RESP_DURATION_REQUIRED",
                            message = "Duration years must be a positive value",
                            field = "durationYears",
                        )
                    }
                }
                TimingKind.UNTIL_MILESTONE -> {
                    if (timing.milestoneLabel.isNullOrBlank()) {
                        result += ValidationResult.error(
                            code = "RESP_MILESTONE_REQUIRED",
                            message = "Milestone label is required",
                            field = "milestoneLabel",
                        )
                    }
                }
                TimingKind.CUSTOM -> {
                    if (timing.customNote.isNullOrBlank()) {
                        result += ValidationResult.error(
                            code = "RESP_CUSTOM_TIMING_REQUIRED",
                            message = "Custom timing requires a note",
                            field = "customNote",
                        )
                    }
                }
                TimingKind.CURRENT_OUTSTANDING,
                TimingKind.AS_LONG_AS_REQUIRED,
                -> Unit
            }
        }

        val hasAmount =
            (responsibility.currentAmount?.amountRupees ?: 0L) > 0L ||
                (responsibility.monthlyAmount?.amountRupees ?: 0L) > 0L
        if (!hasAmount) {
            result += ValidationResult.error(
                code = "RESP_AMOUNT_REQUIRED",
                message = "Selected responsibilities require a current or monthly amount",
                field = "amount",
            )
        }

        if (responsibility.catalogue == ResponsibilityCatalogue.OTHER &&
            responsibility.explicitInflationBps == null
        ) {
            result += ValidationResult.error(
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
            result += validateDetails(item)
        }
        return result
    }
}
