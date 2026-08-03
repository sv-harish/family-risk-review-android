package com.familyriskreview.core.data.repository

import com.familyriskreview.core.model.AdvisorReference
import com.familyriskreview.core.model.AppLanguage
import com.familyriskreview.core.model.HouseholdMember
import com.familyriskreview.core.model.Responsibility
import com.familyriskreview.core.model.Review
import com.familyriskreview.core.model.ReviewMode
import com.familyriskreview.core.model.ReviewStatus
import com.familyriskreview.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface ReviewRepository {
    fun observeActiveReviews(): Flow<List<Review>>

    fun observeByStatus(status: ReviewStatus): Flow<List<Review>>

    fun observeReview(id: String): Flow<Review?>

    suspend fun getReview(id: String): Review?

    suspend fun createReview(
        mode: ReviewMode,
        language: AppLanguage,
    ): Review

    suspend fun updateReview(review: Review)

    suspend fun archiveReview(id: String)

    suspend fun softDeleteReview(id: String)
}

interface HouseholdRepository {
    fun observeMembers(reviewId: String): Flow<List<HouseholdMember>>

    suspend fun getMembers(reviewId: String): List<HouseholdMember>

    suspend fun upsertMember(member: HouseholdMember)

    suspend fun deleteMember(id: String)
}

interface ResponsibilityRepository {
    fun observeResponsibilities(reviewId: String): Flow<List<Responsibility>>

    suspend fun getResponsibilities(reviewId: String): List<Responsibility>

    suspend fun upsertResponsibility(responsibility: Responsibility)

    suspend fun deleteResponsibility(id: String)
}

interface AdvisorReferenceRepository {
    fun observe(reviewId: String): Flow<AdvisorReference?>

    suspend fun get(reviewId: String): AdvisorReference?

    suspend fun upsert(reference: AdvisorReference)

    suspend fun delete(reviewId: String)
}

interface UserPreferencesRepository {
    val preferences: Flow<UserPreferences>

    suspend fun setDefaultLanguage(language: AppLanguage)

    suspend fun setReducedMotion(enabled: Boolean)

    suspend fun setEducationInflationBps(bps: Int)

    suspend fun setMarriageInflationBps(bps: Int)

    suspend fun setExpenseInflationBps(bps: Int)

    suspend fun setSyncEnabled(enabled: Boolean)
}
