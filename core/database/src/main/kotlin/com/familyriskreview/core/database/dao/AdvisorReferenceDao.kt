package com.familyriskreview.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.familyriskreview.core.database.entity.AdvisorReferenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AdvisorReferenceDao {
    @Query("SELECT * FROM advisor_references WHERE reviewId = :reviewId LIMIT 1")
    fun observe(reviewId: String): Flow<AdvisorReferenceEntity?>

    @Query("SELECT * FROM advisor_references WHERE reviewId = :reviewId LIMIT 1")
    suspend fun get(reviewId: String): AdvisorReferenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AdvisorReferenceEntity)

    @Query("DELETE FROM advisor_references WHERE reviewId = :reviewId")
    suspend fun delete(reviewId: String)
}
