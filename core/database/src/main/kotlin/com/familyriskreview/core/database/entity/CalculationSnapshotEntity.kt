package com.familyriskreview.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.familyriskreview.core.model.ScenarioKind

/**
 * Persisted calculation snapshot for a review revision.
 * Soft-deleting a review does not physically cascade-delete rows while the
 * review row remains as a tombstone; FK CASCADE only applies to hard deletes.
 */
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
        Index(value = ["reviewId", "reviewRevision"]),
    ],
)
data class CalculationSnapshotEntity(
    @PrimaryKey val id: String,
    val reviewId: String,
    val reviewRevision: Long,
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
