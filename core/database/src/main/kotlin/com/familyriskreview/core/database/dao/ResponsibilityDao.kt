package com.familyriskreview.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.familyriskreview.core.database.entity.ResponsibilityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ResponsibilityDao {
    @Query("SELECT * FROM responsibilities WHERE reviewId = :reviewId ORDER BY sortOrder ASC")
    fun observeForReview(reviewId: String): Flow<List<ResponsibilityEntity>>

    @Query("SELECT * FROM responsibilities WHERE reviewId = :reviewId ORDER BY sortOrder ASC")
    suspend fun getForReview(reviewId: String): List<ResponsibilityEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ResponsibilityEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<ResponsibilityEntity>)

    @Update
    suspend fun update(entity: ResponsibilityEntity)

    @Query("DELETE FROM responsibilities WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM responsibilities WHERE reviewId = :reviewId")
    suspend fun deleteForReview(reviewId: String)
}
