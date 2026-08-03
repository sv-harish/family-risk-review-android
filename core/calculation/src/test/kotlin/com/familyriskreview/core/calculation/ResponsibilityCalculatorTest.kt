package com.familyriskreview.core.calculation

import com.familyriskreview.core.calculation.ResponsibilityCalculator.futureValue
import com.familyriskreview.core.calculation.ResponsibilityCalculator.indicativeGrossResponsibility
import com.familyriskreview.core.calculation.ResponsibilityCalculator.recurringSupportIndicative
import com.familyriskreview.core.model.MoneyAmount
import com.familyriskreview.core.model.Responsibility
import com.familyriskreview.core.model.ResponsibilityCatalogue
import com.familyriskreview.core.model.ResponsibilityPriority
import com.familyriskreview.core.model.ResponsibilityTiming
import com.familyriskreview.core.model.TimingKind
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ResponsibilityCalculatorTest {

    @Test
    fun futureValue_zeroYears_returnsCurrent() {
        assertThat(futureValue(100_000, 0, 0.08)).isEqualTo(100_000)
    }

    @Test
    fun futureValue_educationDefaultInflation() {
        // 10,00,000 at 8% for 10 years ≈ 21,58,925
        val result = futureValue(1_000_000, 10, 0.08)
        assertThat(result).isEqualTo(2_158_925)
    }

    @Test
    fun recurringSupport_fiveYearsNoDiscount() {
        // 50,000 / month, 5 years, 6% inflation
        val result = recurringSupportIndicative(
            monthlyAmountRupees = 50_000,
            durationYears = 5,
            inflationRateAnnual = 0.06,
        )
        assertThat(result).isGreaterThan(50_000L * 12 * 5)
        assertThat(result).isEqualTo(3_382_256L)
    }

    @Test
    fun indicativeGross_onlyMustContinueCountsPrimary() {
        val responsibilities = listOf(
            Responsibility(
                id = "1",
                reviewId = "r1",
                catalogue = ResponsibilityCatalogue.CHILD_HIGHER_EDUCATION,
                priority = ResponsibilityPriority.MUST_CONTINUE,
                timing = ResponsibilityTiming(TimingKind.ONE_TIME_IN_YEARS, yearsUntilRequired = 0),
                currentAmount = MoneyAmount(1_000_000),
                isSelected = true,
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

        val result = indicativeGrossResponsibility(responsibilities)
        assertThat(result.mustContinueTotalRupees).isEqualTo(1_000_000)
        assertThat(result.adjustableTotalRupees).isEqualTo(500_000)
        assertThat(result.calculationVersion).isEqualTo(ResponsibilityCalculator.CALCULATION_VERSION)
    }
}
