package com.familyriskreview.core.data.repository

import android.database.sqlite.SQLiteConstraintException
import androidx.room.withTransaction
import com.familyriskreview.core.calculation.ResponsibilityCalculator
import com.familyriskreview.core.database.FamilyRiskReviewDatabase
import com.familyriskreview.core.database.dao.AdvisorReferenceDao
import com.familyriskreview.core.database.dao.CalculationSnapshotDao
import com.familyriskreview.core.database.dao.HouseholdMemberDao
import com.familyriskreview.core.database.dao.ResponsibilityDao
import com.familyriskreview.core.database.dao.ReviewDao
import com.familyriskreview.core.database.mapper.toDomain
import com.familyriskreview.core.database.mapper.toEntity
import com.familyriskreview.core.model.AdvisorReference
import com.familyriskreview.core.model.AppLanguage
import com.familyriskreview.core.model.CalculationAssumptions
import com.familyriskreview.core.model.CalculationSnapshot
import com.familyriskreview.core.model.FocusUpdate
import com.familyriskreview.core.model.HouseholdMember
import com.familyriskreview.core.model.Responsibility
import com.familyriskreview.core.model.Review
import com.familyriskreview.core.model.ReviewMode
import com.familyriskreview.core.model.ReviewStatus
import com.familyriskreview.core.model.ReviewStep
import com.familyriskreview.core.model.SyncState
import com.familyriskreview.core.model.lifecycle.ReviewEditability
import com.familyriskreview.core.model.lifecycle.ReviewLifecycle
import com.familyriskreview.core.model.result.DomainError
import com.familyriskreview.core.model.result.DomainResult
import com.familyriskreview.core.model.service.Clock
import com.familyriskreview.core.model.service.IdGenerator
import com.familyriskreview.core.model.service.ReviewNumberProvider
import com.familyriskreview.core.model.validation.HouseholdRules
import com.familyriskreview.core.sync.SyncClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultReviewRepository
@Inject
constructor(
    private val db: FamilyRiskReviewDatabase,
    private val reviewDao: ReviewDao,
    private val syncClient: SyncClient,
    private val clock: Clock,
    private val idGenerator: IdGenerator,
    private val reviewNumberProvider: ReviewNumberProvider,
) : ReviewRepository {
    override fun observeActiveReviews(): Flow<List<Review>> = reviewDao.observeAllActive().map { list -> list.map { it.toDomain() } }

    override fun observeByStatus(status: ReviewStatus): Flow<List<Review>> = reviewDao.observeByStatus(status).map { list -> list.map { it.toDomain() } }

    override fun observeInProgress(): Flow<List<Review>> = reviewDao.observeInProgress().map { list -> list.map { it.toDomain() } }

    override fun observeReview(id: String): Flow<Review?> = reviewDao.observeById(id).map { it?.toDomain() }

    override suspend fun getReview(id: String): Review? = reviewDao.getById(id)?.toDomain()

    override suspend fun createReview(
        mode: ReviewMode,
        language: AppLanguage,
        assumptions: CalculationAssumptions,
    ): DomainResult<Review> {
        val now = clock.now()
        var attempt = 0
        while (attempt < MAX_REVIEW_NUMBER_ATTEMPTS) {
            attempt++
            val reviewNumber = reviewNumberProvider.generate(now)
            if (reviewDao.countByReviewNumber(reviewNumber) > 0) continue
            val review =
                Review(
                    id = idGenerator.newId(),
                    reviewNumber = reviewNumber,
                    mode = mode,
                    language = language,
                    status = ReviewStatus.IN_PROGRESS,
                    currentStep = ReviewStep.HOUSEHOLD_SUPPORT_MAP,
                    createdAt = now,
                    updatedAt = now,
                    assumptionVersion = assumptions.version,
                    calculationVersion = ResponsibilityCalculator.CALCULATION_VERSION,
                    syncState = SyncState.LOCAL_ONLY,
                    revision = 1L,
                    calculationInputRevision = 1L,
                    summaryStale = true,
                    assumptionsJson = assumptions.toJson(),
                )
            try {
                reviewDao.insert(review.toEntity())
                syncClient.enqueueUpsert(review.id)
                return DomainResult.success(review)
            } catch (ex: SQLiteConstraintException) {
                when (classifyConstraint(ex)) {
                    ConstraintKind.REVIEW_NUMBER -> Unit // retry with new number+id
                    ConstraintKind.PRIMARY_KEY -> Unit // retry with new id
                    ConstraintKind.OTHER ->
                        return DomainResult.failure(
                            DomainError.Persistence(
                                "Unexpected constraint while creating review: ${ex.message}",
                            ),
                        )
                }
            }
        }
        return DomainResult.failure(
            DomainError.CollisionExhausted(
                "Unable to allocate a unique review number after $MAX_REVIEW_NUMBER_ATTEMPTS attempts",
            ),
        )
    }

    override suspend fun updateReviewCas(
        review: Review,
        expectedRevision: Long,
    ): DomainResult<Review> {
        val now = clock.now()
        val newRevision = expectedRevision + 1
        val updated =
            review.copy(
                updatedAt = now,
                revision = newRevision,
                syncState = SyncState.PENDING,
            )
        val rows =
            reviewDao.updateCas(
                id = updated.id,
                expectedRevision = expectedRevision,
                newRevision = newRevision,
                reviewNumber = updated.reviewNumber,
                mode = updated.mode,
                language = updated.language,
                status = updated.status,
                currentStep = updated.currentStep,
                updatedAtEpochMs = now.toEpochMilliseconds(),
                completedAtEpochMs = updated.completedAt?.toEpochMilliseconds(),
                focusedIncomeContributorId = updated.focusedIncomeContributorId,
                assumptionVersion = updated.assumptionVersion,
                calculationVersion = updated.calculationVersion,
                syncState = updated.syncState,
                calculationInputRevision = updated.calculationInputRevision,
                customerAcknowledged = updated.customerAcknowledged,
                statusBeforeArchive = updated.statusBeforeArchive,
                summaryStale = updated.summaryStale,
                assumptionsJson = updated.assumptionsJson,
            )
        return casResult(updated.id, expectedRevision, rows, updated)
    }

    override suspend fun advanceStep(
        reviewId: String,
        expectedRevision: Long,
        step: ReviewStep,
    ): DomainResult<Review> {
        val existing =
            reviewDao.getById(reviewId)?.toDomain()
                ?: return DomainResult.failure(DomainError.NotFound("Review $reviewId not found"))
        if (existing.status != ReviewStatus.IN_PROGRESS) {
            return DomainResult.failure(
                DomainError.IllegalState("Only in-progress reviews can change step"),
            )
        }
        val now = clock.now()
        val newRevision = expectedRevision + 1
        val rows =
            reviewDao.updateStepCas(
                id = reviewId,
                expectedRevision = expectedRevision,
                newRevision = newRevision,
                currentStep = step,
                updatedAtEpochMs = now.toEpochMilliseconds(),
                syncState = SyncState.PENDING,
            )
        val updated =
            existing.copy(
                currentStep = step,
                updatedAt = now,
                revision = newRevision,
                syncState = SyncState.PENDING,
            )
        return casResult(reviewId, expectedRevision, rows, updated)
    }

    override suspend fun archiveReview(
        id: String,
        expectedRevision: Long,
    ): DomainResult<Review> {
        val existing =
            reviewDao.getById(id)?.toDomain()
                ?: return DomainResult.failure(DomainError.NotFound("Review $id not found"))
        val transition = ReviewLifecycle.transition(existing.status, ReviewStatus.ARCHIVED)
        if (transition is DomainResult.Failure) return transition
        val now = clock.now()
        val newRevision = expectedRevision + 1
        val prior = existing.status
        val rows =
            reviewDao.updateStatusCas(
                id = id,
                expectedRevision = expectedRevision,
                newRevision = newRevision,
                status = ReviewStatus.ARCHIVED,
                statusBeforeArchive = prior,
                updatedAtEpochMs = now.toEpochMilliseconds(),
                syncState = SyncState.PENDING,
                completedAtEpochMs = existing.completedAt?.toEpochMilliseconds(),
            )
        val updated =
            existing.copy(
                status = ReviewStatus.ARCHIVED,
                statusBeforeArchive = prior,
                updatedAt = now,
                revision = newRevision,
                syncState = SyncState.PENDING,
            )
        return casResult(id, expectedRevision, rows, updated)
    }

    override suspend fun restoreReview(
        id: String,
        expectedRevision: Long,
    ): DomainResult<Review> {
        val existing =
            reviewDao.getById(id)?.toDomain()
                ?: return DomainResult.failure(DomainError.NotFound("Review $id not found"))
        if (existing.status != ReviewStatus.ARCHIVED) {
            return DomainResult.failure(
                DomainError.Transition("Only archived reviews can be restored"),
            )
        }
        val target = ReviewLifecycle.restoreTarget(existing.statusBeforeArchive)
        val transition = ReviewLifecycle.transition(existing.status, target)
        if (transition is DomainResult.Failure) return transition
        val now = clock.now()
        val newRevision = expectedRevision + 1
        val completedAt =
            if (target == ReviewStatus.COMPLETED) existing.completedAt else null
        val rows =
            reviewDao.updateStatusCas(
                id = id,
                expectedRevision = expectedRevision,
                newRevision = newRevision,
                status = target,
                statusBeforeArchive = null,
                updatedAtEpochMs = now.toEpochMilliseconds(),
                syncState = SyncState.PENDING,
                completedAtEpochMs = completedAt?.toEpochMilliseconds(),
            )
        val updated =
            existing.copy(
                status = target,
                statusBeforeArchive = null,
                completedAt = completedAt,
                updatedAt = now,
                revision = newRevision,
                syncState = SyncState.PENDING,
            )
        return casResult(id, expectedRevision, rows, updated)
    }

    override suspend fun reopenReview(
        id: String,
        expectedRevision: Long,
    ): DomainResult<Review> {
        val existing =
            reviewDao.getById(id)?.toDomain()
                ?: return DomainResult.failure(DomainError.NotFound("Review $id not found"))
        if (existing.status != ReviewStatus.COMPLETED) {
            return DomainResult.failure(
                DomainError.Transition("Only completed reviews can be reopened"),
            )
        }
        val transition = ReviewLifecycle.transition(existing.status, ReviewStatus.IN_PROGRESS)
        if (transition is DomainResult.Failure) return transition
        val now = clock.now()
        val newRevision = expectedRevision + 1
        // Policy: clear completedAt on reopen; preserve historical calculation snapshots.
        val rows =
            reviewDao.updateStatusCas(
                id = id,
                expectedRevision = expectedRevision,
                newRevision = newRevision,
                status = ReviewStatus.IN_PROGRESS,
                statusBeforeArchive = existing.statusBeforeArchive,
                updatedAtEpochMs = now.toEpochMilliseconds(),
                syncState = SyncState.PENDING,
                completedAtEpochMs = null,
            )
        val updated =
            existing.copy(
                status = ReviewStatus.IN_PROGRESS,
                completedAt = null,
                updatedAt = now,
                revision = newRevision,
                syncState = SyncState.PENDING,
            )
        return casResult(id, expectedRevision, rows, updated)
    }

    override suspend fun softDeleteReview(
        id: String,
        expectedRevision: Long,
    ): DomainResult<Review> {
        val existing =
            reviewDao.getById(id)?.toDomain()
                ?: return DomainResult.failure(DomainError.NotFound("Review $id not found"))
        val transition = ReviewLifecycle.transition(existing.status, ReviewStatus.DELETED)
        if (transition is DomainResult.Failure) return transition
        val now = clock.now()
        val newRevision = expectedRevision + 1
        val rows =
            reviewDao.updateStatusCas(
                id = id,
                expectedRevision = expectedRevision,
                newRevision = newRevision,
                status = ReviewStatus.DELETED,
                statusBeforeArchive = existing.statusBeforeArchive,
                updatedAtEpochMs = now.toEpochMilliseconds(),
                syncState = SyncState.PENDING,
                completedAtEpochMs = existing.completedAt?.toEpochMilliseconds(),
            )
        if (rows != 1) {
            return conflict(id, expectedRevision)
        }
        syncClient.enqueueDelete(id)
        return DomainResult.success(
            existing.copy(
                status = ReviewStatus.DELETED,
                updatedAt = now,
                revision = newRevision,
                syncState = SyncState.PENDING,
            ),
        )
    }

    override suspend fun completeReview(
        id: String,
        expectedRevision: Long,
        customerAcknowledged: Boolean,
    ): DomainResult<Review> {
        val existing =
            reviewDao.getById(id)?.toDomain()
                ?: return DomainResult.failure(DomainError.NotFound("Review $id not found"))
        val transition = ReviewLifecycle.transition(existing.status, ReviewStatus.COMPLETED)
        if (transition is DomainResult.Failure) return transition
        val now = clock.now()
        val newRevision = expectedRevision + 1
        val rows =
            reviewDao.updateAfterCalculationCas(
                id = id,
                expectedRevision = expectedRevision,
                newRevision = newRevision,
                summaryStale = existing.summaryStale,
                updatedAtEpochMs = now.toEpochMilliseconds(),
                syncState = SyncState.PENDING,
                completedAtEpochMs = now.toEpochMilliseconds(),
                status = ReviewStatus.COMPLETED,
                customerAcknowledged = customerAcknowledged,
            )
        val updated =
            existing.copy(
                status = ReviewStatus.COMPLETED,
                completedAt = now,
                customerAcknowledged = customerAcknowledged,
                updatedAt = now,
                revision = newRevision,
                syncState = SyncState.PENDING,
            )
        return casResult(id, expectedRevision, rows, updated)
    }

    private suspend fun casResult(
        id: String,
        expectedRevision: Long,
        rows: Int,
        updated: Review,
    ): DomainResult<Review> {
        if (rows != 1) return conflict(id, expectedRevision)
        syncClient.enqueueUpsert(id)
        return DomainResult.success(updated)
    }

    private suspend fun conflict(
        id: String,
        expectedRevision: Long,
    ): DomainResult.Failure = DomainResult.Failure(
        DomainError.Conflict(
            message = "Revision conflict for review $id (expected $expectedRevision)",
            currentRevision = reviewDao.getById(id)?.revision,
        ),
    )

    companion object {
        const val MAX_REVIEW_NUMBER_ATTEMPTS: Int = 8

        internal fun classifyConstraint(ex: SQLiteConstraintException): ConstraintKind {
            val msg = ex.message.orEmpty()
            return when {
                "reviewNumber" in msg -> ConstraintKind.REVIEW_NUMBER
                "UNIQUE constraint failed: reviews.reviewNumber" in msg -> ConstraintKind.REVIEW_NUMBER
                "reviews.id" in msg || "PRIMARY KEY" in msg.uppercase() -> ConstraintKind.PRIMARY_KEY
                else -> ConstraintKind.OTHER
            }
        }
    }

    internal enum class ConstraintKind { REVIEW_NUMBER, PRIMARY_KEY, OTHER }
}

