package com.familyriskreview.core.data.di

import com.familyriskreview.core.data.repository.AdvisorReferenceRepository
import com.familyriskreview.core.data.repository.DefaultAdvisorReferenceRepository
import com.familyriskreview.core.data.repository.DefaultHouseholdRepository
import com.familyriskreview.core.data.repository.DefaultResponsibilityRepository
import com.familyriskreview.core.data.repository.DefaultReviewRepository
import com.familyriskreview.core.data.repository.DefaultUserPreferencesRepository
import com.familyriskreview.core.data.repository.HouseholdRepository
import com.familyriskreview.core.data.repository.ResponsibilityRepository
import com.familyriskreview.core.data.repository.ReviewRepository
import com.familyriskreview.core.data.repository.UserPreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds @Singleton
    abstract fun bindReviewRepository(impl: DefaultReviewRepository): ReviewRepository

    @Binds @Singleton
    abstract fun bindHouseholdRepository(impl: DefaultHouseholdRepository): HouseholdRepository

    @Binds @Singleton
    abstract fun bindResponsibilityRepository(impl: DefaultResponsibilityRepository): ResponsibilityRepository

    @Binds @Singleton
    abstract fun bindAdvisorReferenceRepository(impl: DefaultAdvisorReferenceRepository): AdvisorReferenceRepository

    @Binds @Singleton
    abstract fun bindUserPreferencesRepository(impl: DefaultUserPreferencesRepository): UserPreferencesRepository
}
