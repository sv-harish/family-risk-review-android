package com.familyriskreview.core.database

import com.familyriskreview.core.database.entity.ReviewEntity
import com.familyriskreview.core.model.AppLanguage
import com.familyriskreview.core.model.ReviewMode
import com.familyriskreview.core.model.ReviewStatus
import com.familyriskreview.core.model.ReviewStep
import com.familyriskreview.core.model.SyncState
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Lightweight entity smoke test (no Android runtime).
 * Instrumented / Robolectric DAO tests arrive with Phase 1–6 persistence work.
 */
class ReviewEntitySmokeTest {

    @Test
    fun reviewEntityHoldsRequiredFields() {
        val entity = ReviewEntity(
            id = "id-1",
            reviewNumber = "FRR-2026-0001",
            mode = ReviewMode.QUICK,
            language = AppLanguage.ENGLISH,
            status = ReviewStatus.IN_PROGRESS,
            currentStep = ReviewStep.WELCOME,
            createdAtEpochMs = 1L,
            updatedAtEpochMs = 1L,
            assumptionVersion = "1.0.0",
            calculationVersion = "1.0.0",
            syncState = SyncState.LOCAL_ONLY,
            revision = 1L,
            customerAcknowledged = false,
        )
        assertThat(entity.reviewNumber).startsWith("FRR-")
        assertThat(entity.mode).isEqualTo(ReviewMode.QUICK)
    }
}