@Singleton
class DefaultHouseholdRepository
@Inject
constructor(
    private val db: FamilyRiskReviewDatabase,
    private val dao: HouseholdMemberDao,
    private val reviewDao: ReviewDao,
    private val syncClient: SyncClient,
    private val clock: Clock,
) : HouseholdRepository {
    override fun observeMembers(reviewId: String): Flow<List<HouseholdMember>> = dao.observeForReview(reviewId).map { list -> list.map { it.toDomain() } }

    override suspend fun getMembers(reviewId: String): List<HouseholdMember> = dao.getForReview(reviewId).map { it.toDomain() }

    override suspend fun saveMember(
        member: HouseholdMember,
        expectedRevision: Long,
        focusUpdate: FocusUpdate,
    ): DomainResult<Review> = try {
        val updated =
            db.withTransaction {
                val review =
                    reviewDao.getById(member.reviewId)?.toDomain()
                        ?: throw AbortDomainException(
                            DomainError.NotFound("Review ${member.reviewId} not found"),
                        )
                ReviewEditability.requireEditable(review.status).throwIfFailure()
                val existing = dao.getForReview(member.reviewId).map { it.toDomain() }
                val projected = existing.filter { it.id != member.id } + member
                val effectiveFocus =
                    when (focusUpdate) {
                        FocusUpdate.Unchanged -> review.focusedIncomeContributorId
                        FocusUpdate.Clear -> null
                        is FocusUpdate.Set -> focusUpdate.memberId
                    }
                if (focusUpdate is FocusUpdate.Set) {
                    val focusMember = projected.find { it.id == focusUpdate.memberId }
                    if (focusMember == null) {
                        throw AbortDomainException(
                            DomainError.Validation(
                                listOf(
                                    com.familyriskreview.core.model.result.ValidationIssue(
                                        code = "HOUSEHOLD_FOCUS_MISSING",
                                        message =
                                        "Focused income contributor must belong to this review",
                                        field = "focusedIncomeContributorId",
                                    ),
                                ),
                            ),
                        )
                    }
                }
                val validation =
                    HouseholdRules.validateMember(member, existing, effectiveFocus) +
                        HouseholdRules.validateHousehold(projected, effectiveFocus)
                if (!validation.isValid) {
                    throw AbortDomainException(DomainError.Validation(validation.errors))
                }
                dao.upsert(member.toEntity())
                touchParentOrThrow(review, expectedRevision, effectiveFocus)
            }
        syncClient.enqueueUpsert(updated.id)
        DomainResult.success(updated)
    } catch (ex: AggregateCasConflictException) {
        DomainResult.failure(
            DomainError.Conflict(
                message = ex.message ?: "Revision conflict",
                currentRevision = ex.currentRevision,
            ),
        )
    } catch (ex: AbortDomainException) {
        DomainResult.failure(ex.error)
    }

    override suspend fun removeMember(
        memberId: String,
        reviewId: String,
        expectedRevision: Long,
    ): DomainResult<Review> = try {
        val updated =
            db.withTransaction {
                val review =
                    reviewDao.getById(reviewId)?.toDomain()
                        ?: throw AbortDomainException(
                            DomainError.NotFound("Review $reviewId not found"),
                        )
                ReviewEditability.requireEditable(review.status).throwIfFailure()
                dao.deleteById(memberId)
                val focus =
                    if (review.focusedIncomeContributorId == memberId) {
                        null
                    } else {
                        review.focusedIncomeContributorId
                    }
                val remaining = dao.getForReview(reviewId).map { it.toDomain() }
                val validation = HouseholdRules.validateHousehold(remaining, focus)
                if (!validation.isValid) {
                    throw AbortDomainException(DomainError.Validation(validation.errors))
                }
                touchParentOrThrow(review, expectedRevision, focus)
            }
        syncClient.enqueueUpsert(updated.id)
        DomainResult.success(updated)
    } catch (ex: AggregateCasConflictException) {
        DomainResult.failure(
            DomainError.Conflict(
                message = ex.message ?: "Revision conflict",
                currentRevision = ex.currentRevision,
            ),
        )
    } catch (ex: AbortDomainException) {
        DomainResult.failure(ex.error)
    }

    private suspend fun touchParentOrThrow(
        review: Review,
        expectedRevision: Long,
        focusedIncomeContributorId: String?,
    ): Review {
        val now = clock.now()
        val newRevision = expectedRevision + 1
        val newInputRevision = review.calculationInputRevision + 1
        val rows =
            reviewDao.touchAggregateCas(
                id = review.id,
                expectedRevision = expectedRevision,
                newRevision = newRevision,
                newInputRevision = newInputRevision,
                focusedIncomeContributorId = focusedIncomeContributorId,
                updatedAtEpochMs = now.toEpochMilliseconds(),
                syncState = SyncState.PENDING,
            )
        if (rows != 1) {
            throw AggregateCasConflictException(
                reviewId = review.id,
                expectedRevision = expectedRevision,
                currentRevision = reviewDao.getById(review.id)?.revision,
            )
        }
        return review.copy(
            focusedIncomeContributorId = focusedIncomeContributorId,
            summaryStale = true,
            updatedAt = now,
            revision = newRevision,
            calculationInputRevision = newInputRevision,
            syncState = SyncState.PENDING,
        )
    }
}

