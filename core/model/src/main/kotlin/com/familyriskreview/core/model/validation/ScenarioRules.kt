package com.familyriskreview.core.model.validation

import com.familyriskreview.core.model.CalculationAssumptions
import com.familyriskreview.core.model.CalculationScenario
import com.familyriskreview.core.model.ReviewMode
import com.familyriskreview.core.model.ScenarioKind
import com.familyriskreview.core.model.lifecycle.ReviewModePolicy
import com.familyriskreview.core.model.result.DomainError
import com.familyriskreview.core.model.result.DomainResult
import com.familyriskreview.core.model.result.ValidationIssue

/**
 * Validates and deterministically orders calculation scenarios.
 *
 * Empty list → Base-only path handled by the use case (no invented scenarios).
 * Explicit list must have unique kinds; Quick permits Base only; Guided may
 * supply Lower / Base / Higher. Ordered output is always Lower → Base → Higher.
 * Every supplied scenario assumption set must use a supported assumption version.
 */
object ScenarioRules {
    private val CANONICAL_ORDER =
        listOf(ScenarioKind.LOWER_COST, ScenarioKind.BASE, ScenarioKind.HIGHER_COST)

    fun validateAndOrder(
        mode: ReviewMode,
        scenarios: List<CalculationScenario>,
    ): DomainResult<List<CalculationScenario>> {
        if (scenarios.isEmpty()) {
            return DomainResult.success(emptyList())
        }
        for (scenario in scenarios) {
            if (scenario.assumptions.version !in CalculationAssumptions.SUPPORTED_VERSIONS) {
                return DomainResult.failure(
                    DomainError.Validation(
                        listOf(
                            ValidationIssue(
                                code = "SCENARIO_UNSUPPORTED_ASSUMPTION_VERSION",
                                message =
                                "Scenario ${scenario.kind} uses unsupported assumption " +
                                    "version '${scenario.assumptions.version}'",
                                field = "assumptions.version",
                            ),
                        ),
                    ),
                )
            }
        }
        val policy = ReviewModePolicy.forMode(mode)
        val kinds = scenarios.map { it.kind }
        if (kinds.size != kinds.toSet().size) {
            return DomainResult.failure(
                DomainError.Validation(
                    listOf(
                        ValidationIssue(
                            code = "SCENARIO_DUPLICATE_KIND",
                            message = "Scenario kinds must be unique",
                        ),
                    ),
                ),
            )
        }
        if (!policy.allowMultipleScenarios) {
            if (scenarios.size > 1 || scenarios.any { it.kind != ScenarioKind.BASE }) {
                return DomainResult.failure(
                    DomainError.Validation(
                        listOf(
                            ValidationIssue(
                                code = "SCENARIO_NOT_ALLOWED",
                                message =
                                "This review mode permits only a single Base scenario " +
                                    "(or an empty list for the standard Base result)",
                            ),
                        ),
                    ),
                )
            }
        }
        val byKind = scenarios.associateBy { it.kind }
        val ordered =
            CANONICAL_ORDER.mapNotNull { kind -> byKind[kind] }
        return DomainResult.success(ordered)
    }
}
