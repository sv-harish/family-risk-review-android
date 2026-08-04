package com.familyriskreview.feature.review

import com.familyriskreview.core.model.ReviewStep

/**
 * Advisor-facing journey stages. Groups granular [ReviewStep] values into
 * calm labels without changing domain step order (ADR-027).
 */
enum class ReviewJourneyStage {
    YOUR_FAMILY,
    RESPONSIBILITIES,
    PRIORITIES,
    DETAILS,
    TIMELINE,
    UNDERSTANDING_THE_GAP,
    SUMMARY,
}

fun ReviewStep.toJourneyStage(): ReviewJourneyStage = when (this) {
    ReviewStep.HOUSEHOLD_SUPPORT_MAP -> ReviewJourneyStage.YOUR_FAMILY
    ReviewStep.RESPONSIBILITIES -> ReviewJourneyStage.RESPONSIBILITIES
    ReviewStep.PRIORITISATION -> ReviewJourneyStage.PRIORITIES
    ReviewStep.RESPONSIBILITY_DETAILS -> ReviewJourneyStage.DETAILS
    ReviewStep.TIMELINE -> ReviewJourneyStage.TIMELINE
    ReviewStep.INCOME_RISK_EDUCATION,
    ReviewStep.KEY_REALIZATION,
    ReviewStep.GROSS_RESPONSIBILITY,
    ReviewStep.EDUCATIONAL_COMPARISON,
    -> ReviewJourneyStage.UNDERSTANDING_THE_GAP
    ReviewStep.AWARENESS_SUMMARY,
    ReviewStep.ADVISOR_HANDOFF,
    -> ReviewJourneyStage.SUMMARY
}

fun ReviewJourneyStage.labelResId(): Int = when (this) {
    ReviewJourneyStage.YOUR_FAMILY -> R.string.stage_your_family
    ReviewJourneyStage.RESPONSIBILITIES -> R.string.stage_responsibilities
    ReviewJourneyStage.PRIORITIES -> R.string.stage_priorities
    ReviewJourneyStage.DETAILS -> R.string.stage_details
    ReviewJourneyStage.TIMELINE -> R.string.stage_timeline
    ReviewJourneyStage.UNDERSTANDING_THE_GAP -> R.string.stage_understanding_gap
    ReviewJourneyStage.SUMMARY -> R.string.stage_summary
}

val OrderedJourneyStages: List<ReviewJourneyStage> = ReviewJourneyStage.entries
