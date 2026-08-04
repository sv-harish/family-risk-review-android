package com.familyriskreview.core.model.validation

import com.familyriskreview.core.model.ContributionStatus
import com.familyriskreview.core.model.DependencyStatus
import com.familyriskreview.core.model.FamilyMemberType
import com.familyriskreview.core.model.HouseholdMember
import com.familyriskreview.core.model.MoneyAmount
import com.familyriskreview.core.model.Responsibility
import com.familyriskreview.core.model.ResponsibilityCatalogue
import com.familyriskreview.core.model.ResponsibilityPriority
import com.familyriskreview.core.model.ResponsibilityTiming
import com.familyriskreview.core.model.ReviewMode
import com.familyriskreview.core.model.TimingKind
import com.familyriskreview.core.model.lifecycle.ReviewModePolicy
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HouseholdRulesTest {
    @Test
    fun rejectsDuplicateSelf() {
        val members =
            listOf(
                member("1", FamilyMemberType.SELF),
                member("2", FamilyMemberType.SELF),
            )
        val result = HouseholdRules.validateHousehold(members, null)
        assertThat(result.isValid).isFalse()
        assertThat(result.errors.any { it.code == "HOUSEHOLD_DUPLICATE_SELF" }).isTrue()
    }

    @Test
    fun rejectsFocusNonContributor() {
        val member =
            member(
                id = "1",
                type = FamilyMemberType.SELF,
                contribution = ContributionStatus.NON_CONTRIBUTOR,
            )
        val result = HouseholdRules.validateHousehold(listOf(member), "1")
        assertThat(result.isValid).isFalse()
    }

    @Test
    fun ageOutOfRangeIsError() {
        val result =
            HouseholdRules.validateMember(
                member("1", FamilyMemberType.CHILD).copy(age = 200),
                emptyList(),
            )
        assertThat(result.isValid).isFalse()
    }

    private fun member(
        id: String,
        type: FamilyMemberType,
        contribution: ContributionStatus = ContributionStatus.PRIMARY_INCOME,
    ) = HouseholdMember(
        id = id,
        reviewId = "r1",
        relationship = type,
        age = 40,
        contributionStatus = contribution,
        dependencyStatus = DependencyStatus.NOT_APPLICABLE,
    )
}

class ResponsibilityRulesTest {
    @Test
    fun quickModeLimitsMustContinue() {
        val items =
            (1..4).map { index ->
                Responsibility(
                    id = "id-$index",
                    reviewId = "r1",
                    catalogue = ResponsibilityCatalogue.ESSENTIAL_FAMILY_LIVING_EXPENSES,
                    priority = ResponsibilityPriority.MUST_CONTINUE,
                    isSelected = true,
                )
            }
        val result = ResponsibilityRules.validatePrioritisation(items, ReviewMode.QUICK)
        assertThat(result.isValid).isFalse()
        assertThat(result.errors.any { it.code == "RESP_QUICK_MUST_CONTINUE_LIMIT" }).isTrue()
        assertThat(ReviewModePolicy.Quick.maxMustContinue).isEqualTo(3)
    }

    @Test
    fun guidedAllowsMoreThanThreeMustContinue() {
        val items =
            (1..4).map { index ->
                Responsibility(
                    id = "id-$index",
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
                )
            }
        val result = ResponsibilityRules.validatePrioritisation(items, ReviewMode.GUIDED)
        assertThat(result.errors.any { it.code == "RESP_QUICK_MUST_CONTINUE_LIMIT" }).isFalse()
    }

    @Test
    fun otherRequiresLabelAndExplicitInflation() {
        val item =
            Responsibility(
                id = "1",
                reviewId = "r1",
                catalogue = ResponsibilityCatalogue.OTHER,
                isSelected = true,
                priority = ResponsibilityPriority.MUST_CONTINUE,
                timing = ResponsibilityTiming(TimingKind.CURRENT_OUTSTANDING),
                currentAmount = MoneyAmount(10_000),
            )
        val result = ResponsibilityRules.validateDetails(item, ReviewMode.GUIDED)
        assertThat(result.isValid).isFalse()
        assertThat(result.errors.map { it.code }).containsAtLeast(
            "RESP_CUSTOM_LABEL_REQUIRED",
            "RESP_EXPLICIT_INFLATION_REQUIRED",
        )
    }

