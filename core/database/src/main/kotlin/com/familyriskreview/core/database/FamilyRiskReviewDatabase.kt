package com.familyriskreview.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.familyriskreview.core.database.dao.AdvisorReferenceDao
import com.familyriskreview.core.database.dao.CalculationSnapshotDao
import com.familyriskreview.core.database.dao.HouseholdMemberDao
import com.familyriskreview.core.database.dao.ResponsibilityDao
import com.familyriskreview.core.database.dao.ReviewDao
import com.familyriskreview.core.database.entity.AdvisorReferenceEntity
import com.familyriskreview.core.database.entity.CalculationSnapshotEntity
import com.familyriskreview.core.database.entity.HouseholdMemberEntity
import com.familyriskreview.core.database.entity.ResponsibilityEntity
import com.familyriskreview.core.database.entity.ReviewEntity

/**
 * Local source of truth for Family Risk Review.
 *
 * ## Schema policy (Phase 1)
 *
 * Schema version 1 remains **provisional / pre-release**. Fields were corrected
 * directly in Phase 1 (statusBeforeArchive, summaryStale, assumptionsJson,
 * calculation_snapshots, advisor-only columns). No migration chain is required
 * until the schema-freeze milestone.
 *
 * **Schema-freeze milestone:** the first intentionally distributed
 * persistence-compatible build (declared in docs/DECISIONS.md). From that
 * build onward, safe Room migrations are mandatory and destructive migration
 * is forbidden in production.
 *
 * Continue exporting schemas to `schemas/` and checking them into source control.
 */
@Database(
    entities = [
        ReviewEntity::class,
        HouseholdMemberEntity::class,
        ResponsibilityEntity::class,
        AdvisorReferenceEntity::class,
        CalculationSnapshotEntity::class,
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

    abstract fun calculationSnapshotDao(): CalculationSnapshotDao

    companion object {
        /** Provisional pre-release schema. Corrected in Phase 1 before freeze. */
        const val VERSION: Int = 1
        const val NAME: String = "family_risk_review.db"
        const val SCHEMA_STATUS: String = "PROVISIONAL_PRE_RELEASE"
    }
}
