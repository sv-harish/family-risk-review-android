package com.familyriskreview.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.familyriskreview.core.database.entity.CalculationSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CalculationSnapshotDao {
    @Query(
        "SELECT * FROM calculation_snapshots WHERE reviewId = :reviewId " +
            "ORDER BY generatedAtEpochMs DESC LIMIT 1",
    )
    fun observeLatest(reviewId: String): Flow<CalculationSnapshotEntity?>

    @Query(
        "SELECT * FROM calculation_snapshots WHERE reviewId = :reviewId " +
            "ORDER BY generatedAtEpochMs DESC LIMIT 1",
    )
    suspend fun getLatest(reviewId: String): CalculationSnapshotEntity?

    @Query(
        "SELECT * FROM calculation_snapshots WHERE reviewId = :reviewId " +
            "AND reviewRevision = :revision LIMIT 1",
    )
    suspend fun getForRevision(
        reviewId: String,
        revision: Long,
    ): CalculationSnapshotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CalculationSnapshotEntity)
}
