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
 *
 * Completed reviews are read-only until explicitly [reopen]ed to IN_PROGRESS.
 */
object ReviewLifecycle {
    fun canTransition(from: ReviewStatus, to: ReviewStatus): Boolean = when (from) {
        ReviewStatus.IN_PROGRESS ->
            to == ReviewStatus.COMPLETED ||
                to == ReviewStatus.ARCHIVED ||
                to == ReviewStatus.DELETED
        ReviewStatus.COMPLETED ->
            to == ReviewStatus.ARCHIVED ||
                to == ReviewStatus.DELETED ||
                to == ReviewStatus.IN_PROGRESS // explicit reopen
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

    fun restoreTarget(statusBeforeArchive: ReviewStatus?): ReviewStatus = when (statusBeforeArchive) {
        ReviewStatus.COMPLETED -> ReviewStatus.COMPLETED
        ReviewStatus.IN_PROGRESS -> ReviewStatus.IN_PROGRESS
        else -> ReviewStatus.IN_PROGRESS
    }
}

/**
 * Ordinary child-edit eligibility. COMPLETED/ARCHIVED require an explicit
 * lifecycle operation (reopen / restore) before mutation.
 */
object ReviewEditability {
    fun requireEditable(status: ReviewStatus): DomainResult<Unit> = when (status) {
        ReviewStatus.IN_PROGRESS -> DomainResult.success(Unit)
        ReviewStatus.COMPLETED ->
            DomainResult.failure(
                DomainError.IllegalState(
                    "Completed reviews are read-only; reopen before editing",
                ),
            )
        ReviewStatus.ARCHIVED ->
            DomainResult.failure(
                DomainError.IllegalState(
                    "Archived reviews are read-only; restore before editing",
                ),
            )
        ReviewStatus.DELETED ->
            DomainResult.failure(
                DomainError.IllegalState("Deleted reviews cannot be edited"),
            )
    }

    /** Snapshot persistence is allowed while IN_PROGRESS (and during calculate). */
    fun requireCalculable(status: ReviewStatus): DomainResult<Unit> = when (status) {
        ReviewStatus.IN_PROGRESS -> DomainResult.success(Unit)
        else ->
            DomainResult.failure(
                DomainError.IllegalState(
                    "Calculations may only be persisted for in-progress reviews",
                ),
            )
    }
}

/**
 * Explicit Quick vs Guided scope. Do not scatter mode checks in repositories.
 */
data class ReviewModePolicy(
    val maxMustContinue: Int?,
    val requireDetailsForAdjustable: Boolean,
    val requireDetailsForPostponed: Boolean,
    val allowMultipleScenarios: Boolean,
    val shorterSummaryProjection: Boolean,
) {
    companion object {
        val Quick: ReviewModePolicy =
            ReviewModePolicy(
                maxMustContinue = 3,
                requireDetailsForAdjustable = false,
                requireDetailsForPostponed = false,
                allowMultipleScenarios = false,
                shorterSummaryProjection = true,
            )

        val Guided: ReviewModePolicy =
            ReviewModePolicy(
                maxMustContinue = null,
                requireDetailsForAdjustable = true,
                requireDetailsForPostponed = true,
                allowMultipleScenarios = true,
                shorterSummaryProjection = false,
            )

        fun forMode(mode: ReviewMode): ReviewModePolicy = when (mode) {
            ReviewMode.QUICK -> Quick
            ReviewMode.GUIDED -> Guided
        }
    }
}

object ReviewProgression {
    val GUIDED_STEPS: List<ReviewStep> =
        listOf(
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

/** @deprecated Prefer [ReviewModePolicy.Quick.maxMustContinue]. */
object QuickReviewRules {
    const val MAX_MUST_CONTINUE: Int = 3
}
