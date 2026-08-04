package com.familyriskreview.core.data.di

import com.familyriskreview.core.data.repository.AdvisorReferenceRepository
import com.familyriskreview.core.data.repository.CalculationSnapshotRepository
import com.familyriskreview.core.data.repository.DefaultAdvisorReferenceRepository
import com.familyriskreview.core.data.repository.DefaultCalculationSnapshotRepository
import com.familyriskreview.core.data.repository.DefaultHouseholdRepository
import com.familyriskreview.core.data.repository.DefaultResponsibilityRepository
import com.familyriskreview.core.data.repository.DefaultReviewRepository
import com.familyriskreview.core.data.repository.DefaultUserPreferencesRepository
import com.familyriskreview.core.data.repository.HouseholdRepository
import com.familyriskreview.core.data.repository.ResponsibilityRepository
import com.familyriskreview.core.data.repository.ReviewMutationWriter
import com.familyriskreview.core.data.repository.ReviewReader
import com.familyriskreview.core.data.repository.UserPreferencesRepository
import com.familyriskreview.core.data.service.DefaultReviewNumberProvider
import com.familyriskreview.core.data.service.SystemClock
import com.familyriskreview.core.data.service.UuidIdGenerator
import com.familyriskreview.core.model.service.Clock
import com.familyriskreview.core.model.service.IdGenerator
import com.familyriskreview.core.model.service.ReviewNumberProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds @Singleton
    abstract fun bindReviewReader(impl: DefaultReviewRepository): ReviewReader

    @Binds @Singleton
    internal abstract fun bindReviewMutationWriter(impl: DefaultReviewRepository): ReviewMutationWriter

    @Binds @Singleton
    abstract fun bindHouseholdRepository(impl: DefaultHouseholdRepository): HouseholdRepository

    @Binds @Singleton
    abstract fun bindResponsibilityRepository(impl: DefaultResponsibilityRepository): ResponsibilityRepository

    @Binds @Singleton
    abstract fun bindAdvisorReferenceRepository(impl: DefaultAdvisorReferenceRepository): AdvisorReferenceRepository

    @Binds @Singleton
    abstract fun bindCalculationSnapshotRepository(
        impl: DefaultCalculationSnapshotRepository,
    ): CalculationSnapshotRepository

    @Binds @Singleton
    abstract fun bindUserPreferencesRepository(impl: DefaultUserPreferencesRepository): UserPreferencesRepository

    @Binds @Singleton
    abstract fun bindClock(impl: SystemClock): Clock

    @Binds @Singleton
    abstract fun bindIdGenerator(impl: UuidIdGenerator): IdGenerator

    @Binds @Singleton
    abstract fun bindReviewNumberProvider(impl: DefaultReviewNumberProvider): ReviewNumberProvider
}
