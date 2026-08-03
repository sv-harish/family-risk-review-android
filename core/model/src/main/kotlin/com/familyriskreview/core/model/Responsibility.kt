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

@Serializable
enum class ResponsibilityPriority {
    /** Included in the primary Indicative Gross Responsibility Value. */
    MUST_CONTINUE,

    /** Shown separately; amount/timing/scale could change. */
    IMPORTANT_BUT_ADJUSTABLE,

    /** Desirable but not essential to immediate continuity. */
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

@Serializable
data class ResponsibilityTiming(
    val kind: TimingKind,
    val yearsUntilRequired: Int? = null,
    val durationYears: Int? = null,
    val milestoneLabel: String? = null,
    val customNote: String? = null,
)

/**
 * Monetary amounts are stored as minor units (paise) or major units consistently.
 * Phase 0 stores amounts as [Long] major rupee units (whole rupees) for simplicity.
 * Formatted display strings are never the source of truth.
 */
@Serializable
data class MoneyAmount(
    val amountRupees: Long,
)

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
    val inflationRateUsed: Double? = null,
    val assumptionVersion: String? = null,
    val calculationVersion: String? = null,
    val isSelected: Boolean = false,
    val sortOrder: Int = 0,
)
