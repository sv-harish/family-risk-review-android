package com.familyriskreview.core.calculation

import com.familyriskreview.core.model.AnnualRateBps
import com.familyriskreview.core.model.CalculationAssumptions
import com.familyriskreview.core.model.DerivedValueMetadata
import com.familyriskreview.core.model.GrossResponsibilityResult
import com.familyriskreview.core.model.InflationAssumptionKind
import com.familyriskreview.core.model.MoneyAmount
import com.familyriskreview.core.model.Responsibility
import com.familyriskreview.core.model.ResponsibilityCatalogue
import com.familyriskreview.core.model.ResponsibilityPriority
import com.familyriskreview.core.model.ScenarioKind
import com.familyriskreview.core.model.TimingKind
import com.familyriskreview.core.model.defaultInflationKind
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * Pure calculation engine for Family Risk Review (calculation version 1.1.0).
 *
 * Arithmetic uses [BigDecimal] with documented rounding to whole rupees so
 * results are deterministic across devices.
 *
 * ## Recurring-support method (selected, documented)
 *
 * **Beginning-of-year cash-flow model (awareness flow):**
 * For each year index `y` in `0 until durationYears`:
 *
 * ```
 * yearOutlay = monthlyAmount × 12 × (1 + inflation)^(y)
 * ```
 *
 * When optional net return `r` is supplied, discount that year:
 *
 * ```
 * discounted = yearOutlay / (1 + r)^(y)
 * ```
 *
 * Sum years, then round half-up to whole rupees.
 *
 * This replaces the earlier contradictory “mid-year” wording. Mid-year timing
 * is not used in v1.1.0.
 */
object ResponsibilityCalculator {
    const val CALCULATION_VERSION: String = "1.1.0"

    private val MATH = MathContext.DECIMAL64
    private val ROUNDING = RoundingMode.HALF_UP

    /**
     * One-time future value:
     * `futureValue = currentCost × (1 + inflationRate)^years`
     */
    fun futureValue(
        currentCostRupees: Long,
        years: Int,
        inflation: AnnualRateBps,
    ): Long {
        require(currentCostRupees >= 0) { "currentCostRupees must be non-negative" }
        require(years >= 0) { "years must be non-negative" }
        requireNonOverflowBase(currentCostRupees)
        if (years == 0 || currentCostRupees == 0L) return currentCostRupees

        val factor = onePlus(inflation).pow(years, MATH)
        return BigDecimal
            .valueOf(currentCostRupees)
            .multiply(factor, MATH)
            .setScale(0, ROUNDING)
            .longValueExactChecked()
    }

    /**
     * Recurring support indicative total (beginning-of-year cash-flow model).
     */
    fun recurringSupportIndicative(
        monthlyAmountRupees: Long,
        durationYears: Int,
        inflation: AnnualRateBps,
        expectedNetReturn: AnnualRateBps? = null,
    ): Long {
        require(monthlyAmountRupees >= 0) { "monthlyAmountRupees must be non-negative" }
        require(durationYears >= 0) { "durationYears must be non-negative" }
        requireNonOverflowBase(monthlyAmountRupees)
        if (durationYears == 0 || monthlyAmountRupees == 0L) return 0L

        val annualBase = BigDecimal.valueOf(monthlyAmountRupees).multiply(BigDecimal.valueOf(12), MATH)
        val growth = onePlus(inflation)
        val discount = expectedNetReturn?.let { onePlus(it) }

        var total = BigDecimal.ZERO
        var growthFactor = BigDecimal.ONE
        var discountFactor = BigDecimal.ONE
        repeat(durationYears) {
            val yearOutlay = annualBase.multiply(growthFactor, MATH)
            val contribution =
                if (discount == null) {
                    yearOutlay
                } else {
                    yearOutlay.divide(discountFactor, MATH)
                }
            total = total.add(contribution, MATH)
            growthFactor = growthFactor.multiply(growth, MATH)
            if (discount != null) {
                discountFactor = discountFactor.multiply(discount, MATH)
            }
        }
        return total.setScale(0, ROUNDING).longValueExactChecked()
    }

