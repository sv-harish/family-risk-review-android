package com.familyriskreview.core.model.validation

import com.familyriskreview.core.model.AppLanguage
import com.familyriskreview.core.model.CalculationAssumptions
import com.familyriskreview.core.model.CalculationSnapshot
import com.familyriskreview.core.model.ContributionStatus
import com.familyriskreview.core.model.DependencyStatus
import com.familyriskreview.core.model.FamilyMemberType
import com.familyriskreview.core.model.HouseholdMember
import com.familyriskreview.core.model.MoneyAmount
import com.familyriskreview.core.model.Responsibility
import com.familyriskreview.core.model.ResponsibilityCatalogue
import com.familyriskreview.core.model.ResponsibilityPriority
import com.familyriskreview.core.model.ResponsibilityTiming
import com.familyriskreview.core.model.Review
import com.familyriskreview.core.model.ReviewMode
import com.familyriskreview.core.model.ReviewStatus
import com.familyriskreview.core.model.ReviewStep
import com.familyriskreview.core.model.ScenarioKind
import com.familyriskreview.core.model.TimingKind
import com.google.common.truth.Truth.assertThat
import kotlinx.datetime.Instant
import org.junit.Test

class ProgressionGatesTest {
    private val review =
        Review(
            id = "r1",
            reviewNumber = "FRR-1",
            mode = ReviewMode.GUIDED,
            language = AppLanguage.ENGLISH,
            status = ReviewStatus.IN_PROGRESS,
            currentStep = ReviewStep.HOUSEHOLD_SUPPORT_MAP,
            createdAt = Instant.fromEpochMilliseconds(1),
            updatedAt = Instant.fromEpochMilliseconds(1),
            calculationVersion = "1.1.0",
            focusedIncomeContributorId = "m1",
            summaryStale = false,
            calculationInputRevision = 2,
            assumptionVersion = "1.1.0",
            assumptionsJson = CalculationAssumptions.Default.toJson(),
        )

    @Test
    fun householdGate_requiresMembers() {
        val result =
            ProgressionGates.validateLeaving(
                ReviewStep.HOUSEHOLD_SUPPORT_MAP,
                review,
                members = emptyList(),
                responsibilities = emptyList(),
                latestSnapshot = null,
                authoritativeCalculationVersion = "1.1.0",
            )
        assertThat(result.isValid).isFalse()
    }

    @Test
    fun responsibilitiesGate_requiresSelection() {
        val result =
            ProgressionGates.validateLeaving(
                ReviewStep.RESPONSIBILITIES,
                review,
                members = listOf(member()),
                responsibilities = emptyList(),
                latestSnapshot = null,
                authoritativeCalculationVersion = "1.1.0",
            )
        assertThat(result.errors.any { it.code == "RESP_NONE_SELECTED" }).isTrue()
    }

    @Test
    fun prioritisationGate_requiresPriorities() {
        val result =
            ProgressionGates.validateLeaving(
                ReviewStep.PRIORITISATION,
                review,
                members = listOf(member()),
                responsibilities =
                listOf(
                    Responsibility(
                        id = "x",
                        reviewId = "r1",
                        catalogue = ResponsibilityCatalogue.ESSENTIAL_FAMILY_LIVING_EXPENSES,
                        isSelected = true,
                        priority = null,
                    ),
                ),
                latestSnapshot = null,
                authoritativeCalculationVersion = "1.1.0",
            )
        assertThat(result.errors.any { it.code == "RESP_PRIORITY_REQUIRED" }).isTrue()
    }

    @Test
    fun detailsGate_requiresCatalogueAmounts() {
        val result =
            ProgressionGates.validateLeaving(
                ReviewStep.RESPONSIBILITY_DETAILS,
                review,
                members = listOf(member()),
                responsibilities =
                listOf(
                    Responsibility(
                        id = "x",
                        reviewId = "r1",
                        catalogue = ResponsibilityCatalogue.ESSENTIAL_FAMILY_LIVING_EXPENSES,
                        isSelected = true,
                        priority = ResponsibilityPriority.MUST_CONTINUE,
                        timing =
                        ResponsibilityTiming(
                            TimingKind.RECURRING_DURATION,
                            durationYears = 10,
                        ),
                    ),
                ),
                latestSnapshot = null,
                authoritativeCalculationVersion = "1.1.0",
            )
        assertThat(result.errors.any { it.code == "RESP_MONTHLY_REQUIRED" }).isTrue()
    }

    @Test
    fun grossResponsibilityGate_requiresFreshSnapshot() {
        val stale = review.copy(summaryStale = true)
        val result =
            ProgressionGates.validateLeaving(
                ReviewStep.GROSS_RESPONSIBILITY,
                stale,
                members = listOf(member()),
                responsibilities = listOf(living()),
                latestSnapshot = null,
                authoritativeCalculationVersion = "1.1.0",
            )
        assertThat(result.errors.any { it.code == "SUMMARY_STALE" }).isTrue()
    }

    @Test
    fun awarenessGate_requiresMatchingInputRevision() {
        val snapshot =
            CalculationSnapshot(
                id = "s",
                reviewId = "r1",
                reviewRevision = 3,
                calculationInputRevision = 1,
                assumptionVersion = "1.1.0",
                calculationVersion = "1.1.0",
                scenarioKind = ScenarioKind.BASE,
                assumptionsJson = CalculationAssumptions.Default.toJson(),
                mustContinueTotalRupees = 1,
                adjustableTotalRupees = 0,
                postponedTotalRupees = 0,
                perResponsibilityJson = "[]",
                generatedAtEpochMs = 1,
            )
        val result =
            ProgressionGates.validateLeaving(
                ReviewStep.AWARENESS_SUMMARY,
                review,
                members = listOf(member()),
                responsibilities = listOf(living()),
                latestSnapshot = snapshot,
                authoritativeCalculationVersion = "1.1.0",
            )
        assertThat(result.errors.any { it.code == "SUMMARY_INPUT_MISMATCH" }).isTrue()
    }

    private fun member() = HouseholdMember(
        id = "m1",
        reviewId = "r1",
        relationship = FamilyMemberType.SELF,
        age = 40,
        contributionStatus = ContributionStatus.PRIMARY_INCOME,
        dependencyStatus = DependencyStatus.NOT_APPLICABLE,
    )

    private fun living() = Responsibility(
        id = "x",
        reviewId = "r1",
        catalogue = ResponsibilityCatalogue.ESSENTIAL_FAMILY_LIVING_EXPENSES,
        isSelected = true,
        priority = ResponsibilityPriority.MUST_CONTINUE,
        timing = ResponsibilityTiming(TimingKind.RECURRING_DURATION, durationYears = 10),
        monthlyAmount = MoneyAmount(20_000),
    )
}
