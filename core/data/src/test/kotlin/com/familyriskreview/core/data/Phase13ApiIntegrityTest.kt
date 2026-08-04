package com.familyriskreview.core.data

import com.familyriskreview.core.calculation.ResponsibilityCalculator
import com.familyriskreview.core.data.repository.CalculationSnapshotRepository
import com.familyriskreview.core.data.repository.ReviewMutationWriter
import com.familyriskreview.core.data.repository.ReviewReader
import com.familyriskreview.core.data.usecase.AdvanceReviewStepUseCase
import com.familyriskreview.core.data.usecase.CompleteReviewUseCase
import com.familyriskreview.core.model.AppLanguage
import com.familyriskreview.core.model.CalculationAssumptions
import com.familyriskreview.core.model.CalculationSnapshot
import com.familyriskreview.core.model.Review
import com.familyriskreview.core.model.ReviewMode
import com.familyriskreview.core.model.ReviewStatus
import com.familyriskreview.core.model.ReviewStep
import com.familyriskreview.core.model.SyncState
import com.familyriskreview.core.model.result.DomainError
import com.familyriskreview.core.model.result.DomainResult
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import org.junit.Test
import java.io.File

class Phase13ApiIntegrityTest {
    @Test
    fun reviewReader_hasNoMutationMethods() {
        val methods = ReviewReader::class.java.methods.map { it.name }.toSet()
        assertThat(methods).containsAtLeast(
            "observeActiveReviews",
            "observeByStatus",
            "observeInProgress",
            "observeReview",
            "getReview",
        )
        assertThat(methods).containsNoneOf(
            "completeReview",
            "advanceStep",
            "createReview",
            "archiveReview",
            "restoreReview",
            "reopenReview",
            "softDeleteReview",
            "updateReviewCas",
        )
    }

    @Test
    fun mutationWriter_isDeclaredInternalInSource() {
        val source =
            File("src/main/kotlin/com/familyriskreview/core/data/repository/Repositories.kt")
                .readText()
        assertThat(source).contains("internal interface ReviewMutationWriter")
        assertThat(source).doesNotContain("interface ReviewRepository")
        assertThat(source).doesNotContain("interface ReviewInternalWriter")
        assertThat(source).contains("suspend fun completeReview(")
        assertThat(source).contains("suspend fun advanceStep(")
    }

    @Test
    fun featureSources_doNotImportMutationWriter() {
        val featureRoot = File("../feature")
        if (!featureRoot.exists()) return
        val offenders =
            featureRoot
                .walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .filter { it.readText().contains("ReviewMutationWriter") }
                .map { it.path }
                .toList()
        assertThat(offenders).isEmpty()
    }

    @Test
    fun completeReviewUseCase_rejectsStaleSummary() = runBlocking {
        val review =
            Review(
                id = "r1",
                reviewNumber = "FRR-20260101-000001",
                mode = ReviewMode.QUICK,
                language = AppLanguage.ENGLISH,
                status = ReviewStatus.IN_PROGRESS,
                currentStep = ReviewStep.AWARENESS_SUMMARY,
                createdAt = Instant.fromEpochMilliseconds(1),
                updatedAt = Instant.fromEpochMilliseconds(1),
                syncState = SyncState.LOCAL_ONLY,
                revision = 2L,
                calculationVersion = ResponsibilityCalculator.CALCULATION_VERSION,
                summaryStale = true,
                assumptionsJson = CalculationAssumptions.Default.toJson(),
            )
        var completeCalled = false
        val useCase =
            CompleteReviewUseCase(
                reviewReader =
                object : ReviewReader {
                    override fun observeActiveReviews() = flowOf(listOf(review))

                    override fun observeByStatus(status: ReviewStatus) = flowOf(emptyList<Review>())

                    override fun observeInProgress() = flowOf(listOf(review))

                    override fun observeReview(id: String) = flowOf(review)

                    override suspend fun getReview(id: String) = review
                },
                reviewWriter =
                object : ReviewMutationWriter {
                    override suspend fun createReview(
                        mode: ReviewMode,
                        language: AppLanguage,
                        assumptions: CalculationAssumptions,
                    ) = error("unused")

                    override suspend fun advanceStep(
                        reviewId: String,
                        expectedRevision: Long,
                        step: ReviewStep,
                    ) = error("unused")

                    override suspend fun archiveReview(
                        id: String,
                        expectedRevision: Long,
                    ) = error("unused")

                    override suspend fun restoreReview(
                        id: String,
                        expectedRevision: Long,
                    ) = error("unused")

                    override suspend fun reopenReview(
                        id: String,
                        expectedRevision: Long,
                    ) = error("unused")

                    override suspend fun softDeleteReview(
                        id: String,
                        expectedRevision: Long,
                    ) = error("unused")

                    override suspend fun completeReview(
                        id: String,
                        expectedRevision: Long,
                        customerAcknowledged: Boolean,
                    ): DomainResult<Review> {
                        completeCalled = true
                        return DomainResult.success(review)
                    }
                },
                snapshotRepository =
                object : CalculationSnapshotRepository {
                    override fun observeLatest(reviewId: String): Flow<CalculationSnapshot?> = flowOf(null)

                    override suspend fun getLatest(reviewId: String): CalculationSnapshot? = null

                    override suspend fun saveSnapshot(
                        snapshot: CalculationSnapshot,
                        expectedReviewRevision: Long,
                        markSummaryFresh: Boolean,
                    ) = error("unused")

                    override suspend fun isStale(
                        review: Review,
                        authoritativeCalculationVersion: String,
                    ): Boolean = true
                },
            )
        val result = useCase("r1", expectedRevision = 2L, customerAcknowledged = true)
        assertThat(result).isInstanceOf(DomainResult.Failure::class.java)
        assertThat(completeCalled).isFalse()
        val error = (result as DomainResult.Failure).error
        assertThat(error).isInstanceOf(DomainError.Validation::class.java)
    }

    @Test
    fun advanceReviewStepUseCase_isPublicProgressionEntry() {
        assertThat(AdvanceReviewStepUseCase::class.java).isNotNull()
        val ctor = AdvanceReviewStepUseCase::class.java.constructors.first()
        val paramTypes = ctor.parameterTypes.map { it.simpleName }
        assertThat(paramTypes).contains("ReviewMutationWriter")
        assertThat(paramTypes).contains("ReviewReader")
    }
}
