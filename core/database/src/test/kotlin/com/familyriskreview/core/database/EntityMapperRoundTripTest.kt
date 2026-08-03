package com.familyriskreview.core.database

import com.familyriskreview.core.database.mapper.toDomain
import com.familyriskreview.core.database.mapper.toEntity
import com.familyriskreview.core.model.AdvisorReference
import com.familyriskreview.core.model.AppLanguage
import com.familyriskreview.core.model.ContributionStatus
import com.familyriskreview.core.model.DependencyStatus
import com.familyriskreview.core.model.DerivedValueMetadata
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
import com.familyriskreview.core.model.SyncState
import com.familyriskreview.core.model.TimingKind
import com.google.common.truth.Truth.assertThat
import kotlinx.datetime.Instant
import org.junit.Test

class EntityMapperRoundTripTest {
    @Test
    fun reviewRoundTrip_preservesModeAndStep() {
        val original =
            Review(
                id = "id-1",
                reviewNumber = "FRR-20260101-ABC123",
                mode = ReviewMode.GUIDED,
                language = AppLanguage.TAMIL,
                status = ReviewStatus.IN_PROGRESS,
                currentStep = ReviewStep.HOUSEHOLD_SUPPORT_MAP,
                createdAt = Instant.fromEpochMilliseconds(10),
                updatedAt = Instant.fromEpochMilliseconds(20),
                syncState = SyncState.LOCAL_ONLY,
                revision = 3,
            )
        assertThat(original.toEntity().toDomain()).isEqualTo(original)
    }

    @Test
    fun householdMemberRoundTrip() {
        val member =
            HouseholdMember(
                id = "m1",
                reviewId = "id-1",
                relationship = FamilyMemberType.CHILD,
                age = 12,
                contributionStatus = ContributionStatus.NON_CONTRIBUTOR,
                dependencyStatus = DependencyStatus.FULLY_DEPENDENT,
                displayLabel = "Child 1",
                sortOrder = 1,
            )
        assertThat(member.toEntity().toDomain()).isEqualTo(member)
    }

    @Test
    fun responsibilityRoundTrip_withDerivedMetadata() {
        val responsibility =
            Responsibility(
                id = "r1",
                reviewId = "id-1",
                catalogue = ResponsibilityCatalogue.CHILD_HIGHER_EDUCATION,
                priority = ResponsibilityPriority.MUST_CONTINUE,
                timing = ResponsibilityTiming(TimingKind.ONE_TIME_IN_YEARS, yearsUntilRequired = 10),
                currentAmount = MoneyAmount(1_000_000),
                futureIndicativeAmount = MoneyAmount(2_158_925),
                derivedMetadata =
                DerivedValueMetadata(
                    sourceCurrentAmountRupees = 1_000_000,
                    sourceMonthlyAmountRupees = null,
                    sourceYears = 10,
                    sourceDurationYears = null,
                    inflationRateBps = 800,
                    expectedNetReturnBps = null,
                    assumptionVersion = "1.1.0",
                    calculationVersion = "1.1.0",
                ),
                isSelected = true,
            )
        assertThat(responsibility.toEntity().toDomain()).isEqualTo(responsibility)
    }

    @Test
    fun advisorReferenceRoundTrip() {
        val ref =
            AdvisorReference(
                reviewId = "id-1",
                customerInitialsOrNickname = "RK",
                crmReference = "CRM-9",
                privateNote = "private",
                includeInCustomerSummary = false,
            )
        assertThat(ref.toEntity().toDomain()).isEqualTo(ref)
    }
}
