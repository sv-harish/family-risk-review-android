package com.familyriskreview.core.data

import com.familyriskreview.core.calculation.ResponsibilityCalculator
import com.familyriskreview.core.model.MoneyAmount
import com.familyriskreview.core.model.QuantificationStatus
import com.familyriskreview.core.model.Responsibility
import com.familyriskreview.core.model.ResponsibilityCatalogue
import com.familyriskreview.core.model.ResponsibilityPriority
import com.familyriskreview.core.model.ResponsibilityTiming
import com.familyriskreview.core.model.ReviewMode
import com.familyriskreview.core.model.TimingKind
import com.familyriskreview.core.model.validation.ResponsibilityRules
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class Phase12QuantificationTest {
    @Test
    fun quickNonQuantifiedAppearsWithNullIndicativeAndExcludedFromTotals() {
        val lightweight =
            Responsibility(
                id = "adj",
                reviewId = "r1",
                catalogue = ResponsibilityCatalogue.CHILD_MARRIAGE_SUPPORT,
                priority = ResponsibilityPriority.IMPORTANT_BUT_ADJUSTABLE,
                isSelected = true,
                quantificationStatus = QuantificationStatus.NOT_YET_QUANTIFIED,
            )
        val must =
            Responsibility(
                id = "living",
                reviewId = "r1",
                catalogue = ResponsibilityCatalogue.ESSENTIAL_FAMILY_LIVING_EXPENSES,
                priority = ResponsibilityPriority.MUST_CONTINUE,
                isSelected = true,
                timing =
                ResponsibilityTiming(
                    TimingKind.RECURRING_DURATION,
                    durationYears = 10,
                ),
                monthlyAmount = MoneyAmount(10_000),
                quantificationStatus = QuantificationStatus.QUANTIFIED,
            )
        assertThat(ResponsibilityRules.validateDetails(lightweight, ReviewMode.QUICK).isValid).isTrue()
        assertThat(ResponsibilityCalculator.optionalIndicativeAmount(lightweight)).isNull()
        val gross =
            ResponsibilityCalculator.indicativeGrossResponsibility(listOf(lightweight, must))
        assertThat(gross.adjustableTotalRupees).isEqualTo(0L)
        assertThat(gross.mustContinueTotalRupees).isGreaterThan(0L)
    }

    @Test
    fun quickQuantifiedWithoutDetails_rejected() {
        val item =
            Responsibility(
                id = "adj",
                reviewId = "r1",
                catalogue = ResponsibilityCatalogue.CHILD_MARRIAGE_SUPPORT,
                priority = ResponsibilityPriority.IMPORTANT_BUT_ADJUSTABLE,
                isSelected = true,
                quantificationStatus = QuantificationStatus.QUANTIFIED,
            )
        val result = ResponsibilityRules.validateDetails(item, ReviewMode.QUICK)
        assertThat(result.errors.any { it.code == "RESP_LIGHTWEIGHT_MUST_BE_NON_QUANTIFIED" }).isTrue()
    }

    @Test
    fun reviewReaderApi_exposesReadsOnly() {
        val methods =
            com.familyriskreview.core.data.repository.ReviewReader::class.java.methods.map { it.name }
        assertThat(methods).doesNotContain("updateReviewCas")
        assertThat(methods).doesNotContain("advanceStep")
        assertThat(methods).doesNotContain("completeReview")
    }
}
