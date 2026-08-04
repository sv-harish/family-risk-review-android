package com.familyriskreview.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.familyriskreview.core.database.entity.ReviewEntity
import com.familyriskreview.core.model.AppLanguage
import com.familyriskreview.core.model.ReviewMode
import com.familyriskreview.core.model.ReviewStatus
import com.familyriskreview.core.model.ReviewStep
import com.familyriskreview.core.model.SyncState
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewDao {
    @Query("SELECT * FROM reviews WHERE status != 'DELETED' ORDER BY updatedAtEpochMs DESC")
    fun observeAllActive(): Flow<List<ReviewEntity>>

    @Query(
        "SELECT * FROM reviews WHERE status = :status AND status != 'DELETED' " +
            "ORDER BY updatedAtEpochMs DESC",
    )
    fun observeByStatus(status: ReviewStatus): Flow<List<ReviewEntity>>

    @Query(
        "SELECT * FROM reviews WHERE status = 'IN_PROGRESS' " +
            "ORDER BY updatedAtEpochMs DESC",
    )
    fun observeInProgress(): Flow<List<ReviewEntity>>

    @Query("SELECT * FROM reviews WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ReviewEntity?

    @Query("SELECT * FROM reviews WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<ReviewEntity?>

    @Query("SELECT COUNT(*) FROM reviews WHERE reviewNumber = :reviewNumber")
    suspend fun countByReviewNumber(reviewNumber: String): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: ReviewEntity)

    @Update
    suspend fun update(entity: ReviewEntity)

    @Query(
        """
        UPDATE reviews SET
            reviewNumber = :reviewNumber,
            mode = :mode,
            language = :language,
            status = :status,
            currentStep = :currentStep,
            updatedAtEpochMs = :updatedAtEpochMs,
            completedAtEpochMs = :completedAtEpochMs,
            focusedIncomeContributorId = :focusedIncomeContributorId,
            assumptionVersion = :assumptionVersion,
            calculationVersion = :calculationVersion,
            syncState = :syncState,
            revision = :newRevision,
            calculationInputRevision = :calculationInputRevision,
            customerAcknowledged = :customerAcknowledged,
            statusBeforeArchive = :statusBeforeArchive,
            summaryStale = :summaryStale,
            assumptionsJson = :assumptionsJson
        WHERE id = :id AND revision = :expectedRevision
        """,
    )
    suspend fun updateCas(
        id: String,
        expectedRevision: Long,
        newRevision: Long,
        reviewNumber: String,
        mode: ReviewMode,
        language: AppLanguage,
        status: ReviewStatus,
        currentStep: ReviewStep,
        updatedAtEpochMs: Long,
        completedAtEpochMs: Long?,
        focusedIncomeContributorId: String?,
        assumptionVersion: String,
        calculationVersion: String,
        syncState: SyncState,
        calculationInputRevision: Long,
        customerAcknowledged: Boolean,
        statusBeforeArchive: ReviewStatus?,
        summaryStale: Boolean,
        assumptionsJson: String,
    ): Int

    @Query(
        """
        UPDATE reviews SET
            status = :status,
            statusBeforeArchive = :statusBeforeArchive,
            updatedAtEpochMs = :updatedAtEpochMs,
            revision = :newRevision,
            syncState = :syncState,
            completedAtEpochMs = :completedAtEpochMs
        WHERE id = :id AND revision = :expectedRevision
        """,
    )
    suspend fun updateStatusCas(
        id: String,
        expectedRevision: Long,
        newRevision: Long,
        status: ReviewStatus,
        statusBeforeArchive: ReviewStatus?,
        updatedAtEpochMs: Long,
        syncState: SyncState,
        completedAtEpochMs: Long? = null,
    ): Int

    @Query(
        """
        UPDATE reviews SET
            currentStep = :currentStep,
            updatedAtEpochMs = :updatedAtEpochMs,
            revision = :newRevision,
            syncState = :syncState
        WHERE id = :id AND revision = :expectedRevision
        """,
    )
    suspend fun updateStepCas(
        id: String,
        expectedRevision: Long,
        newRevision: Long,
        currentStep: ReviewStep,
        updatedAtEpochMs: Long,
        syncState: SyncState,
    ): Int

    /**
     * Calculation-input mutation: bumps aggregate revision and input fingerprint,
     * marks summary stale.
     */
    @Query(
        """
        UPDATE reviews SET
            focusedIncomeContributorId = :focusedIncomeContributorId,
            summaryStale = 1,
            updatedAtEpochMs = :updatedAtEpochMs,
            revision = :newRevision,
            calculationInputRevision = :newInputRevision,
            syncState = :syncState
        WHERE id = :id AND revision = :expectedRevision
        """,
    )
    suspend fun touchAggregateCas(
        id: String,
        expectedRevision: Long,
        newRevision: Long,
        newInputRevision: Long,
        focusedIncomeContributorId: String?,
        updatedAtEpochMs: Long,
        syncState: SyncState,
    ): Int

    @Query(
        """
        UPDATE reviews SET
            summaryStale = :summaryStale,
            updatedAtEpochMs = :updatedAtEpochMs,
            revision = :newRevision,
            syncState = :syncState,
            completedAtEpochMs = :completedAtEpochMs,
            status = :status,
            customerAcknowledged = :customerAcknowledged
        WHERE id = :id AND revision = :expectedRevision
        """,
    )
    suspend fun updateAfterCalculationCas(
        id: String,
        expectedRevision: Long,
        newRevision: Long,
        summaryStale: Boolean,
        updatedAtEpochMs: Long,
        syncState: SyncState,
        completedAtEpochMs: Long?,
        status: ReviewStatus,
        customerAcknowledged: Boolean,
    ): Int
}
