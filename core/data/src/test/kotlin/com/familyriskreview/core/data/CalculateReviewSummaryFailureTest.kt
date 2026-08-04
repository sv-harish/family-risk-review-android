package com.familyriskreview.core.data

import com.familyriskreview.core.calculation.ResponsibilityCalculator
import com.familyriskreview.core.data.repository.CalculationSnapshotRepository
import com.familyriskreview.core.data.repository.ResponsibilityRepository
import com.familyriskreview.core.data.repository.ReviewRepository
import com.familyriskreview.core.data.usecase.CalculateReviewSummaryUseCase
import com.familyriskreview.core.model.AppLanguage
import com.familyriskreview.core.model.CalculationAssumptions
import com.familyriskreview.core.model.CalculationSnapshot
import com.familyriskreview.core.model.DerivedValueMetadata
import com.familyriskreview.core.model.MoneyAmount
import com.familyriskreview.core.model.QuantificationStatus
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
import com.familyriskreview.core.model.result.DomainError
import com.familyriskreview.core.model.result.DomainResult
import com.familyriskreview.core.model.service.Clock
import com.familyriskreview.core.model.service.IdGenerator
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import org.junit.Test

class CalculateReviewSummaryFailureTest {
    @Test
    fun aggregateOverflow_returnsTypedCalculationFailure_notRawException() = runBlocking {
        val review =
            Review(
                id = "r1",
                reviewNumber = "FRR-20260101-000001",
                mode = ReviewMode.GUIDED,
                language = AppLanguage.ENGLISH,
                status = ReviewStatus.IN_PROGRESS,
                currentStep = ReviewStep.AWARENESS_SUMMARY,
                createdAt = Instant.fromEpochMilliseconds(1),
                updatedAt = Instant.fromEpochMilliseconds(1),
                syncState = SyncState.LOCAL_ONLY,
                revision = 1L,
                calculationVersion = ResponsibilityCalculator.CALCULATION_VERSION,
                summaryStale = true,
                assumptionsJson = CalculationAssumptions.Default.toJson(),
            )
        // Reusable derived values large enough that checkedSum overflows Long.
        val half = Long.MAX_VALUE / 2 + 1
        fun loan(id: String) = Responsibility(
            id = id,
            reviewId = "r1",
            catalogue = ResponsibilityCatalogue.HOME_LOAN_REPAYMENT,
            priority = ResponsibilityPriority.MUST_CONTINUE,
            isSelected = true,
            quantificationStatus = QuantificationStatus.QUANTIFIED,
            timing = ResponsibilityTiming(TimingKind.CURRENT_OUTSTANDING),
            currentAmount = MoneyAmount(100_000),
            futureIndicativeAmount = MoneyAmount(half),
            derivedMetadata =
            DerivedValueMetadata(
                sourceCurrentAmountRupees = 100_000,
                sourceMonthlyAmountRupees = null,
                sourceYears = null,
                sourceDurationYears = 0,
                inflationRateBps = 0,
                expectedNetReturnBps = null,
                assumptionVersion = CalculationAssumptions.Default.version,
                calculationVersion = ResponsibilityCalculator.CALCULATION_VERSION,
            ),
        )
        val useCase =
            CalculateReviewSummaryUseCase(
                reviewRepository = FixedReviewRepository(review),
                responsibilityRepository =
                FixedResponsibilityRepository(listOf(loan("a"), loan("b"))),
                snapshotRepository = NoOpSnapshotRepository(review),
                clock =
                object : Clock {
                    override fun now(): Instant = Instant.fromEpochMilliseconds(1)
                },
                idGenerator =
                object : IdGenerator {
                    override fun newId(): String = "snap-1"
                },
            )
        val result = useCase(reviewId = "r1", expectedRevision = 1L)
        assertThat(result).isInstanceOf(DomainResult.Failure::class.java)
        val error = (result as DomainResult.Failure).error
        assertThat(error).isInstanceOf(DomainError.Calculation::class.java)
        assertThat((error as DomainError.Calculation).code).isEqualTo("CALC_AGGREGATE_OVERFLOW")
    }

    private class FixedReviewRepository(
        private val review: Review,
    ) : ReviewRepository {
        override fun observeActiveReviews(): Flow<List<Review>> = flowOf(listOf(review))

        override fun observeByStatus(status: ReviewStatus): Flow<List<Review>> = flowOf(emptyList())

        override fun observeInProgress(): Flow<List<Review>> = flowOf(listOf(review))

        override fun observeReview(id: String): Flow<Review?> = flowOf(review)

        override suspend fun getReview(id: String): Review? = review

        override suspend fun createReview(
            mode: ReviewMode,
            language: AppLanguage,
            assumptions: CalculationAssumptions,
        ): DomainResult<Review> = error("unused")

        override suspend fun archiveReview(
            id: String,
            expectedRevision: Long,
        ): DomainResult<Review> = error("unused")

        override suspend fun restoreReview(
            id: String,
            expectedRevision: Long,
        ): DomainResult<Review> = error("unused")

        override suspend fun reopenReview(
            id: String,
            expectedRevision: Long,
        ): DomainResult<Review> = error("unused")

        override suspend fun softDeleteReview(
            id: String,
            expectedRevision: Long,
        ): DomainResult<Review> = error("unused")

        override suspend fun completeReview(
            id: String,
            expectedRevision: Long,
            customerAcknowledged: Boolean,
        ): DomainResult<Review> = error("unused")
    }

    private class FixedResponsibilityRepository(
        private val items: List<Responsibility>,
    ) : ResponsibilityRepository {
        override fun observeResponsibilities(reviewId: String): Flow<List<Responsibility>> = flowOf(items)

        override suspend fun getResponsibilities(reviewId: String): List<Responsibility> = items

        override suspend fun saveResponsibility(
            responsibility: Responsibility,
            expectedRevision: Long,
        ): DomainResult<Review> = error("unused")

        override suspend fun removeResponsibility(
            responsibilityId: String,
            reviewId: String,
            expectedRevision: Long,
        ): DomainResult<Review> = error("unused")
    }

    private class NoOpSnapshotRepository(
        private val review: Review,
    ) : CalculationSnapshotRepository {
        override fun observeLatest(reviewId: String): Flow<CalculationSnapshot?> = flowOf(null)

        override suspend fun getLatest(reviewId: String): CalculationSnapshot? = null

        override suspend fun saveSnapshot(
            snapshot: CalculationSnapshot,
            expectedReviewRevision: Long,
            markSummaryFresh: Boolean,
        ): DomainResult<Review> = DomainResult.success(review.copy(revision = expectedReviewRevision + 1))

        override suspend fun isStale(
            review: Review,
            authoritativeCalculationVersion: String,
        ): Boolean = false
    }
}
