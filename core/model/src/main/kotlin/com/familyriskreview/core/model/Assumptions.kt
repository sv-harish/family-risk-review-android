package com.familyriskreview.core.model

import com.familyriskreview.core.model.result.DomainError
import com.familyriskreview.core.model.result.DomainResult
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Versioned calculation assumptions using validated basis-point rates.
 * Never hard-code these rates inside UI composables.
 *
 * ## Decode policy (v1.1.0)
 *
 * - Creating a **new** review may use [Default].
 * - Reading a **stored** snapshot must use [parseStored]; malformed JSON or an
 *   unsupported [version] fails closed with [DomainError.CorruptData].
 * - Within the same supported version, kotlinx.serialization may apply Kotlin
 *   default parameter values for newly-added optional fields (documented additive
 *   migration within a version). Cross-version decode is not silently upgraded.
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

        /** Versions this build can decode without an explicit migration. */
        val SUPPORTED_VERSIONS: Set<String> = setOf(CURRENT_VERSION)

        val Default: CalculationAssumptions = CalculationAssumptions()

        fun fromJson(json: String): CalculationAssumptions = AssumptionsJson.decodeFromString(json)

        fun parseStored(json: String): DomainResult<CalculationAssumptions> {
            if (json.isBlank()) {
                return DomainResult.failure(
                    DomainError.CorruptData(
                        code = "INVALID_ASSUMPTION_SNAPSHOT",
                        message = "Assumption snapshot is blank",
                    ),
                )
            }
            val parsed =
                try {
                    fromJson(json)
                } catch (ex: Exception) {
                    return DomainResult.failure(
                        DomainError.CorruptData(
                            code = "INVALID_ASSUMPTION_SNAPSHOT",
                            message = "Assumption snapshot JSON is malformed: ${ex.message}",
                        ),
                    )
                }
            if (parsed.version !in SUPPORTED_VERSIONS) {
                return DomainResult.failure(
                    DomainError.CorruptData(
                        code = "UNSUPPORTED_ASSUMPTION_VERSION",
                        message = "Unsupported assumption version '${parsed.version}'",
                    ),
                )
            }
            return DomainResult.success(parsed)
        }
    }
}

internal val AssumptionsJson =
    Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

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

@Serializable
data class CalculationSnapshot(
    val id: String,
    val reviewId: String,
    val reviewRevision: Long,
    /** Bound to [Review.calculationInputRevision] at snapshot time. */
    val calculationInputRevision: Long,
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
    val indicativeAmountRupees: Long?,
    val excludedFromNumericCalculation: Boolean = false,
)

@Serializable
enum class InflationAssumptionKind {
    EDUCATION,
    MARRIAGE,
    EXPENSE,
    RECURRING_SUPPORT,
    NONE,
    EXPLICIT,
}
