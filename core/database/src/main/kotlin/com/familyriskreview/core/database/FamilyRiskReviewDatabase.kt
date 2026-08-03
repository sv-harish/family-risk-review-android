package com.familyriskreview.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.familyriskreview.core.database.dao.AdvisorReferenceDao
import com.familyriskreview.core.database.dao.HouseholdMemberDao
import com.familyriskreview.core.database.dao.ResponsibilityDao
import com.familyriskreview.core.database.dao.ReviewDao
import com.familyriskreview.core.database.entity.AdvisorReferenceEntity
import com.familyriskreview.core.database.entity.HouseholdMemberEntity
import com.familyriskreview.core.database.entity.ResponsibilityEntity
import com.familyriskreview.core.database.entity.ReviewEntity

/**
 * Local source of truth for Family Risk Review.
 *
 * Schema version 1 is the first public schema. From this point forward,
 * use safe Room migrations — never destructive migration in production.
 */
@Database(
    entities = [
        ReviewEntity::class,
        HouseholdMemberEntity::class,
        ResponsibilityEntity::class,
        AdvisorReferenceEntity::class,
    ],
    version = FamilyRiskReviewDatabase.VERSION,
    exportSchema = true,
)
@TypeConverters(DatabaseConverters::class)
abstract class FamilyRiskReviewDatabase : RoomDatabase() {
    abstract fun reviewDao(): ReviewDao
    abstract fun householdMemberDao(): HouseholdMemberDao
    abstract fun responsibilityDao(): ResponsibilityDao
    abstract fun advisorReferenceDao(): AdvisorReferenceDao

    companion object {
        const val VERSION: Int = 1
        const val NAME: String = "family_risk_review.db"
    }
}
