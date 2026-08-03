package com.familyriskreview.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.familyriskreview.core.database.entity.ReviewEntity
import com.familyriskreview.core.model.ReviewStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewDao {
    @Query("SELECT * FROM reviews WHERE status != 'DELETED' ORDER BY updatedAtEpochMs DESC")
    fun observeAllActive(): Flow<List<ReviewEntity>>

    @Query("SELECT * FROM reviews WHERE status = :status ORDER BY updatedAtEpochMs DESC")
    fun observeByStatus(status: ReviewStatus): Flow<List<ReviewEntity>>

    @Query("SELECT * FROM reviews WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ReviewEntity?

    @Query("SELECT * FROM reviews WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<ReviewEntity?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: ReviewEntity)

    @Update
    suspend fun update(entity: ReviewEntity)

    @Query("UPDATE reviews SET status = 'DELETED', updatedAtEpochMs = :updatedAt, revision = revision + 1 WHERE id = :id")
    suspend fun softDelete(
        id: String,
        updatedAt: Long,
    )

    @Query("UPDATE reviews SET status = 'ARCHIVED', updatedAtEpochMs = :updatedAt, revision = revision + 1 WHERE id = :id")
    suspend fun archive(
        id: String,
        updatedAt: Long,
    )
}