    fun resolveInflation(
        responsibility: Responsibility,
        assumptions: CalculationAssumptions,
    ): AnnualRateBps {
        responsibility.explicitInflationBps?.let { return AnnualRateBps(it) }
        return when (responsibility.catalogue.defaultInflationKind()) {
            InflationAssumptionKind.EDUCATION -> assumptions.educationInflation
            InflationAssumptionKind.MARRIAGE -> assumptions.marriageInflation
            InflationAssumptionKind.EXPENSE -> assumptions.expenseInflation
            InflationAssumptionKind.RECURRING_SUPPORT -> assumptions.recurringSupportInflation
            InflationAssumptionKind.NONE -> AnnualRateBps.ZERO
            InflationAssumptionKind.EXPLICIT ->
                error(
                    "Custom/OTHER responsibilities require explicitInflationBps; " +
                        "do not silently guess an inflation assumption.",
                )
        }
    }

    fun indicativeAmount(
        responsibility: Responsibility,
        assumptions: CalculationAssumptions = CalculationAssumptions.Default,
    ): Long {
        if (canReuseDerived(responsibility, assumptions)) {
            return responsibility.futureIndicativeAmount!!.amountRupees
        }
        return computeIndicative(responsibility, assumptions).amountRupees
    }

    fun computeIndicative(
        responsibility: Responsibility,
        assumptions: CalculationAssumptions = CalculationAssumptions.Default,
    ): MoneyAmount {
        val inflation = resolveInflation(responsibility, assumptions)
        val timing = responsibility.timing
        val monthly = responsibility.monthlyAmount?.amountRupees
        if (monthly != null && monthly > 0L) {
            val duration =
                timing?.durationYears
                    ?: timing?.yearsUntilRequired
                    ?: 0
            val value =
                recurringSupportIndicative(
                    monthlyAmountRupees = monthly,
                    durationYears = duration,
                    inflation = inflation,
                    expectedNetReturn = assumptions.expectedNetReturn,
                )
            return MoneyAmount(value)
        }

        val current = responsibility.currentAmount?.amountRupees ?: return MoneyAmount(0)
        val years =
            when (timing?.kind) {
                TimingKind.CURRENT_OUTSTANDING, null -> 0
                else -> timing.yearsUntilRequired ?: timing.durationYears ?: 0
            }
        if (years <= 0 || inflation == AnnualRateBps.ZERO) {
            return MoneyAmount(current)
        }
        return MoneyAmount(futureValue(current, years, inflation))
    }

    fun buildDerivedMetadata(
        responsibility: Responsibility,
        assumptions: CalculationAssumptions,
        calculated: MoneyAmount,
    ): DerivedValueMetadata {
        val inflation = resolveInflation(responsibility, assumptions)
        return DerivedValueMetadata(
            sourceCurrentAmountRupees = responsibility.currentAmount?.amountRupees,
            sourceMonthlyAmountRupees = responsibility.monthlyAmount?.amountRupees,
            sourceYears = responsibility.timing?.yearsUntilRequired,
            sourceDurationYears = responsibility.timing?.durationYears,
            inflationRateBps = inflation.value,
            expectedNetReturnBps = assumptions.expectedNetReturn?.value,
            assumptionVersion = assumptions.version,
            calculationVersion = CALCULATION_VERSION,
        )
    }

