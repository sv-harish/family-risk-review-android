package com.familyriskreview.core.data.repository

import com.familyriskreview.core.model.AdvisorReference
import com.familyriskreview.core.model.AppLanguage
import com.familyriskreview.core.model.CalculationAssumptions
import com.familyriskreview.core.model.CalculationScenario
import com.familyriskreview.core.model.CalculationSnapshot
import com.familyriskreview.core.model.FocusUpdate
import com.familyriskreview.core.model.HouseholdMember
import com.familyriskreview.core.model.Responsibility
import com.familyriskreview.core.model.Review
import com.familyriskreview.core.model.ReviewMode
import com.familyriskreview.core.model.ReviewStatus
import com.familyriskreview.core.model.ReviewStep
import com.familyriskreview.core.model.UserPreferences
import com.familyriskreview.core.model.result.DomainResult
import kotlinx.coroutines.flow.Flow

interface ReviewRepository {
    fun observeActiveReviews(): Flow<List<Review>>

    fun observeByStatus(status: ReviewStatus): Flow<List<Review>>

    fun observeInProgress(): Flow<List<Review>>

    fun observeReview(id: String): Flow<Review?>

    suspend fun getReview(id: String): Review?

    suspend fun createReview(
        mode: ReviewMode,
        language: AppLanguage,
        assumptions: CalculationAssumptions = CalculationAssumptions.Default,
    ): DomainResult<Review>

    suspend fun archiveReview(
        id: String,
        expectedRevision: Long,
    ): DomainResult<Review>

    suspend fun restoreReview(
        id: String,
        expectedRevision: Long,
    ): DomainResult<Review>

    /** COMPLETED → IN_PROGRESS. Clears completedAt; preserves calculation snapshots. */
    suspend fun reopenReview(
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

/**
 * Internal write surface for use cases in `:core:data` only.
 * Not exposed on the feature-facing [ReviewRepository] API so callers cannot
 * bypass progression gates or arbitrarily mutate protected aggregate fields.
 */
interface ReviewInternalWriter {
    suspend fun advanceStep(
        reviewId: String,
        expectedRevision: Long,
        step: ReviewStep,
    ): DomainResult<Review>
}

interface HouseholdRepository {
    fun observeMembers(reviewId: String): Flow<List<HouseholdMember>>

    suspend fun getMembers(reviewId: String): List<HouseholdMember>

    suspend fun saveMember(
        member: HouseholdMember,
        expectedRevision: Long,
        focusUpdate: FocusUpdate = FocusUpdate.Unchanged,
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

/**
 * Advisor references are intentionally **local-only** (ADR-017).
 * They do not bump review revision or enqueue SyncClient intents.
 */
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

    suspend fun isStale(
        review: Review,
        authoritativeCalculationVersion: String,
    ): Boolean
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

data class CalculateSummaryRequest(
    val reviewId: String,
    val expectedRevision: Long,
    val scenarios: List<CalculationScenario>,
)
