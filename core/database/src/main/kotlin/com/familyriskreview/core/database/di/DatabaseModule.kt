package com.familyriskreview.core.database.di

import android.content.Context
import androidx.room.Room
import com.familyriskreview.core.database.FamilyRiskReviewDatabase
import com.familyriskreview.core.database.dao.AdvisorReferenceDao
import com.familyriskreview.core.database.dao.HouseholdMemberDao
import com.familyriskreview.core.database.dao.ResponsibilityDao
import com.familyriskreview.core.database.dao.ReviewDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): FamilyRiskReviewDatabase =
        Room.databaseBuilder(
            context,
            FamilyRiskReviewDatabase::class.java,
            FamilyRiskReviewDatabase.NAME,
        )
            // Safe migrations required from schema v1 onward.
            // fallbackToDestructiveMigration is intentionally omitted for production builds.
            .build()

    @Provides
    fun provideReviewDao(db: FamilyRiskReviewDatabase): ReviewDao = db.reviewDao()

    @Provides
    fun provideHouseholdMemberDao(db: FamilyRiskReviewDatabase): HouseholdMemberDao =
        db.householdMemberDao()

    @Provides
    fun provideResponsibilityDao(db: FamilyRiskReviewDatabase): ResponsibilityDao =
        db.responsibilityDao()

    @Provides
    fun provideAdvisorReferenceDao(db: FamilyRiskReviewDatabase): AdvisorReferenceDao =
        db.advisorReferenceDao()
}
