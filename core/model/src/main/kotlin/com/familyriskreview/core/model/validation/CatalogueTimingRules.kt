package com.familyriskreview.core.model.validation

import com.familyriskreview.core.model.ResponsibilityAmountModel
import com.familyriskreview.core.model.ResponsibilityCatalogue
import com.familyriskreview.core.model.TimingKind
import com.familyriskreview.core.model.result.ValidationResult

/**
 * Catalogue-specific allowed timing kinds.
 */
object CatalogueTimingRules {
    fun allowedKinds(
        catalogue: ResponsibilityCatalogue,
        amountModel: ResponsibilityAmountModel? = null,
    ): Set<TimingKind> = when (catalogue) {
        ResponsibilityCatalogue.ESSENTIAL_FAMILY_LIVING_EXPENSES ->
            setOf(
                TimingKind.RECURRING_DURATION,
                TimingKind.AS_LONG_AS_REQUIRED,
                TimingKind.UNTIL_MILESTONE,
            )
        ResponsibilityCatalogue.PARENT_SUPPORT,
        ResponsibilityCatalogue.SPOUSE_OR_PARTNER_SUPPORT,
        ResponsibilityCatalogue.SPECIAL_NEEDS_DEPENDANT_SUPPORT,
        ResponsibilityCatalogue.CHILDCARE_REPLACEMENT,
        ResponsibilityCatalogue.HOUSEHOLD_CARE_REPLACEMENT,
        ->
            setOf(
                TimingKind.RECURRING_DURATION,
                TimingKind.AS_LONG_AS_REQUIRED,
                TimingKind.UNTIL_MILESTONE,
                TimingKind.CUSTOM,
            )
        ResponsibilityCatalogue.CHILD_HIGHER_EDUCATION,
        ResponsibilityCatalogue.CHILD_MARRIAGE_SUPPORT,
        ResponsibilityCatalogue.BUYING_OR_COMPLETING_HOUSE,
        ->
            setOf(
                TimingKind.ONE_TIME_IN_YEARS,
                TimingKind.UNTIL_MILESTONE,
                TimingKind.CUSTOM,
            )
        ResponsibilityCatalogue.HOME_LOAN_REPAYMENT,
        ResponsibilityCatalogue.OTHER_OUTSTANDING_LOANS,
        -> setOf(TimingKind.CURRENT_OUTSTANDING)
        ResponsibilityCatalogue.OTHER ->
            when (amountModel) {
                ResponsibilityAmountModel.ONE_TIME ->
                    setOf(
                        TimingKind.ONE_TIME_IN_YEARS,
                        TimingKind.UNTIL_MILESTONE,
                        TimingKind.CUSTOM,
                        TimingKind.CURRENT_OUTSTANDING,
                    )
                ResponsibilityAmountModel.RECURRING ->
                    setOf(
                        TimingKind.RECURRING_DURATION,
                        TimingKind.AS_LONG_AS_REQUIRED,
                        TimingKind.UNTIL_MILESTONE,
                        TimingKind.CUSTOM,
                    )
                null -> emptySet()
            }
    }

    fun validate(
        catalogue: ResponsibilityCatalogue,
        timingKind: TimingKind?,
        amountModel: ResponsibilityAmountModel?,
    ): ValidationResult {
        if (timingKind == null) {
            return ValidationResult.error(
                code = "RESP_TIMING_REQUIRED",
                message = "Timing kind is required",
                field = "timing",
            )
        }
        if (catalogue == ResponsibilityCatalogue.OTHER && amountModel == null) {
            return ValidationResult.error(
                code = "RESP_AMOUNT_MODEL_REQUIRED",
                message = "Custom responsibilities require an explicit amount model",
                field = "amountModel",
            )
        }
        val allowed = allowedKinds(catalogue, amountModel)
        if (timingKind !in allowed) {
            return ValidationResult.error(
                code = "RESP_TIMING_NOT_ALLOWED",
                message =
                "Timing $timingKind is not allowed for catalogue $catalogue" +
                    (amountModel?.let { " ($it)" } ?: ""),
                field = "timing",
            )
        }
        return ValidationResult.Ok
    }
}
