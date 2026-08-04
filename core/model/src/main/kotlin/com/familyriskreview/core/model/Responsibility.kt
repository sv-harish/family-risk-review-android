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

@Serializable
enum class TimingKind {
    ONE_TIME_IN_YEARS,
    RECURRING_DURATION,
    CURRENT_OUTSTANDING,
    UNTIL_MILESTONE,
    AS_LONG_AS_REQUIRED,
    CUSTOM,
}

/**
 * Timing for a responsibility.
 *
 * For [TimingKind.AS_LONG_AS_REQUIRED], [TimingKind.UNTIL_MILESTONE], and
 * [TimingKind.CUSTOM], either a calculable horizon
 * ([modellingDurationYears] / [yearsUntilRequired] / [durationYears]) must be
 * set, or the parent responsibility must be marked
 * [Responsibility.excludedFromNumericCalculation].
 */
@Serializable
data class ResponsibilityTiming(
    val kind: TimingKind,
    val yearsUntilRequired: Int? = null,
    val durationYears: Int? = null,
    val milestoneLabel: String? = null,
    val customNote: String? = null,
    /**
     * Explicit modelling horizon (years) used for awareness calculations when
     * the conceptual duration is open-ended (e.g. “as long as required”).
     */
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
    /**
     * When true, the item is shown as “Duration not yet quantified” and is
     * excluded from numeric totals (never silently treated as ₹0).
     */
    val excludedFromNumericCalculation: Boolean = false,
) {
    fun isIncludedInNumericTotal(): Boolean = isSelected && !excludedFromNumericCalculation
}
