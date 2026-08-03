package com.familyriskreview.core.calculation

import com.familyriskreview.core.model.AnnualRateBps
import com.familyriskreview.core.model.CalculationAssumptions
import com.familyriskreview.core.model.CalculationScenario
import com.familyriskreview.core.model.DerivedValueMetadata
import com.familyriskreview.core.model.MoneyAmount
import com.familyriskreview.core.model.Responsibility
import com.familyriskreview.core.model.ResponsibilityCatalogue
import com.familyriskreview.core.model.ResponsibilityPriority
import com.familyriskreview.core.model.ResponsibilityTiming
import com.familyriskreview.core.model.ScenarioKind
import com.familyriskreview.core.model.TimingKind
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class ResponsibilityCalculatorTest {
    @Test
    fun futureValue_zeroYears_returnsCurrent() {
        assertThat(
            ResponsibilityCalculator.futureValue(100_000, 0, AnnualRateBps(800)),
        ).isEqualTo(100_000)
    }

    @Test
    fun futureValue_educationDefaultInflation() {
        val result = ResponsibilityCalculator.futureValue(1_000_000, 10, AnnualRateBps.EDUCATION_DEFAULT)
        assertThat(result).isEqualTo(2_158_925)
    }

    @Test
    fun futureValue_marriageDefaultInflation() {
        val result = ResponsibilityCalculator.futureValue(500_000, 5, AnnualRateBps.MARRIAGE_DEFAULT)
        assertThat(result).isEqualTo(669_113)
    }

    @Test
    fun loan_noInflation_keepsOutstanding() {
        val loan =
            Responsibility(
                id = "loan",
                reviewId = "r1",
                catalogue = ResponsibilityCatalogue.HOME_LOAN_REPAYMENT,
                priority = ResponsibilityPriority.MUST_CONTINUE,
                timing = ResponsibilityTiming(TimingKind.CURRENT_OUTSTANDING),
                currentAmount = MoneyAmount(2_400_000),
                isSelected = true,
            )
        assertThat(ResponsibilityCalculator.indicativeAmount(loan)).isEqualTo(2_400_000)
        assertThat(
            ResponsibilityCalculator.resolveInflation(loan, CalculationAssumptions.Default),
        ).isEqualTo(AnnualRateBps.ZERO)
    }

    @Test
    fun recurringSupport_fiveYearsNoDiscount() {
        val result =
            ResponsibilityCalculator.recurringSupportIndicative(
                monthlyAmountRupees = 50_000,
                durationYears = 5,
                inflation = AnnualRateBps(600),
            )
        assertThat(result).isEqualTo(3_382_256L)
    }

    @Test
    fun recurringSupport_withOptionalDiscounting() {
        val undiscounted =
            ResponsibilityCalculator.recurringSupportIndicative(
                monthlyAmountRupees = 50_000,
                durationYears = 5,
                inflation = AnnualRateBps(600),
            )
        val discounted =
            ResponsibilityCalculator.recurringSupportIndicative(
                monthlyAmountRupees = 50_000,
                durationYears = 5,
                inflation = AnnualRateBps(600),
                expectedNetReturn = AnnualRateBps(400),
            )
        assertThat(discounted).isLessThan(undiscounted)
        assertThat(discounted).isGreaterThan(0)
    }

    @Test
    fun zeroAmount_returnsZero() {
        assertThat(
            ResponsibilityCalculator.futureValue(0, 10, AnnualRateBps(800)),
        ).isEqualTo(0)
        assertThat(
            ResponsibilityCalculator.recurringSupportIndicative(0, 10, AnnualRateBps(600)),
        ).isEqualTo(0)
    }

    @Test
    fun invalidDuration_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            ResponsibilityCalculator.recurringSupportIndicative(1_000, -1, AnnualRateBps(600))
        }
    }

    @Test
    fun invalidRate_throwsOnConstruction() {
        assertThrows(IllegalArgumentException::class.java) {
            AnnualRateBps(-1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            AnnualRateBps(50_01)
        }
    }

    @Test
    fun overflowHandling_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            ResponsibilityCalculator.futureValue(1_000_000_000_001L, 1, AnnualRateBps(800))
        }
    }

    @Test
    fun deterministicRounding_stableAcrossCalls() {
        val a = ResponsibilityCalculator.futureValue(123_456, 7, AnnualRateBps(800))
        val b = ResponsibilityCalculator.futureValue(123_456, 7, AnnualRateBps(800))
        assertThat(a).isEqualTo(b)
    }

    @Test
    fun staleDerivedResult_recalculated() {
        val responsibility =
            baseEducation().copy(
                futureIndicativeAmount = MoneyAmount(9_999_999),
                derivedMetadata =
                DerivedValueMetadata(
                    sourceCurrentAmountRupees = 1_000_000,
                    sourceMonthlyAmountRupees = null,
                    sourceYears = 10,
                    sourceDurationYears = null,
                    inflationRateBps = 800,
                    expectedNetReturnBps = null,
                    assumptionVersion = "0.0.1",
                    calculationVersion = "1.1.0",
                ),
            )
        val amount = ResponsibilityCalculator.indicativeAmount(responsibility)
        assertThat(amount).isEqualTo(2_158_925)
    }

    @Test
    fun matchingDerivedResult_reused() {
        val calculated = 2_158_925L
        val responsibility =
            baseEducation().copy(
                futureIndicativeAmount = MoneyAmount(calculated),
                derivedMetadata =
                DerivedValueMetadata(
                    sourceCurrentAmountRupees = 1_000_000,
                    sourceMonthlyAmountRupees = null,
                    sourceYears = 10,
                    sourceDurationYears = null,
                    inflationRateBps = 800,
                    expectedNetReturnBps = null,
                    assumptionVersion = CalculationAssumptions.CURRENT_VERSION,
                    calculationVersion = ResponsibilityCalculator.CALCULATION_VERSION,
                ),
            )
        assertThat(ResponsibilityCalculator.canReuseDerived(responsibility, CalculationAssumptions.Default))
            .isTrue()
        assertThat(ResponsibilityCalculator.indicativeAmount(responsibility)).isEqualTo(calculated)
    }

    @Test
    fun assumptionVersionMismatch_forcesRecalc() {
        val responsibility =
            baseEducation().copy(
                futureIndicativeAmount = MoneyAmount(1),
                derivedMetadata =
                DerivedValueMetadata(
                    sourceCurrentAmountRupees = 1_000_000,
                    sourceMonthlyAmountRupees = null,
                    sourceYears = 10,
                    sourceDurationYears = null,
                    inflationRateBps = 800,
                    expectedNetReturnBps = null,
                    assumptionVersion = "old",
                    calculationVersion = ResponsibilityCalculator.CALCULATION_VERSION,
                ),
            )
        assertThat(ResponsibilityCalculator.canReuseDerived(responsibility, CalculationAssumptions.Default))
            .isFalse()
    }

    @Test
    fun calculationVersionMismatch_forcesRecalc() {
        val responsibility =
            baseEducation().copy(
                futureIndicativeAmount = MoneyAmount(1),
                derivedMetadata =
                DerivedValueMetadata(
                    sourceCurrentAmountRupees = 1_000_000,
                    sourceMonthlyAmountRupees = null,
                    sourceYears = 10,
                    sourceDurationYears = null,
                    inflationRateBps = 800,
                    expectedNetReturnBps = null,
                    assumptionVersion = CalculationAssumptions.CURRENT_VERSION,
                    calculationVersion = "0.9.0",
                ),
            )
        assertThat(ResponsibilityCalculator.canReuseDerived(responsibility, CalculationAssumptions.Default))
            .isFalse()
    }

    @Test
    fun customResponsibility_withoutExplicitRate_throws() {
        val custom =
            Responsibility(
                id = "c",
                reviewId = "r1",
                catalogue = ResponsibilityCatalogue.OTHER,
                currentAmount = MoneyAmount(100_000),
                timing = ResponsibilityTiming(TimingKind.ONE_TIME_IN_YEARS, yearsUntilRequired = 5),
                isSelected = true,
            )
        assertThrows(IllegalStateException::class.java) {
            ResponsibilityCalculator.resolveInflation(custom, CalculationAssumptions.Default)
        }
    }

    @Test
    fun scenarioDifferences_useExplicitAssumptions() {
        val items = listOf(baseEducation().copy(priority = ResponsibilityPriority.MUST_CONTINUE, isSelected = true))
        val lower =
            CalculationScenario(
                ScenarioKind.LOWER_COST,
                CalculationAssumptions(educationInflation = AnnualRateBps(400)),
            )
        val base =
            CalculationScenario(
                ScenarioKind.BASE,
                CalculationAssumptions.Default,
            )
        val higher =
            CalculationScenario(
                ScenarioKind.HIGHER_COST,
                CalculationAssumptions(educationInflation = AnnualRateBps(1000)),
            )
        val results = ResponsibilityCalculator.evaluateScenarios(items, listOf(lower, base, higher))
        assertThat(results[0].mustContinueTotalRupees)
            .isLessThan(results[1].mustContinueTotalRupees)
        assertThat(results[1].mustContinueTotalRupees)
            .isLessThan(results[2].mustContinueTotalRupees)
    }

    @Test
    fun indicativeGross_onlyMustContinueCountsPrimary() {
        val responsibilities =
            listOf(
                baseEducation().copy(
                    id = "1",
                    priority = ResponsibilityPriority.MUST_CONTINUE,
                    timing = ResponsibilityTiming(TimingKind.ONE_TIME_IN_YEARS, yearsUntilRequired = 0),
                    futureIndicativeAmount = null,
                    derivedMetadata = null,
                ),
                Responsibility(
                    id = "2",
                    reviewId = "r1",
                    catalogue = ResponsibilityCatalogue.CHILD_MARRIAGE_SUPPORT,
                    priority = ResponsibilityPriority.IMPORTANT_BUT_ADJUSTABLE,
                    timing = ResponsibilityTiming(TimingKind.ONE_TIME_IN_YEARS, yearsUntilRequired = 0),
                    currentAmount = MoneyAmount(500_000),
                    isSelected = true,
                ),
            )
        val result = ResponsibilityCalculator.indicativeGrossResponsibility(responsibilities)
        assertThat(result.mustContinueTotalRupees).isEqualTo(1_000_000)
        assertThat(result.adjustableTotalRupees).isEqualTo(500_000)
    }

    @Test
    fun checkedSum_throwsOnOverflow() {
        assertThrows(ArithmeticException::class.java) {
            ResponsibilityCalculator.checkedSum(sequenceOf(Long.MAX_VALUE, 1L))
        }
    }

    private fun baseEducation() = Responsibility(
        id = "edu",
        reviewId = "r1",
        catalogue = ResponsibilityCatalogue.CHILD_HIGHER_EDUCATION,
        priority = ResponsibilityPriority.MUST_CONTINUE,
        timing = ResponsibilityTiming(TimingKind.ONE_TIME_IN_YEARS, yearsUntilRequired = 10),
        currentAmount = MoneyAmount(1_000_000),
        isSelected = true,
    )
}
