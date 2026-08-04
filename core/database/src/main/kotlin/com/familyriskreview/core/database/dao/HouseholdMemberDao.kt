package com.familyriskreview.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.familyriskreview.core.database.entity.HouseholdMemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HouseholdMemberDao {
    @Query("SELECT * FROM household_members WHERE reviewId = :reviewId ORDER BY sortOrder ASC")
    fun observeForReview(reviewId: String): Flow<List<HouseholdMemberEntity>>

    @Query("SELECT * FROM household_members WHERE reviewId = :reviewId ORDER BY sortOrder ASC")
    suspend fun getForReview(reviewId: String): List<HouseholdMemberEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: HouseholdMemberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<HouseholdMemberEntity>)

    @Update
    suspend fun update(entity: HouseholdMemberEntity)

    @Query("DELETE FROM household_members WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM household_members WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): HouseholdMemberEntity?

    @Query("DELETE FROM household_members WHERE reviewId = :reviewId")
    suspend fun deleteForReview(reviewId: String)
}
