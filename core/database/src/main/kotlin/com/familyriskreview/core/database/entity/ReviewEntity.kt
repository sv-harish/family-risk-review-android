package com.familyriskreview.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.familyriskreview.core.model.AppLanguage
import com.familyriskreview.core.model.ReviewMode
import com.familyriskreview.core.model.ReviewStatus
import com.familyriskreview.core.model.ReviewStep
import com.familyriskreview.core.model.SyncState

@Entity(
    tableName = "reviews",
    indices = [
        Index(value = ["reviewNumber"], unique = true),
        Index(value = ["status"]),
        Index(value = ["updatedAtEpochMs"]),
    ],
)
data class ReviewEntity(
    @PrimaryKey val id: String,
    val reviewNumber: String,
    val mode: ReviewMode,
    val language: AppLanguage,
    val status: ReviewStatus,
    val currentStep: ReviewStep,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val completedAtEpochMs: Long? = null,
    val focusedIncomeContributorId: String? = null,
    val assumptionVersion: String,
    val calculationVersion: String,
    val syncState: SyncState,
    val revision: Long,
    val customerAcknowledged: Boolean,
    val serverUpdatedAtEpochMs: Long? = null,
)
