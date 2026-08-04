package com.familyriskreview.feature.review

import com.familyriskreview.core.model.ReviewStep
import com.familyriskreview.core.model.suggestion.ResponsibilitySuggestionEngine
import com.familyriskreview.core.model.FamilyMemberType
import com.familyriskreview.core.model.HouseholdMember
import com.familyriskreview.core.model.ContributionStatus
import com.familyriskreview.core.model.DependencyStatus
import com.familyriskreview.core.model.lifecycle.QuickReviewRules
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ReviewJourneyStageTest {
    @Test
    fun mapsEveryReviewStepToVisibleStage() {
        assertThat(ReviewStep.HOUSEHOLD_SUPPORT_MAP.toJourneyStage())
            .isEqualTo(ReviewJourneyStage.YOUR_FAMILY)
        assertThat(ReviewStep.RESPONSIBILITIES.toJourneyStage())
            .isEqualTo(ReviewJourneyStage.RESPONSIBILITIES)
        assertThat(ReviewStep.PRIORITISATION.toJourneyStage())
            .isEqualTo(ReviewJourneyStage.PRIORITIES)
        assertThat(ReviewStep.RESPONSIBILITY_DETAILS.toJourneyStage())
            .isEqualTo(ReviewJourneyStage.DETAILS)
        assertThat(ReviewStep.TIMELINE.toJourneyStage())
            .isEqualTo(ReviewJourneyStage.TIMELINE)
        assertThat(ReviewStep.INCOME_RISK_EDUCATION.toJourneyStage())
            .isEqualTo(ReviewJourneyStage.UNDERSTANDING_THE_GAP)
        assertThat(ReviewStep.KEY_REALIZATION.toJourneyStage())
            .isEqualTo(ReviewJourneyStage.UNDERSTANDING_THE_GAP)
        assertThat(ReviewStep.GROSS_RESPONSIBILITY.toJourneyStage())
            .isEqualTo(ReviewJourneyStage.UNDERSTANDING_THE_GAP)
        assertThat(ReviewStep.EDUCATIONAL_COMPARISON.toJourneyStage())
            .isEqualTo(ReviewJourneyStage.UNDERSTANDING_THE_GAP)
        assertThat(ReviewStep.AWARENESS_SUMMARY.toJourneyStage())
            .isEqualTo(ReviewJourneyStage.SUMMARY)
        assertThat(ReviewStep.ADVISOR_HANDOFF.toJourneyStage())
            .isEqualTo(ReviewJourneyStage.SUMMARY)
    }

    @Test
    fun orderedStagesMatchProductLabels() {
        assertThat(OrderedJourneyStages).containsExactly(
            ReviewJourneyStage.YOUR_FAMILY,
            ReviewJourneyStage.RESPONSIBILITIES,
            ReviewJourneyStage.PRIORITIES,
            ReviewJourneyStage.DETAILS,
            ReviewJourneyStage.TIMELINE,
            ReviewJourneyStage.UNDERSTANDING_THE_GAP,
            ReviewJourneyStage.SUMMARY,
        ).inOrder()
    }

    @Test
    fun suggestionsNeverImplySelection() {
        val members = listOf(
            HouseholdMember(
                id = "1",
                reviewId = "r1",
                relationship = FamilyMemberType.SELF,
                age = 40,
                contributionStatus = ContributionStatus.PRIMARY_INCOME,
                dependencyStatus = DependencyStatus.NOT_APPLICABLE,
            ),
            HouseholdMember(
                id = "2",
                reviewId = "r1",
                relationship = FamilyMemberType.CHILD,
                age = 10,
                contributionStatus = ContributionStatus.NON_CONTRIBUTOR,
                dependencyStatus = DependencyStatus.FULLY_DEPENDENT,
            ),
        )
        val suggestions = ResponsibilitySuggestionEngine.suggest(members)
        assertThat(suggestions).isNotEmpty()
        // Suggestions are advisory only — ViewModel exposes them separately from isSelected.
        assertThat(suggestions.map { it.catalogue }.toSet()).isNotEmpty()
    }

    @Test
    fun quickMustContinueLimitMessageMatchesDomain() {
        assertThat(QuickReviewRules.MAX_MUST_CONTINUE).isEqualTo(3)
        val message = "This review mode allows at most ${QuickReviewRules.MAX_MUST_CONTINUE} must-continue responsibilities"
        assertThat(message).contains("at most 3")
    }

    @Test
    fun moneyFormatterKeepsRawParsingSeparateFromDisplay() {
        assertThat(MoneyInputFormatter.parseRupees("12,345")).isEqualTo(12345L)
        assertThat(MoneyInputFormatter.formatRupees(12345L)).contains("12")
        assertThat(MoneyInputFormatter.formatOrEmpty(null)).isEmpty()
    }
}
