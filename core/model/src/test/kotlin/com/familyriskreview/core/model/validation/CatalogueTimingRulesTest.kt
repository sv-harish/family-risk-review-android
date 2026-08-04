package com.familyriskreview.core.model.validation

import com.familyriskreview.core.model.QuantificationStatus
import com.familyriskreview.core.model.Responsibility
import com.familyriskreview.core.model.ResponsibilityAmountModel
import com.familyriskreview.core.model.ResponsibilityCatalogue
import com.familyriskreview.core.model.ResponsibilityPriority
import com.familyriskreview.core.model.ResponsibilityTiming
import com.familyriskreview.core.model.ReviewMode
import com.familyriskreview.core.model.TimingKind
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CatalogueTimingRulesTest {
    @Test
    fun livingAllowsRecurringRejectsLoanTiming() {
        assertThat(
            CatalogueTimingRules.validate(
                ResponsibilityCatalogue.ESSENTIAL_FAMILY_LIVING_EXPENSES,
                TimingKind.RECURRING_DURATION,
                null,
            ).isValid,
        ).isTrue()
        assertThat(
            CatalogueTimingRules.validate(
                ResponsibilityCatalogue.ESSENTIAL_FAMILY_LIVING_EXPENSES,
                TimingKind.CURRENT_OUTSTANDING,
                null,
            ).errors.any { it.code == "RESP_TIMING_NOT_ALLOWED" },
        ).isTrue()
    }

    @Test
    fun loansRequireCurrentOutstanding() {
        assertThat(
            CatalogueTimingRules.validate(
                ResponsibilityCatalogue.HOME_LOAN_REPAYMENT,
                TimingKind.CURRENT_OUTSTANDING,
                null,
            ).isValid,
        ).isTrue()
        assertThat(
            CatalogueTimingRules.validate(
                ResponsibilityCatalogue.HOME_LOAN_REPAYMENT,
                TimingKind.RECURRING_DURATION,
                null,
            ).isValid,
        ).isFalse()
    }

    @Test
    fun educationAllowsOneTimeRejectsAsLongAsRequired() {
        assertThat(
            CatalogueTimingRules.validate(
                ResponsibilityCatalogue.CHILD_HIGHER_EDUCATION,
                TimingKind.ONE_TIME_IN_YEARS,
                null,
            ).isValid,
        ).isTrue()
        assertThat(
            CatalogueTimingRules.validate(
                ResponsibilityCatalogue.CHILD_HIGHER_EDUCATION,
                TimingKind.AS_LONG_AS_REQUIRED,
                null,
            ).isValid,
        ).isFalse()
    }

    @Test
    fun otherRequiresAmountModel() {
        assertThat(
            CatalogueTimingRules.validate(
                ResponsibilityCatalogue.OTHER,
                TimingKind.ONE_TIME_IN_YEARS,
                null,
            ).errors.any { it.code == "RESP_AMOUNT_MODEL_REQUIRED" },
        ).isTrue()
        assertThat(
            CatalogueTimingRules.validate(
                ResponsibilityCatalogue.OTHER,
                TimingKind.RECURRING_DURATION,
                ResponsibilityAmountModel.RECURRING,
            ).isValid,
        ).isTrue()
        assertThat(
            CatalogueTimingRules.validate(
                ResponsibilityCatalogue.OTHER,
                TimingKind.ONE_TIME_IN_YEARS,
                ResponsibilityAmountModel.RECURRING,
            ).isValid,
        ).isFalse()
    }

    @Test
    fun guidedAdjustableStillRequiresFullDetails() {
        val item =
            Responsibility(
                id = "1",
                reviewId = "r1",
                catalogue = ResponsibilityCatalogue.PARENT_SUPPORT,
                isSelected = true,
                priority = ResponsibilityPriority.IMPORTANT_BUT_ADJUSTABLE,
                quantificationStatus = QuantificationStatus.QUANTIFIED,
                timing =
                ResponsibilityTiming(
                    TimingKind.RECURRING_DURATION,
                    durationYears = 10,
                ),
                // missing monthly
            )
        val result = ResponsibilityRules.validateDetails(item, ReviewMode.GUIDED)
        assertThat(result.isValid).isFalse()
        assertThat(result.errors.any { it.code == "RESP_MONTHLY_REQUIRED" }).isTrue()
    }

    @Test
    fun parentSupport_allowsCustomRejectsOneTime() {
        assertThat(
            CatalogueTimingRules.validate(
                ResponsibilityCatalogue.PARENT_SUPPORT,
                TimingKind.CUSTOM,
                null,
            ).isValid,
        ).isTrue()
        assertThat(
            CatalogueTimingRules.validate(
                ResponsibilityCatalogue.PARENT_SUPPORT,
                TimingKind.ONE_TIME_IN_YEARS,
                null,
            ).errors.any { it.code == "RESP_TIMING_NOT_ALLOWED" },
        ).isTrue()
    }

    @Test
    fun house_rejectsRecurringAndLoanTiming() {
        assertThat(
            CatalogueTimingRules.validate(
                ResponsibilityCatalogue.BUYING_OR_COMPLETING_HOUSE,
                TimingKind.ONE_TIME_IN_YEARS,
                null,
            ).isValid,
        ).isTrue()
        assertThat(
            CatalogueTimingRules.validate(
                ResponsibilityCatalogue.BUYING_OR_COMPLETING_HOUSE,
                TimingKind.RECURRING_DURATION,
                null,
            ).isValid,
        ).isFalse()
        assertThat(
            CatalogueTimingRules.validate(
                ResponsibilityCatalogue.BUYING_OR_COMPLETING_HOUSE,
                TimingKind.CURRENT_OUTSTANDING,
                null,
            ).isValid,
        ).isFalse()
    }

    @Test
    fun supportCatalogue_rejectsCurrentOutstanding() {
        assertThat(
            CatalogueTimingRules.validate(
                ResponsibilityCatalogue.CHILDCARE_REPLACEMENT,
                TimingKind.CURRENT_OUTSTANDING,
                null,
            ).errors.any { it.code == "RESP_TIMING_NOT_ALLOWED" },
        ).isTrue()
    }
}
