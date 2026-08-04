package com.familyriskreview.core.data

import com.familyriskreview.core.calculation.ResponsibilityCalculator
import com.familyriskreview.core.data.repository.CalculationSnapshotRepository
import com.familyriskreview.core.data.repository.ResponsibilityRepository
import com.familyriskreview.core.data.repository.ReviewReader
import com.familyriskreview.core.data.usecase.CalculateReviewSummaryUseCase
import com.familyriskreview.core.model.AnnualRateBps
import com.familyriskreview.core.model.AppLanguage
import com.familyriskreview.core.model.CalculationAssumptions
import com.familyriskreview.core.model.CalculationScenario
import com.familyriskreview.core.model.CalculationSnapshot
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
import com.familyriskreview.core.model.ScenarioKind
import com.familyriskreview.core.model.SyncState
import com.familyriskreview.core.model.TimingKind
import com.familyriskreview.core.model.result.DomainResult
import com.familyriskreview.core.model.service.Clock
import com.familyriskreview.core.model.service.IdGenerator
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import org.junit.Test

class CalculateReviewSummaryAssumptionAuditTest {
    private val education =
        Responsibility(
            id = "edu",
            reviewId = "r1",
            catalogue = ResponsibilityCatalogue.CHILD_HIGHER_EDUCATION,
            priority = ResponsibilityPriority.MUST_CONTINUE,
            isSelected = true,
            quantificationStatus = QuantificationStatus.QUANTIFIED,
            timing =
            ResponsibilityTiming(
                TimingKind.ONE_TIME_IN_YEARS,
                yearsUntilRequired = 10,
            ),
            currentAmount = MoneyAmount(1_000_000),
        )

    @Test
    fun explicitBaseScenarioAssumptions_persistedInSnapshot() = runBlocking {
        val reviewAssumptions =
            CalculationAssumptions.Default.copy(
                educationInflation = AnnualRateBps(800),
            )
        val baseScenarioAssumptions =
            CalculationAssumptions.Default.copy(
                educationInflation = AnnualRateBps(600),
            )
        val review = review(assumptions = reviewAssumptions)
        val useCase = useCase(review, listOf(education))
        val expectedAmount =
            ResponsibilityCalculator.indicativeAmount(education, baseScenarioAssumptions)
        val result =
            useCase(
                reviewId = "r1",
                expectedRevision = 1L,
                scenarios =
                listOf(
                    CalculationScenario(ScenarioKind.BASE, baseScenarioAssumptions),
                ),
            )
        assertThat(result).isInstanceOf(DomainResult.Success::class.java)
        val success = (result as DomainResult.Success).value
        assertThat(success.base.mustContinueTotalRupees).isEqualTo(expectedAmount)
        val persisted = CalculationAssumptions.fromJson(success.snapshot.assumptionsJson)
        assertThat(persisted.educationInflation).isEqualTo(AnnualRateBps(600))
        assertThat(success.snapshot.assumptionVersion).isEqualTo(baseScenarioAssumptions.version)
        assertThat(success.snapshot.assumptionsJson).isEqualTo(baseScenarioAssumptions.toJson())
        assertThat(success.snapshot.perResponsibilityJson).contains("\"indicativeAmountRupees\":$expectedAmount")
    }

    @Test
    fun emptyScenarios_usesReviewAssumptions() = runBlocking {
        val reviewAssumptions =
            CalculationAssumptions.Default.copy(
                educationInflation = AnnualRateBps(800),
            )
        val review = review(assumptions = reviewAssumptions)
        val useCase = useCase(review, listOf(education))
        val expectedAmount =
            ResponsibilityCalculator.indicativeAmount(education, reviewAssumptions)
        val result = useCase(reviewId = "r1", expectedRevision = 1L, scenarios = emptyList())
        val success = (result as DomainResult.Success).value
        assertThat(success.base.mustContinueTotalRupees).isEqualTo(expectedAmount)
        assertThat(CalculationAssumptions.fromJson(success.snapshot.assumptionsJson).educationInflation)
            .isEqualTo(AnnualRateBps(800))
    }

    private fun review(assumptions: CalculationAssumptions) = Review(
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
        assumptionVersion = assumptions.version,
        calculationVersion = ResponsibilityCalculator.CALCULATION_VERSION,
        summaryStale = true,
        assumptionsJson = assumptions.toJson(),
    )

    private fun useCase(
        review: Review,
        items: List<Responsibility>,
    ) = CalculateReviewSummaryUseCase(
        reviewReader = FixedReviewReader(review),
        responsibilityRepository = FixedResponsibilityRepository(items),
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

    private class FixedReviewReader(
        private val review: Review,
    ) : ReviewReader {
        override fun observeActiveReviews(): Flow<List<Review>> = flowOf(listOf(review))

        override fun observeByStatus(status: ReviewStatus): Flow<List<Review>> = flowOf(emptyList())

        override fun observeInProgress(): Flow<List<Review>> = flowOf(listOf(review))

        override fun observeReview(id: String): Flow<Review?> = flowOf(review)

        override suspend fun getReview(id: String): Review? = review
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
