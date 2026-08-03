package com.familyriskreview.core.model

import kotlinx.serialization.Serializable

/**
 * Versioned calculation assumptions.
 * Never hard-code these rates inside UI composables.
 */
@Serializable
data class CalculationAssumptions(
    val version: String = CURRENT_VERSION,
    val educationInflationAnnual: Double = 0.08,
    val marriageInflationAnnual: Double = 0.06,
    val expenseInflationAnnual: Double = 0.06,
    val expectedNetReturnAnnual: Double? = null,
) {
    companion object {
        const val CURRENT_VERSION: String = "1.0.0"
        val Default: CalculationAssumptions = CalculationAssumptions()
    }
}

/**
 * Placeholder result for Indicative Gross Responsibility Value.
 * Full engine arrives in Phase 1.
 */
@Serializable
data class GrossResponsibilityResult(
    val mustContinueTotalRupees: Long,
    val adjustableTotalRupees: Long,
    val postponedTotalRupees: Long,
    val assumptionVersion: String,
    val calculationVersion: String,
    val scenario: ScenarioKind = ScenarioKind.BASE,
)

@Serializable
enum class ScenarioKind {
    LOWER_COST,
    BASE,
    HIGHER_COST,
}
