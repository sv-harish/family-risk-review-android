package com.familyriskreview.core.model.lifecycle

import com.familyriskreview.core.model.ReviewStatus
import com.familyriskreview.core.model.ReviewStep
import com.familyriskreview.core.model.result.DomainResult
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ReviewLifecycleTest {
    @Test
    fun allowsArchiveFromInProgressAndCompleted() {
        assertThat(ReviewLifecycle.canTransition(ReviewStatus.IN_PROGRESS, ReviewStatus.ARCHIVED)).isTrue()
        assertThat(ReviewLifecycle.canTransition(ReviewStatus.COMPLETED, ReviewStatus.ARCHIVED)).isTrue()
        assertThat(ReviewLifecycle.canTransition(ReviewStatus.DELETED, ReviewStatus.ARCHIVED)).isFalse()
    }

    @Test
    fun restoreUsesStoredPriorStatus() {
        assertThat(ReviewLifecycle.restoreTarget(ReviewStatus.COMPLETED)).isEqualTo(ReviewStatus.COMPLETED)
        assertThat(ReviewLifecycle.restoreTarget(ReviewStatus.IN_PROGRESS)).isEqualTo(ReviewStatus.IN_PROGRESS)
        assertThat(ReviewLifecycle.restoreTarget(null)).isEqualTo(ReviewStatus.IN_PROGRESS)
    }

    @Test
    fun deletedCannotTransition() {
        val result = ReviewLifecycle.transition(ReviewStatus.DELETED, ReviewStatus.IN_PROGRESS)
        assertThat(result).isInstanceOf(DomainResult.Failure::class.java)
    }

    @Test
    fun progressionStartsAtHousehold() {
        assertThat(ReviewProgression.GUIDED_STEPS.first())
            .isEqualTo(ReviewStep.HOUSEHOLD_SUPPORT_MAP)
        assertThat(ReviewProgression.nextStep(com.familyriskreview.core.model.ReviewMode.QUICK, ReviewStep.HOUSEHOLD_SUPPORT_MAP))
            .isEqualTo(ReviewStep.RESPONSIBILITIES)
    }
}