    fun canReuseDerived(
        responsibility: Responsibility,
        assumptions: CalculationAssumptions,
    ): Boolean {
        val derived = responsibility.futureIndicativeAmount ?: return false
        val meta = responsibility.derivedMetadata ?: return false
        if (meta.assumptionVersion != assumptions.version) return false
        if (meta.calculationVersion != CALCULATION_VERSION) return false
        if (meta.sourceCurrentAmountRupees != responsibility.currentAmount?.amountRupees) return false
        if (meta.sourceMonthlyAmountRupees != responsibility.monthlyAmount?.amountRupees) return false
        if (meta.sourceYears != responsibility.timing?.yearsUntilRequired) return false
        if (meta.sourceDurationYears != responsibility.timing?.durationYears) return false
        val inflation =
            runCatching { resolveInflation(responsibility, assumptions) }.getOrNull()
                ?: return false
        if (meta.inflationRateBps != inflation.value) return false
        if (meta.expectedNetReturnBps != assumptions.expectedNetReturn?.value) return false
        // Sanity: derived amount present
        return derived.amountRupees >= 0
    }

    fun indicativeGrossResponsibility(
        responsibilities: List<Responsibility>,
        assumptions: CalculationAssumptions = CalculationAssumptions.Default,
        scenarioKind: ScenarioKind = ScenarioKind.BASE,
    ): GrossResponsibilityResult {
        fun sumFor(priority: ResponsibilityPriority): Long = checkedSum(
            responsibilities
                .asSequence()
                .filter { it.isSelected && it.priority == priority }
                .map { indicativeAmount(it, assumptions) },
        )

        return GrossResponsibilityResult(
            mustContinueTotalRupees = sumFor(ResponsibilityPriority.MUST_CONTINUE),
            adjustableTotalRupees = sumFor(ResponsibilityPriority.IMPORTANT_BUT_ADJUSTABLE),
            postponedTotalRupees = sumFor(ResponsibilityPriority.CAN_BE_POSTPONED_OR_REDUCED),
            assumptionVersion = assumptions.version,
            calculationVersion = CALCULATION_VERSION,
            scenarioKind = scenarioKind,
        )
    }

    /** Checked Long addition — never silently overflow aggregate totals. */
    fun checkedSum(values: Sequence<Long>): Long {
        var total = 0L
        for (value in values) {
            total = Math.addExact(total, value)
        }
        return total
    }

    /**
     * Scenario ranges are produced only when each scenario has an explicit assumption set.
     * If only Base is approved, callers should not invent Lower/Higher.
     */
    fun evaluateScenarios(
        responsibilities: List<Responsibility>,
        scenarios: List<com.familyriskreview.core.model.CalculationScenario>,
    ): List<GrossResponsibilityResult> = scenarios.map { scenario ->
        indicativeGrossResponsibility(
            responsibilities = responsibilities,
            assumptions = scenario.assumptions,
            scenarioKind = scenario.kind,
        )
    }

    fun defaultInflationForCatalogue(
        catalogue: ResponsibilityCatalogue,
        assumptions: CalculationAssumptions = CalculationAssumptions.Default,
    ): AnnualRateBps? = when (catalogue.defaultInflationKind()) {
        InflationAssumptionKind.EDUCATION -> assumptions.educationInflation
        InflationAssumptionKind.MARRIAGE -> assumptions.marriageInflation
        InflationAssumptionKind.EXPENSE -> assumptions.expenseInflation
        InflationAssumptionKind.RECURRING_SUPPORT -> assumptions.recurringSupportInflation
        InflationAssumptionKind.NONE -> AnnualRateBps.ZERO
        InflationAssumptionKind.EXPLICIT -> null
    }

    private fun onePlus(rate: AnnualRateBps): BigDecimal = BigDecimal.ONE.add(
        BigDecimal.valueOf(rate.value.toLong()).movePointLeft(4),
        MATH,
    )

    private fun requireNonOverflowBase(amount: Long) {
        // Guard extreme bases that would explode under multi-year compounding.
        require(amount <= 1_000_000_000_000L) {
            "amount exceeds supported calculation range"
        }
    }

    private fun BigDecimal.longValueExactChecked(): Long = try {
        longValueExact()
    } catch (ex: ArithmeticException) {
        throw IllegalArgumentException("calculation result overflows Long", ex)
    }
}
