package com.familyriskreview.core.calculation

import com.familyriskreview.core.model.CalculationAssumptions
import com.familyriskreview.core.model.GrossResponsibilityResult
import com.familyriskreview.core.model.Responsibility
import com.familyriskreview.core.model.ResponsibilityPriority
import com.familyriskreview.core.model.ScenarioKind
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * Pure calculation engine for Family Risk Review.
 *
 * Phase 0 ships the documented formulas and a thin aggregator so unit tests
 * and module wiring are in place. Phase 1 expands coverage and edge cases.
 */
object ResponsibilityCalculator {

    const val CALCULATION_VERSION: String = "1.0.0"

    /**
     * One-time future value:
     * `futureValue = currentCost × (1 + inflationRate)^years`
     */
    fun futureValue(
        currentCostRupees: Long,
        years: Int,
        inflationRateAnnual: Double,
    ): Long {
        require(currentCostRupees >= 0) { "currentCostRupees must be non-negative" }
        require(years >= 0) { "years must be non-negative" }
        require(inflationRateAnnual >= 0.0) { "inflationRateAnnual must be non-negative" }
        if (years == 0) return currentCostRupees
        val factor = (1.0 + inflationRateAnnual).pow(years.toDouble())
        return (currentCostRupees.toDouble() * factor).roundToLong()
    }

    /**
     * Recurring support present-value approximation under inflation.
     *
     * Documented methodology (v1.0.0):
     * Sum inflated monthly amounts over [durationYears], using mid-year
     * compounding for annual inflation applied to the monthly base.
     *
     * When [expectedNetReturnAnnual] is null, returns the inflated-sum only
     * (no discounting). Phase 1 may introduce optional discounting for
     * detailed-gap assessments without changing awareness-flow defaults.
     */
    fun recurringSupportIndicative(
        monthlyAmountRupees: Long,
        durationYears: Int,
        inflationRateAnnual: Double,
        expectedNetReturnAnnual: Double? = null,
    ): Long {
        require(monthlyAmountRupees >= 0) { "monthlyAmountRupees must be non-negative" }
        require(durationYears >= 0) { "durationYears must be non-negative" }
        require(inflationRateAnnual >= 0.0) { "inflationRateAnnual must be non-negative" }

        if (durationYears == 0 || monthlyAmountRupees == 0L) return 0L

        var total = 0.0
        for (yearIndex in 0 until durationYears) {
            val yearFactor = (1.0 + inflationRateAnnual).pow(yearIndex.toDouble())
            val annualOutlay = monthlyAmountRupees.toDouble() * 12.0 * yearFactor
            total += if (expectedNetReturnAnnual == null) {
                annualOutlay
            } else {
                val discount = (1.0 + expectedNetReturnAnnual).pow(yearIndex.toDouble())
                annualOutlay / discount
            }
        }
        return total.roundToLong()
    }

    fun indicativeGrossResponsibility(
        responsibilities: List<Responsibility>,
        assumptions: CalculationAssumptions = CalculationAssumptions.Default,
    ): GrossResponsibilityResult {
        fun sumFor(priority: ResponsibilityPriority): Long =
            responsibilities
                .filter { it.isSelected && it.priority == priority }
                .sumOf { responsibilityIndicative(it, assumptions) }

        return GrossResponsibilityResult(
            mustContinueTotalRupees = sumFor(ResponsibilityPriority.MUST_CONTINUE),
            adjustableTotalRupees = sumFor(ResponsibilityPriority.IMPORTANT_BUT_ADJUSTABLE),
            postponedTotalRupees = sumFor(ResponsibilityPriority.CAN_BE_POSTPONED_OR_REDUCED),
            assumptionVersion = assumptions.version,
            calculationVersion = CALCULATION_VERSION,
            scenario = ScenarioKind.BASE,
        )
    }

    private fun responsibilityIndicative(
        responsibility: Responsibility,
        assumptions: CalculationAssumptions,
    ): Long {
        responsibility.futureIndicativeAmount?.let { return it.amountRupees }

        val timing = responsibility.timing
        val years = timing?.yearsUntilRequired
            ?: timing?.durationYears
            ?: 0

        val monthly = responsibility.monthlyAmount?.amountRupees
        if (monthly != null && monthly > 0L) {
            val duration = timing?.durationYears ?: years
            return recurringSupportIndicative(
                monthlyAmountRupees = monthly,
                durationYears = duration,
                inflationRateAnnual = responsibility.inflationRateUsed
                    ?: assumptions.expenseInflationAnnual,
                expectedNetReturnAnnual = assumptions.expectedNetReturnAnnual,
            )
        }

        val current = responsibility.currentAmount?.amountRupees ?: return 0L
        if (years <= 0) return current

        val rate = responsibility.inflationRateUsed
            ?: assumptions.expenseInflationAnnual
        return futureValue(current, years, rate)
    }
}
