package com.familyriskreview.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.familyriskreview.core.model.ScenarioKind

@Entity(
    tableName = "calculation_snapshots",
    foreignKeys = [
        ForeignKey(
            entity = ReviewEntity::class,
            parentColumns = ["id"],
            childColumns = ["reviewId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("reviewId"),
        Index(value = ["reviewId", "calculationInputRevision"]),
    ],
)
data class CalculationSnapshotEntity(
    @PrimaryKey val id: String,
    val reviewId: String,
    val reviewRevision: Long,
    val calculationInputRevision: Long,
    val assumptionVersion: String,
    val calculationVersion: String,
    val scenarioKind: ScenarioKind,
    val assumptionsJson: String,
    val mustContinueTotalRupees: Long,
    val adjustableTotalRupees: Long,
    val postponedTotalRupees: Long,
    val perResponsibilityJson: String,
    val generatedAtEpochMs: Long,
)
