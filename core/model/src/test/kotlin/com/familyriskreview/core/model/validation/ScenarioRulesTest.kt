package com.familyriskreview.core.model.validation

import com.familyriskreview.core.model.AnnualRateBps
import com.familyriskreview.core.model.CalculationAssumptions
import com.familyriskreview.core.model.CalculationScenario
import com.familyriskreview.core.model.ReviewMode
import com.familyriskreview.core.model.ScenarioKind
import com.familyriskreview.core.model.result.DomainError
import com.familyriskreview.core.model.result.DomainResult
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ScenarioRulesTest {
    private val base =
        CalculationScenario(ScenarioKind.BASE, CalculationAssumptions.Default)
    private val lower =
        CalculationScenario(
            ScenarioKind.LOWER_COST,
            CalculationAssumptions.Default.copy(educationInflation = AnnualRateBps(600)),
        )
    private val higher =
        CalculationScenario(
            ScenarioKind.HIGHER_COST,
            CalculationAssumptions.Default.copy(educationInflation = AnnualRateBps(1000)),
        )

    @Test
    fun emptyList_ok() {
        val result = ScenarioRules.validateAndOrder(ReviewMode.QUICK, emptyList())
        assertThat(result).isInstanceOf(DomainResult.Success::class.java)
        assertThat((result as DomainResult.Success).value).isEmpty()
    }

    @Test
    fun quickBaseOnly_ok() {
        val result = ScenarioRules.validateAndOrder(ReviewMode.QUICK, listOf(base))
        assertThat((result as DomainResult.Success).value.map { it.kind })
            .containsExactly(ScenarioKind.BASE)
    }

    @Test
    fun quickLowerRejected() {
        val result = ScenarioRules.validateAndOrder(ReviewMode.QUICK, listOf(lower))
        assertThat(result).isInstanceOf(DomainResult.Failure::class.java)
    }

    @Test
    fun quickDuplicateBaseRejected() {
        val result = ScenarioRules.validateAndOrder(ReviewMode.QUICK, listOf(base, base))
        assertThat(
            ((result as DomainResult.Failure).error as DomainError.Validation)
                .issues
                .any { it.code == "SCENARIO_DUPLICATE_KIND" },
        ).isTrue()
    }

    @Test
    fun guidedOrdersDeterministically() {
        val result =
            ScenarioRules.validateAndOrder(
                ReviewMode.GUIDED,
                listOf(higher, lower, base),
            )
        assertThat((result as DomainResult.Success).value.map { it.kind })
            .containsExactly(
                ScenarioKind.LOWER_COST,
                ScenarioKind.BASE,
                ScenarioKind.HIGHER_COST,
            )
            .inOrder()
    }

    @Test
    fun duplicateLowerRejected() {
        val result =
            ScenarioRules.validateAndOrder(
                ReviewMode.GUIDED,
                listOf(lower, lower.copy()),
            )
        assertThat(result).isInstanceOf(DomainResult.Failure::class.java)
    }

    @Test
    fun guidedDuplicateBaseRejected() {
        val result =
            ScenarioRules.validateAndOrder(
                ReviewMode.GUIDED,
                listOf(base, base.copy()),
            )
        assertThat(
            ((result as DomainResult.Failure).error as DomainError.Validation)
                .issues
                .any { it.code == "SCENARIO_DUPLICATE_KIND" },
        ).isTrue()
    }

    @Test
    fun guidedBaseOnly_ok() {
        val result = ScenarioRules.validateAndOrder(ReviewMode.GUIDED, listOf(base))
        assertThat((result as DomainResult.Success).value.map { it.kind })
            .containsExactly(ScenarioKind.BASE)
    }

    @Test
    fun guidedLowerHigherWithoutBase_ordered() {
        val result =
            ScenarioRules.validateAndOrder(
                ReviewMode.GUIDED,
                listOf(higher, lower),
            )
        assertThat((result as DomainResult.Success).value.map { it.kind })
            .containsExactly(ScenarioKind.LOWER_COST, ScenarioKind.HIGHER_COST)
            .inOrder()
    }

    @Test
    fun unsupportedBaseAssumptionVersion_rejected() {
        val bad =
            CalculationScenario(
                ScenarioKind.BASE,
                CalculationAssumptions.Default.copy(version = "0.0.1"),
            )
        val result = ScenarioRules.validateAndOrder(ReviewMode.GUIDED, listOf(bad))
        assertThat(
            ((result as DomainResult.Failure).error as DomainError.Validation)
                .issues
                .any { it.code == "SCENARIO_UNSUPPORTED_ASSUMPTION_VERSION" },
        ).isTrue()
    }

    @Test
    fun unsupportedLowerAssumptionVersion_rejected() {
        val bad =
            CalculationScenario(
                ScenarioKind.LOWER_COST,
                CalculationAssumptions.Default.copy(version = "9.9.9"),
            )
        val result = ScenarioRules.validateAndOrder(ReviewMode.GUIDED, listOf(bad, base))
        assertThat(result).isInstanceOf(DomainResult.Failure::class.java)
    }
}