@Singleton
class DefaultResponsibilityRepository
@Inject
constructor(
    private val db: FamilyRiskReviewDatabase,
    private val dao: ResponsibilityDao,
    private val reviewDao: ReviewDao,
    private val syncClient: SyncClient,
    private val clock: Clock,
) : ResponsibilityRepository {
    override fun observeResponsibilities(reviewId: String): Flow<List<Responsibility>> = dao.observeForReview(reviewId).map { list -> list.map { it.toDomain() } }

    override suspend fun getResponsibilities(reviewId: String): List<Responsibility> = dao.getForReview(reviewId).map { it.toDomain() }

    override suspend fun saveResponsibility(
        responsibility: Responsibility,
        expectedRevision: Long,
    ): DomainResult<Review> = try {
        val updated =
            db.withTransaction {
                val review =
                    reviewDao.getById(responsibility.reviewId)?.toDomain()
                        ?: throw AbortDomainException(
                            DomainError.NotFound(
                                "Review ${responsibility.reviewId} not found",
                            ),
                        )
                ReviewEditability.requireEditable(review.status).throwIfFailure()
                dao.upsert(responsibility.toEntity())
                touchParentOrThrow(review, expectedRevision)
            }
        syncClient.enqueueUpsert(updated.id)
        DomainResult.success(updated)
    } catch (ex: AggregateCasConflictException) {
        DomainResult.failure(
            DomainError.Conflict(
                message = ex.message ?: "Revision conflict",
                currentRevision = ex.currentRevision,
            ),
        )
    } catch (ex: AbortDomainException) {
        DomainResult.failure(ex.error)
    }

    override suspend fun removeResponsibility(
        responsibilityId: String,
        reviewId: String,
        expectedRevision: Long,
    ): DomainResult<Review> = try {
        val updated =
            db.withTransaction {
                val review =
                    reviewDao.getById(reviewId)?.toDomain()
                        ?: throw AbortDomainException(
                            DomainError.NotFound("Review $reviewId not found"),
                        )
                ReviewEditability.requireEditable(review.status).throwIfFailure()
                dao.deleteById(responsibilityId)
                touchParentOrThrow(review, expectedRevision)
            }
        syncClient.enqueueUpsert(updated.id)
        DomainResult.success(updated)
    } catch (ex: AggregateCasConflictException) {
        DomainResult.failure(
            DomainError.Conflict(
                message = ex.message ?: "Revision conflict",
                currentRevision = ex.currentRevision,
            ),
        )
    } catch (ex: AbortDomainException) {
        DomainResult.failure(ex.error)
    }

    private suspend fun touchParentOrThrow(
        review: Review,
        expectedRevision: Long,
    ): Review {
        val now = clock.now()
        val newRevision = expectedRevision + 1
        val newInputRevision = review.calculationInputRevision + 1
        val rows =
            reviewDao.touchAggregateCas(
                id = review.id,
                expectedRevision = expectedRevision,
                newRevision = newRevision,
                newInputRevision = newInputRevision,
                focusedIncomeContributorId = review.focusedIncomeContributorId,
                updatedAtEpochMs = now.toEpochMilliseconds(),
                syncState = SyncState.PENDING,
            )
        if (rows != 1) {
            throw AggregateCasConflictException(
                reviewId = review.id,
                expectedRevision = expectedRevision,
                currentRevision = reviewDao.getById(review.id)?.revision,
            )
        }
        return review.copy(
            summaryStale = true,
            updatedAt = now,
            revision = newRevision,
            calculationInputRevision = newInputRevision,
            syncState = SyncState.PENDING,
        )
    }
}

