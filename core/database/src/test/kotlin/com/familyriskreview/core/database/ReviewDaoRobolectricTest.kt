package com.familyriskreview.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.familyriskreview.core.database.entity.ReviewEntity
import com.familyriskreview.core.model.AppLanguage
import com.familyriskreview.core.model.CalculationAssumptions
import com.familyriskreview.core.model.ReviewMode
import com.familyriskreview.core.model.ReviewStatus
import com.familyriskreview.core.model.ReviewStep
import com.familyriskreview.core.model.SyncState
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReviewDaoRobolectricTest {
    private lateinit var db: FamilyRiskReviewDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db =
            Room
                .inMemoryDatabaseBuilder(context, FamilyRiskReviewDatabase::class.java)
                .allowMainThreadQueries()
                .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun provisionalSchema_insertAndRetrieve() = runBlocking {
        val entity =
            ReviewEntity(
                id = "id-1",
                reviewNumber = "FRR-20260101-AAAAAA",
                mode = ReviewMode.QUICK,
                language = AppLanguage.ENGLISH,
                status = ReviewStatus.IN_PROGRESS,
                currentStep = ReviewStep.RESPONSIBILITIES,
                createdAtEpochMs = 1L,
                updatedAtEpochMs = 1L,
                assumptionVersion = "1.1.0",
                calculationVersion = "1.1.0",
                syncState = SyncState.LOCAL_ONLY,
                revision = 1L,
                calculationInputRevision = 1L,
                customerAcknowledged = false,
                summaryStale = true,
                assumptionsJson = CalculationAssumptions.Default.toJson(),
            )
        db.reviewDao().insert(entity)
        val loaded = db.reviewDao().getById("id-1")
        assertThat(loaded).isEqualTo(entity)
        assertThat(loaded!!.mode).isEqualTo(ReviewMode.QUICK)
        assertThat(FamilyRiskReviewDatabase.SCHEMA_STATUS).isEqualTo("PROVISIONAL_PRE_RELEASE")
    }

    @Test
    fun updateStatusCas_softDeletePreservesRow() = runBlocking {
        val entity =
            ReviewEntity(
                id = "id-2",
                reviewNumber = "FRR-20260101-BBBBBB",
                mode = ReviewMode.GUIDED,
                language = AppLanguage.HINDI,
                status = ReviewStatus.IN_PROGRESS,
                currentStep = ReviewStep.HOUSEHOLD_SUPPORT_MAP,
                createdAtEpochMs = 1L,
                updatedAtEpochMs = 1L,
                assumptionVersion = "1.1.0",
                calculationVersion = "1.1.0",
                syncState = SyncState.LOCAL_ONLY,
                revision = 1L,
                calculationInputRevision = 1L,
                customerAcknowledged = false,
                summaryStale = true,
                assumptionsJson = CalculationAssumptions.Default.toJson(),
            )
        db.reviewDao().insert(entity)
        val rows =
            db.reviewDao().updateStatusCas(
                id = "id-2",
                expectedRevision = 1L,
                newRevision = 2L,
                status = ReviewStatus.DELETED,
                statusBeforeArchive = null,
                updatedAtEpochMs = 5L,
                syncState = SyncState.PENDING,
                completedAtEpochMs = null,
            )
        assertThat(rows).isEqualTo(1)
        val loaded = db.reviewDao().getById("id-2")
        assertThat(loaded!!.status).isEqualTo(ReviewStatus.DELETED)
        assertThat(loaded.revision).isEqualTo(2L)
    }

    @Test
    fun updateCas_returnsZeroOnConflict() = runBlocking {
        val entity =
            ReviewEntity(
                id = "id-3",
                reviewNumber = "FRR-20260101-CCCCCC",
                mode = ReviewMode.QUICK,
                language = AppLanguage.ENGLISH,
                status = ReviewStatus.IN_PROGRESS,
                currentStep = ReviewStep.HOUSEHOLD_SUPPORT_MAP,
                createdAtEpochMs = 1L,
                updatedAtEpochMs = 1L,
                assumptionVersion = "1.1.0",
                calculationVersion = "1.1.0",
                syncState = SyncState.LOCAL_ONLY,
                revision = 1L,
                calculationInputRevision = 1L,
                customerAcknowledged = false,
                summaryStale = true,
                assumptionsJson = CalculationAssumptions.Default.toJson(),
            )
        db.reviewDao().insert(entity)
        val rows =
            db.reviewDao().updateStepCas(
                id = "id-3",
                expectedRevision = 99L,
                newRevision = 100L,
                currentStep = ReviewStep.RESPONSIBILITIES,
                updatedAtEpochMs = 9L,
                syncState = SyncState.PENDING,
            )
        assertThat(rows).isEqualTo(0)
    }
}
