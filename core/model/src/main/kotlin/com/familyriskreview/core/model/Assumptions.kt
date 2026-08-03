package com.familyriskreview.core.model

import kotlinx.serialization.Serializable

/**
 * Versioned calculation assumptions using validated basis-point rates.
 * Never hard-code these rates inside UI composables.
 */
@Serializable
data class CalculationAssumptions(
    val version: String = CURRENT_VERSION,
    val educationInflation: AnnualRateBps = AnnualRateBps.EDUCATION_DEFAULT,
    val marriageInflation: AnnualRateBps = AnnualRateBps.MARRIAGE_DEFAULT,
    val expenseInflation: AnnualRateBps = AnnualRateBps.EXPENSE_DEFAULT,
    val recurringSupportInflation: AnnualRateBps = AnnualRateBps.RECURRING_SUPPORT_DEFAULT,
    val expectedNetReturn: AnnualRateBps? = null,
) {
    companion object {
        const val CURRENT_VERSION: String = "1.1.0"
        val Default: CalculationAssumptions = CalculationAssumptions()
    }
}

/**
 * Explicit scenario definition. A scenario label alone is never enough —
 * each scenario must carry its own assumption set.
 */
@Serializable
data class CalculationScenario(
    val kind: ScenarioKind,
    val assumptions: CalculationAssumptions,
)

@Serializable
enum class ScenarioKind {
    LOWER_COST,
    BASE,
    HIGHER_COST,
}

/**
 * Metadata that must accompany any stored derived (future/indicative) amount.
 * A derived value may be reused only when all of these still match the request.
 */
@Serializable
data class DerivedValueMetadata(
    val sourceCurrentAmountRupees: Long?,
    val sourceMonthlyAmountRupees: Long?,
    val sourceYears: Int?,
    val sourceDurationYears: Int?,
    val inflationRateBps: Int,
    val expectedNetReturnBps: Int?,
    val assumptionVersion: String,
    val calculationVersion: String,
)

@Serializable
data class GrossResponsibilityResult(
    val mustContinueTotalRupees: Long,
    val adjustableTotalRupees: Long,
    val postponedTotalRupees: Long,
    val assumptionVersion: String,
    val calculationVersion: String,
    val scenarioKind: ScenarioKind = ScenarioKind.BASE,
)

/**
 * Which default inflation assumption applies to a responsibility catalogue item.
 * Custom responsibilities require an explicit selection — never silently guess.
 */
@Serializable
enum class InflationAssumptionKind {
    EDUCATION,
    MARRIAGE,
    EXPENSE,
    RECURRING_SUPPORT,
    NONE,
    EXPLICIT,
}
