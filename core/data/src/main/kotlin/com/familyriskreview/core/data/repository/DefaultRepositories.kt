package com.familyriskreview.core.data.repository

import com.familyriskreview.core.database.dao.AdvisorReferenceDao
import com.familyriskreview.core.database.dao.HouseholdMemberDao
import com.familyriskreview.core.database.dao.ResponsibilityDao
import com.familyriskreview.core.database.dao.ReviewDao
import com.familyriskreview.core.database.mapper.toDomain
import com.familyriskreview.core.database.mapper.toEntity
import com.familyriskreview.core.model.AdvisorReference
import com.familyriskreview.core.model.AppLanguage
import com.familyriskreview.core.model.CalculationAssumptions
import com.familyriskreview.core.model.HouseholdMember
import com.familyriskreview.core.model.Responsibility
import com.familyriskreview.core.model.Review
import com.familyriskreview.core.model.ReviewMode
import com.familyriskreview.core.model.ReviewNumberGenerator
import com.familyriskreview.core.model.ReviewStatus
import com.familyriskreview.core.model.ReviewStep
import com.familyriskreview.core.model.SyncState
import com.familyriskreview.core.sync.SyncClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultReviewRepository
@Inject
constructor(
    private val reviewDao: ReviewDao,
    private val syncClient: SyncClient,
) : ReviewRepository {
    override fun observeActiveReviews(): Flow<List<Review>> = reviewDao.observeAllActive().map { list -> list.map { it.toDomain() } }

    override fun observeByStatus(status: ReviewStatus): Flow<List<Review>> = reviewDao.observeByStatus(status).map { list -> list.map { it.toDomain() } }

    override fun observeReview(id: String): Flow<Review?> = reviewDao.observeById(id).map { it?.toDomain() }

    override suspend fun getReview(id: String): Review? = reviewDao.getById(id)?.toDomain()

    override suspend fun createReview(
        mode: ReviewMode,
        language: AppLanguage,
    ): Review {
        val nowMs = System.currentTimeMillis()
        val now = Instant.fromEpochMilliseconds(nowMs)
        val review =
            Review(
                id = UUID.randomUUID().toString(),
                reviewNumber = ReviewNumberGenerator.generate(epochMs = nowMs),
                mode = mode,
                language = language,
                status = ReviewStatus.IN_PROGRESS,
                currentStep = ReviewStep.HOUSEHOLD_SUPPORT_MAP,
                createdAt = now,
                updatedAt = now,
                assumptionVersion = CalculationAssumptions.CURRENT_VERSION,
                calculationVersion = "1.1.0",
                syncState = SyncState.LOCAL_ONLY,
            )
        reviewDao.insert(review.toEntity())
        syncClient.enqueueUpsert(review.id)
        return review
    }

    override suspend fun updateReview(review: Review) {
        val now = Instant.fromEpochMilliseconds(System.currentTimeMillis())
        val updated =
            review.copy(
                updatedAt = now,
                revision = review.revision + 1,
                syncState = SyncState.PENDING,
            )
        reviewDao.update(updated.toEntity())
        syncClient.enqueueUpsert(updated.id)
    }

    override suspend fun archiveReview(id: String) {
        reviewDao.archive(id, System.currentTimeMillis())
        syncClient.enqueueUpsert(id)
    }

    override suspend fun softDeleteReview(id: String) {
        reviewDao.softDelete(id, System.currentTimeMillis())
        syncClient.enqueueDelete(id)
    }
}

@Singleton
class DefaultHouseholdRepository
@Inject
constructor(
    private val dao: HouseholdMemberDao,
) : HouseholdRepository {
    override fun observeMembers(reviewId: String): Flow<List<HouseholdMember>> = dao.observeForReview(reviewId).map { list -> list.map { it.toDomain() } }

    override suspend fun getMembers(reviewId: String): List<HouseholdMember> = dao.getForReview(reviewId).map { it.toDomain() }

    override suspend fun upsertMember(member: HouseholdMember) {
        dao.upsert(member.toEntity())
    }

    override suspend fun deleteMember(id: String) {
        dao.deleteById(id)
    }
}

@Singleton
class DefaultResponsibilityRepository
@Inject
constructor(
    private val dao: ResponsibilityDao,
) : ResponsibilityRepository {
    override fun observeResponsibilities(reviewId: String): Flow<List<Responsibility>> = dao.observeForReview(reviewId).map { list -> list.map { it.toDomain() } }

    override suspend fun getResponsibilities(reviewId: String): List<Responsibility> = dao.getForReview(reviewId).map { it.toDomain() }

    override suspend fun upsertResponsibility(responsibility: Responsibility) {
        dao.upsert(responsibility.toEntity())
    }

    override suspend fun deleteResponsibility(id: String) {
        dao.deleteById(id)
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

    override suspend fun upsert(reference: AdvisorReference) {
        dao.upsert(reference.toEntity())
    }

    override suspend fun delete(reviewId: String) {
        dao.delete(reviewId)
    }
}
