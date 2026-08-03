package com.familyriskreview.core.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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
    fun toJson(): String = AssumptionsJson.encodeToString(this)

    companion object {
        const val CURRENT_VERSION: String = "1.1.0"
        val Default: CalculationAssumptions = CalculationAssumptions()

        fun fromJson(json: String): CalculationAssumptions = AssumptionsJson.decodeFromString(json)
    }
}

internal val AssumptionsJson =
    Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
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
 * Persisted calculation snapshot for a review aggregate revision.
 * Summary is stale when [reviewRevision] no longer matches the review, or
 * when assumption/calculation versions diverge.
 */
@Serializable
data class CalculationSnapshot(
    val id: String,
    val reviewId: String,
    val reviewRevision: Long,
    val assumptionVersion: String,
    val calculationVersion: String,
    val scenarioKind: ScenarioKind,
    val assumptionsJson: String,
    val mustContinueTotalRupees: Long,
    val adjustableTotalRupees: Long,
    val postponedTotalRupees: Long,
    val perResponsibilityJson: String,
    val generatedAtEpochMs: Long,
)

@Serializable
data class ResponsibilityIndicativeLine(
    val responsibilityId: String,
    val catalogue: ResponsibilityCatalogue,
    val priority: ResponsibilityPriority?,
    val indicativeAmountRupees: Long,
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
