package com.familyriskreview.core.data.repository

import com.familyriskreview.core.model.AdvisorReference
import com.familyriskreview.core.model.AppLanguage
import com.familyriskreview.core.model.CalculationAssumptions
import com.familyriskreview.core.model.CalculationScenario
import com.familyriskreview.core.model.CalculationSnapshot
import com.familyriskreview.core.model.HouseholdMember
import com.familyriskreview.core.model.Responsibility
import com.familyriskreview.core.model.Review
import com.familyriskreview.core.model.ReviewMode
import com.familyriskreview.core.model.ReviewStatus
import com.familyriskreview.core.model.ReviewStep
import com.familyriskreview.core.model.UserPreferences
import com.familyriskreview.core.model.result.DomainResult
import kotlinx.coroutines.flow.Flow

/**
 * Aggregate-oriented review persistence.
 *
 * Child writes go through transactional APIs that bump parent [Review.revision],
 * [Review.updatedAt], sync pending, and mark [Review.summaryStale].
 */
interface ReviewRepository {
    fun observeActiveReviews(): Flow<List<Review>>

    fun observeByStatus(status: ReviewStatus): Flow<List<Review>>

    fun observeInProgress(): Flow<List<Review>>

    fun observeReview(id: String): Flow<Review?>

    suspend fun getReview(id: String): Review?

    /**
     * Creates a review with collision-bounded review-number retry.
     * Does not use System.currentTimeMillis / UUID / SecureRandom directly —
     * those are injected via Clock / IdGenerator / ReviewNumberProvider.
     */
    suspend fun createReview(
        mode: ReviewMode,
        language: AppLanguage,
        assumptions: CalculationAssumptions = CalculationAssumptions.Default,
    ): DomainResult<Review>

    suspend fun updateReviewCas(
        review: Review,
        expectedRevision: Long,
    ): DomainResult<Review>

    suspend fun advanceStep(
        reviewId: String,
        expectedRevision: Long,
        step: ReviewStep,
    ): DomainResult<Review>

    suspend fun archiveReview(
        id: String,
        expectedRevision: Long,
    ): DomainResult<Review>

    suspend fun restoreReview(
        id: String,
        expectedRevision: Long,
    ): DomainResult<Review>

    suspend fun softDeleteReview(
        id: String,
        expectedRevision: Long,
    ): DomainResult<Review>

    suspend fun completeReview(
        id: String,
        expectedRevision: Long,
        customerAcknowledged: Boolean,
    ): DomainResult<Review>
}

interface HouseholdRepository {
    fun observeMembers(reviewId: String): Flow<List<HouseholdMember>>

    suspend fun getMembers(reviewId: String): List<HouseholdMember>

    /** Transactional save: validates + upserts member + bumps parent revision. */
    suspend fun saveMember(
        member: HouseholdMember,
        focusedIncomeContributorId: String?,
        expectedRevision: Long,
    ): DomainResult<Review>

    suspend fun removeMember(
        memberId: String,
        reviewId: String,
        expectedRevision: Long,
    ): DomainResult<Review>
}

interface ResponsibilityRepository {
    fun observeResponsibilities(reviewId: String): Flow<List<Responsibility>>

    suspend fun getResponsibilities(reviewId: String): List<Responsibility>

    suspend fun saveResponsibility(
        responsibility: Responsibility,
        expectedRevision: Long,
    ): DomainResult<Review>

    suspend fun removeResponsibility(
        responsibilityId: String,
        reviewId: String,
        expectedRevision: Long,
    ): DomainResult<Review>
}

interface AdvisorReferenceRepository {
    fun observe(reviewId: String): Flow<AdvisorReference?>

    suspend fun get(reviewId: String): AdvisorReference?

    suspend fun upsert(reference: AdvisorReference): DomainResult<Unit>

    suspend fun delete(reviewId: String): DomainResult<Unit>
}

interface CalculationSnapshotRepository {
    fun observeLatest(reviewId: String): Flow<CalculationSnapshot?>

    suspend fun getLatest(reviewId: String): CalculationSnapshot?

    suspend fun saveSnapshot(
        snapshot: CalculationSnapshot,
        expectedReviewRevision: Long,
        markSummaryFresh: Boolean = true,
    ): DomainResult<Review>

    suspend fun isStale(review: Review): Boolean
}

interface UserPreferencesRepository {
    val preferences: Flow<UserPreferences>

    suspend fun setDefaultLanguage(language: AppLanguage)

    suspend fun setReducedMotion(enabled: Boolean)

    suspend fun setEducationInflationBps(bps: Int)

    suspend fun setMarriageInflationBps(bps: Int)

    suspend fun setExpenseInflationBps(bps: Int)

    suspend fun setSyncEnabled(enabled: Boolean)
}

/** Bundled calculate+persist request. */
data class CalculateSummaryRequest(
    val reviewId: String,
    val expectedRevision: Long,
    val scenarios: List<CalculationScenario>,
)
