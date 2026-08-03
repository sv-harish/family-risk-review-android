package com.familyriskreview.core.model.lifecycle

import com.familyriskreview.core.model.ReviewMode
import com.familyriskreview.core.model.ReviewStatus
import com.familyriskreview.core.model.ReviewStep
import com.familyriskreview.core.model.result.DomainError
import com.familyriskreview.core.model.result.DomainResult

/**
 * Review status transition rules.
 *
 * Archive stores [statusBeforeArchive] so restore returns to the prior
 * meaningful status (IN_PROGRESS or COMPLETED), not an arbitrary restart.
 */
object ReviewLifecycle {
    fun canTransition(from: ReviewStatus, to: ReviewStatus): Boolean = when (from) {
        ReviewStatus.IN_PROGRESS ->
            to == ReviewStatus.COMPLETED ||
                to == ReviewStatus.ARCHIVED ||
                to == ReviewStatus.DELETED
        ReviewStatus.COMPLETED -> to == ReviewStatus.ARCHIVED || to == ReviewStatus.DELETED
        ReviewStatus.ARCHIVED ->
            to == ReviewStatus.IN_PROGRESS ||
                to == ReviewStatus.COMPLETED ||
                to == ReviewStatus.DELETED
        ReviewStatus.DELETED -> false
    }

    fun transition(from: ReviewStatus, to: ReviewStatus): DomainResult<ReviewStatus> {
        if (!canTransition(from, to)) {
            return DomainResult.failure(
                DomainError.Transition("Cannot transition from $from to $to"),
            )
        }
        return DomainResult.success(to)
    }

    /**
     * Status to restore after un-archiving.
     * Uses stored prior status when available; defaults to IN_PROGRESS.
     */
    fun restoreTarget(statusBeforeArchive: ReviewStatus?): ReviewStatus = when (statusBeforeArchive) {
        ReviewStatus.COMPLETED -> ReviewStatus.COMPLETED
        ReviewStatus.IN_PROGRESS -> ReviewStatus.IN_PROGRESS
        else -> ReviewStatus.IN_PROGRESS
    }
}

/**
 * Ordered customer-journey steps for Guided and Quick modes.
 *
 * Quick Review uses the same conceptual stages but enforces a limit of three
 * must-continue responsibilities and lighter detail requirements (see
 * [QuickReviewRules]).
 */
object ReviewProgression {
    val GUIDED_STEPS: List<ReviewStep> = listOf(
        ReviewStep.HOUSEHOLD_SUPPORT_MAP,
        ReviewStep.RESPONSIBILITIES,
        ReviewStep.PRIORITISATION,
        ReviewStep.RESPONSIBILITY_DETAILS,
        ReviewStep.TIMELINE,
        ReviewStep.INCOME_RISK_EDUCATION,
        ReviewStep.KEY_REALIZATION,
        ReviewStep.GROSS_RESPONSIBILITY,
        ReviewStep.EDUCATIONAL_COMPARISON,
        ReviewStep.AWARENESS_SUMMARY,
        ReviewStep.ADVISOR_HANDOFF,
    )

    /** Quick uses the same ordered stages; scope differs via limits/validation. */
    val QUICK_STEPS: List<ReviewStep> = GUIDED_STEPS

    fun stepsFor(mode: ReviewMode): List<ReviewStep> = when (mode) {
        ReviewMode.QUICK -> QUICK_STEPS
        ReviewMode.GUIDED -> GUIDED_STEPS
    }

    fun nextStep(mode: ReviewMode, current: ReviewStep): ReviewStep? {
        val steps = stepsFor(mode)
        val index = steps.indexOf(current)
        if (index < 0 || index >= steps.lastIndex) return null
        return steps[index + 1]
    }

    fun previousStep(mode: ReviewMode, current: ReviewStep): ReviewStep? {
        val steps = stepsFor(mode)
        val index = steps.indexOf(current)
        if (index <= 0) return null
        return steps[index - 1]
    }

    fun isFirst(mode: ReviewMode, step: ReviewStep): Boolean = stepsFor(mode).firstOrNull() == step

    fun isLast(mode: ReviewMode, step: ReviewStep): Boolean = stepsFor(mode).lastOrNull() == step
}

object QuickReviewRules {
    /** Maximum selected must-continue responsibilities in Quick mode. */
    const val MAX_MUST_CONTINUE: Int = 3
}
