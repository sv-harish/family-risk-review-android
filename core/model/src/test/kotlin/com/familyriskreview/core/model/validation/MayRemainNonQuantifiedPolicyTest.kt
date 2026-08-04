package com.familyriskreview.core.model.validation

import com.familyriskreview.core.model.ResponsibilityCatalogue
import com.familyriskreview.core.model.ResponsibilityPriority
import com.familyriskreview.core.model.ReviewMode
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MayRemainNonQuantifiedPolicyTest {
    @Test
    fun quick_adjustableAndPostponed_mayRemainNonQuantified_acrossCatalogue() {
        for (catalogue in ResponsibilityCatalogue.entries) {
            assertThat(
                ResponsibilityRules.mayRemainNonQuantified(
                    ReviewMode.QUICK,
                    ResponsibilityPriority.IMPORTANT_BUT_ADJUSTABLE,
                ),
            ).isTrue()
            assertThat(
                ResponsibilityRules.mayRemainNonQuantified(
                    ReviewMode.QUICK,
                    ResponsibilityPriority.CAN_BE_POSTPONED_OR_REDUCED,
                ),
            ).isTrue()
            // Policy is mode+priority based (catalogue-agnostic); catalogue loop documents coverage.
            assertThat(catalogue).isNotNull()
        }
    }

    @Test
    fun quick_mustContinue_andNullPriority_mayNotRemainNonQuantified() {
        assertThat(
            ResponsibilityRules.mayRemainNonQuantified(
                ReviewMode.QUICK,
                ResponsibilityPriority.MUST_CONTINUE,
            ),
        ).isFalse()
        assertThat(ResponsibilityRules.mayRemainNonQuantified(ReviewMode.QUICK, null)).isFalse()
    }

    @Test
    fun guided_neverMayRemainNonQuantified_forAnyPriority() {
        for (priority in ResponsibilityPriority.entries + listOf<ResponsibilityPriority?>(null)) {
            assertThat(ResponsibilityRules.mayRemainNonQuantified(ReviewMode.GUIDED, priority))
                .isFalse()
        }
        // Explicit catalogue sweep for documentation parity with Quick coverage.
        for (catalogue in ResponsibilityCatalogue.entries) {
            assertThat(
                ResponsibilityRules.mayRemainNonQuantified(
                    ReviewMode.GUIDED,
                    ResponsibilityPriority.IMPORTANT_BUT_ADJUSTABLE,
                ),
            ).isFalse()
            assertThat(catalogue).isNotNull()
        }
    }
}