@Singleton
class DefaultAdvisorReferenceRepository
@Inject
constructor(
    private val dao: AdvisorReferenceDao,
) : AdvisorReferenceRepository {
    override fun observe(reviewId: String): Flow<AdvisorReference?> = dao.observe(reviewId).map { it?.toDomain() }

    override suspend fun get(reviewId: String): AdvisorReference? = dao.get(reviewId)?.toDomain()

    override suspend fun upsert(reference: AdvisorReference): DomainResult<Unit> {
        dao.upsert(reference.toEntity())
        return DomainResult.success(Unit)
    }

    override suspend fun delete(reviewId: String): DomainResult<Unit> {
        dao.delete(reviewId)
        return DomainResult.success(Unit)
    }
}

@Singleton
class DefaultCalculationSnapshotRepository
@Inject
constructor(
    private val db: FamilyRiskReviewDatabase,
    private val snapshotDao: CalculationSnapshotDao,
    private val reviewDao: ReviewDao,
    private val syncClient: SyncClient,
    private val clock: Clock,
) : CalculationSnapshotRepository {
    override fun observeLatest(reviewId: String): Flow<CalculationSnapshot?> = snapshotDao.observeLatest(reviewId).map { it?.toDomain() }

    override suspend fun getLatest(reviewId: String): CalculationSnapshot? = snapshotDao.getLatest(reviewId)?.toDomain()

    override suspend fun saveSnapshot(
        snapshot: CalculationSnapshot,
        expectedReviewRevision: Long,
        markSummaryFresh: Boolean,
    ): DomainResult<Review> = try {
        val updated =
            db.withTransaction {
                val review =
                    reviewDao.getById(snapshot.reviewId)?.toDomain()
                        ?: throw AbortDomainException(
                            DomainError.NotFound("Review ${snapshot.reviewId} not found"),
                        )
                ReviewEditability.requireCalculable(review.status).throwIfFailure()
                val now = clock.now()
                val newRevision = expectedReviewRevision + 1
                val stored =
                    snapshot.copy(
                        reviewRevision = newRevision,
                        calculationInputRevision = review.calculationInputRevision,
                    )
                snapshotDao.upsert(stored.toEntity())
                val rows =
                    reviewDao.updateAfterCalculationCas(
                        id = review.id,
                        expectedRevision = expectedReviewRevision,
                        newRevision = newRevision,
                        summaryStale = !markSummaryFresh,
                        updatedAtEpochMs = now.toEpochMilliseconds(),
                        syncState = SyncState.PENDING,
                        completedAtEpochMs = review.completedAt?.toEpochMilliseconds(),
                        status = review.status,
                        customerAcknowledged = review.customerAcknowledged,
                    )
                if (rows != 1) {
                    throw AggregateCasConflictException(
                        reviewId = review.id,
                        expectedRevision = expectedReviewRevision,
                        currentRevision = reviewDao.getById(review.id)?.revision,
                    )
                }
                review.copy(
                    summaryStale = !markSummaryFresh,
                    updatedAt = now,
                    revision = newRevision,
                    syncState = SyncState.PENDING,
                )
            }
        syncClient.enqueueUpsert(updated.id)
        DomainResult.success(updated)
    } catch (ex: AggregateCasConflictException) {
        DomainResult.failure(
            DomainError.Conflict(
                message = ex.message ?: "Revision conflict",
                currentRevision = ex.currentRevision,
            ),
        )
    } catch (ex: AbortDomainException) {
        DomainResult.failure(ex.error)
    }

    override suspend fun isStale(
        review: Review,
        authoritativeCalculationVersion: String,
    ): Boolean {
        if (review.summaryStale) return true
        val latest = snapshotDao.getLatest(review.id)?.toDomain() ?: return true
        if (latest.calculationInputRevision != review.calculationInputRevision) return true
        if (latest.assumptionVersion != review.assumptionVersion) return true
        if (latest.calculationVersion != review.calculationVersion) return true
        return latest.calculationVersion != authoritativeCalculationVersion
    }
}

/** Domain abort inside a transaction before/without relying on Room rollback for validation. */
internal class AbortDomainException(
    val error: DomainError,
) : RuntimeException(error.toString())

private fun DomainResult<Unit>.throwIfFailure() {
    if (this is DomainResult.Failure) throw AbortDomainException(error)
}