    @Test
    fun asLongAsRequiredWithoutHorizonFails() {
        val item =
            Responsibility(
                id = "1",
                reviewId = "r1",
                catalogue = ResponsibilityCatalogue.PARENT_SUPPORT,
                isSelected = true,
                priority = ResponsibilityPriority.MUST_CONTINUE,
                timing = ResponsibilityTiming(TimingKind.AS_LONG_AS_REQUIRED),
                monthlyAmount = MoneyAmount(20_000),
            )
        val result = ResponsibilityRules.validateDetails(item, ReviewMode.GUIDED)
        assertThat(result.errors.any { it.code == "RESP_MODELLING_HORIZON_REQUIRED" }).isTrue()
    }

    @Test
    fun asLongAsRequiredWithModellingHorizonPasses() {
        val item =
            Responsibility(
                id = "1",
                reviewId = "r1",
                catalogue = ResponsibilityCatalogue.PARENT_SUPPORT,
                isSelected = true,
                priority = ResponsibilityPriority.MUST_CONTINUE,
                timing =
                ResponsibilityTiming(
                    TimingKind.AS_LONG_AS_REQUIRED,
                    modellingDurationYears = 15,
                ),
                monthlyAmount = MoneyAmount(20_000),
            )
        val result = ResponsibilityRules.validateDetails(item, ReviewMode.GUIDED)
        assertThat(result.isValid).isTrue()
    }

    @Test
    fun excludedNonQuantifiedSkipsAmountRules() {
        val item =
            Responsibility(
                id = "1",
                reviewId = "r1",
                catalogue = ResponsibilityCatalogue.PARENT_SUPPORT,
                isSelected = true,
                priority = ResponsibilityPriority.MUST_CONTINUE,
                timing = ResponsibilityTiming(TimingKind.AS_LONG_AS_REQUIRED),
                excludedFromNumericCalculation = true,
            )
        val result = ResponsibilityRules.validateDetails(item, ReviewMode.GUIDED)
        assertThat(result.isValid).isTrue()
    }

    @Test
    fun educationRejectsMonthlyOnly() {
        val item =
            Responsibility(
                id = "1",
                reviewId = "r1",
                catalogue = ResponsibilityCatalogue.CHILD_HIGHER_EDUCATION,
                isSelected = true,
                priority = ResponsibilityPriority.MUST_CONTINUE,
                timing =
                ResponsibilityTiming(
                    TimingKind.ONE_TIME_IN_YEARS,
                    yearsUntilRequired = 10,
                ),
                monthlyAmount = MoneyAmount(5_000),
            )
        val result = ResponsibilityRules.validateDetails(item, ReviewMode.GUIDED)
        assertThat(result.errors.map { it.code }).contains("RESP_MONTHLY_NOT_ALLOWED")
    }

    @Test
    fun quickDoesNotRequireFullDetailsForAdjustable() {
        val item =
            Responsibility(
                id = "1",
                reviewId = "r1",
                catalogue = ResponsibilityCatalogue.CHILD_MARRIAGE_SUPPORT,
                isSelected = true,
                priority = ResponsibilityPriority.IMPORTANT_BUT_ADJUSTABLE,
            )
        val result = ResponsibilityRules.validateDetails(item, ReviewMode.QUICK)
        assertThat(result.isValid).isTrue()
    }

    @Test
    fun untilMilestoneRequiresHorizonOrExclusion() {
        val item =
            Responsibility(
                id = "1",
                reviewId = "r1",
                catalogue = ResponsibilityCatalogue.CHILD_HIGHER_EDUCATION,
                isSelected = true,
                priority = ResponsibilityPriority.MUST_CONTINUE,
                timing =
                ResponsibilityTiming(
                    TimingKind.UNTIL_MILESTONE,
                    milestoneLabel = "Graduation",
                ),
                currentAmount = MoneyAmount(1_000_000),
            )
        val result = ResponsibilityRules.validateDetails(item, ReviewMode.GUIDED)
        assertThat(result.errors.any { it.code == "RESP_MILESTONE_HORIZON_REQUIRED" }).isTrue()
    }
}
