package com.familyriskreview.core.model.lifecycle

import com.familyriskreview.core.model.ReviewMode
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
    fun allowsReopenFromCompleted() {
        assertThat(ReviewLifecycle.canTransition(ReviewStatus.COMPLETED, ReviewStatus.IN_PROGRESS)).isTrue()
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
        assertThat(ReviewProgression.nextStep(ReviewMode.QUICK, ReviewStep.HOUSEHOLD_SUPPORT_MAP))
            .isEqualTo(ReviewStep.RESPONSIBILITIES)
    }

    @Test
    fun editability_completedRequiresReopen() {
        assertThat(ReviewEditability.requireEditable(ReviewStatus.IN_PROGRESS))
            .isInstanceOf(DomainResult.Success::class.java)
        assertThat(ReviewEditability.requireEditable(ReviewStatus.COMPLETED))
            .isInstanceOf(DomainResult.Failure::class.java)
        assertThat(ReviewEditability.requireEditable(ReviewStatus.ARCHIVED))
            .isInstanceOf(DomainResult.Failure::class.java)
        assertThat(ReviewEditability.requireEditable(ReviewStatus.DELETED))
            .isInstanceOf(DomainResult.Failure::class.java)
    }

    @Test
    fun modePoliciesDiffer() {
        assertThat(ReviewModePolicy.Quick.maxMustContinue).isEqualTo(3)
        assertThat(ReviewModePolicy.Guided.maxMustContinue).isNull()
        assertThat(ReviewModePolicy.Quick.requireDetailsForAdjustable).isFalse()
        assertThat(ReviewModePolicy.Guided.requireDetailsForAdjustable).isTrue()
        assertThat(ReviewModePolicy.Quick.allowMultipleScenarios).isFalse()
        assertThat(ReviewModePolicy.Guided.allowMultipleScenarios).isTrue()
    }
}
