package com.familyriskreview.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class ResponsibilityCatalogue {
    ESSENTIAL_FAMILY_LIVING_EXPENSES,
    CHILD_HIGHER_EDUCATION,
    CHILD_MARRIAGE_SUPPORT,
    SPOUSE_OR_PARTNER_SUPPORT,
    PARENT_SUPPORT,
    HOME_LOAN_REPAYMENT,
    OTHER_OUTSTANDING_LOANS,
    BUYING_OR_COMPLETING_HOUSE,
    SPECIAL_NEEDS_DEPENDANT_SUPPORT,
    CHILDCARE_REPLACEMENT,
    HOUSEHOLD_CARE_REPLACEMENT,
    OTHER,
}

fun ResponsibilityCatalogue.defaultInflationKind(): InflationAssumptionKind = when (this) {
    ResponsibilityCatalogue.CHILD_HIGHER_EDUCATION -> InflationAssumptionKind.EDUCATION
    ResponsibilityCatalogue.CHILD_MARRIAGE_SUPPORT -> InflationAssumptionKind.MARRIAGE
    ResponsibilityCatalogue.ESSENTIAL_FAMILY_LIVING_EXPENSES -> InflationAssumptionKind.EXPENSE
    ResponsibilityCatalogue.PARENT_SUPPORT,
    ResponsibilityCatalogue.SPOUSE_OR_PARTNER_SUPPORT,
    ResponsibilityCatalogue.SPECIAL_NEEDS_DEPENDANT_SUPPORT,
    ResponsibilityCatalogue.CHILDCARE_REPLACEMENT,
    ResponsibilityCatalogue.HOUSEHOLD_CARE_REPLACEMENT,
    -> InflationAssumptionKind.RECURRING_SUPPORT
    ResponsibilityCatalogue.HOME_LOAN_REPAYMENT,
    ResponsibilityCatalogue.OTHER_OUTSTANDING_LOANS,
    -> InflationAssumptionKind.NONE
    ResponsibilityCatalogue.BUYING_OR_COMPLETING_HOUSE -> InflationAssumptionKind.EXPENSE
    ResponsibilityCatalogue.OTHER -> InflationAssumptionKind.EXPLICIT
}

@Serializable
enum class ResponsibilityPriority {
    MUST_CONTINUE,
    IMPORTANT_BUT_ADJUSTABLE,
    CAN_BE_POSTPONED_OR_REDUCED,
}

/**
 * Explicit quantitative state. Do not infer quantification from nullable amounts.
 *
 * [NOT_YET_QUANTIFIED] items appear in summaries with a null indicative amount and
 * are excluded from numeric totals (never silently ₹0).
 */
@Serializable
enum class QuantificationStatus {
    QUANTIFIED,
    NOT_YET_QUANTIFIED,
}

/**
 * Explicit amount model for custom / OTHER responsibilities.
 * Timing and amount fields are validated against this model — not by probing
 * which nullable money fields happen to be set.
 */
@Serializable
enum class ResponsibilityAmountModel {
    ONE_TIME,
    RECURRING,
}

@Serializable
enum class TimingKind {
    ONE_TIME_IN_YEARS,
    RECURRING_DURATION,
    CURRENT_OUTSTANDING,
    UNTIL_MILESTONE,
    AS_LONG_AS_REQUIRED,
    CUSTOM,
}

@Serializable
data class ResponsibilityTiming(
    val kind: TimingKind,
    val yearsUntilRequired: Int? = null,
    val durationYears: Int? = null,
    val milestoneLabel: String? = null,
    val customNote: String? = null,
    val modellingDurationYears: Int? = null,
)

@Serializable
data class MoneyAmount(
    val amountRupees: Long,
) {
    init {
        require(amountRupees >= 0) { "amountRupees must be non-negative" }
    }
}

@Serializable
data class Responsibility(
    val id: String,
    val reviewId: String,
    val catalogue: ResponsibilityCatalogue,
    val customLabel: String? = null,
    val priority: ResponsibilityPriority? = null,
    val timing: ResponsibilityTiming? = null,
    val currentAmount: MoneyAmount? = null,
    val monthlyAmount: MoneyAmount? = null,
    val futureIndicativeAmount: MoneyAmount? = null,
    val derivedMetadata: DerivedValueMetadata? = null,
    val explicitInflationBps: Int? = null,
    val assumptionVersion: String? = null,
    val calculationVersion: String? = null,
    val isSelected: Boolean = false,
    val sortOrder: Int = 0,
    val quantificationStatus: QuantificationStatus = QuantificationStatus.QUANTIFIED,
    /** Required when [catalogue] is [ResponsibilityCatalogue.OTHER]. */
    val amountModel: ResponsibilityAmountModel? = null,
) {
    fun isNonQuantified(): Boolean = quantificationStatus == QuantificationStatus.NOT_YET_QUANTIFIED

    fun isIncludedInNumericTotal(): Boolean = isSelected && quantificationStatus == QuantificationStatus.QUANTIFIED
}
