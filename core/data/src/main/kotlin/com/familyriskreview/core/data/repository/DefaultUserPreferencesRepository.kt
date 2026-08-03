package com.familyriskreview.core.data.repository

import com.familyriskreview.core.datastore.UserPreferencesDataSource
import com.familyriskreview.core.model.AppLanguage
import com.familyriskreview.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultUserPreferencesRepository
@Inject
constructor(
    private val dataSource: UserPreferencesDataSource,
) : UserPreferencesRepository {
    override val preferences: Flow<UserPreferences> = dataSource.preferences

    override suspend fun setDefaultLanguage(language: AppLanguage) {
        dataSource.setDefaultLanguage(language)
    }

    override suspend fun setReducedMotion(enabled: Boolean) {
        dataSource.setReducedMotion(enabled)
    }

    override suspend fun setEducationInflationBps(bps: Int) {
        dataSource.setEducationInflationBps(bps)
    }

    override suspend fun setMarriageInflationBps(bps: Int) {
        dataSource.setMarriageInflationBps(bps)
    }

    override suspend fun setExpenseInflationBps(bps: Int) {
        dataSource.setExpenseInflationBps(bps)
    }

    override suspend fun setSyncEnabled(enabled: Boolean) {
        dataSource.setSyncEnabled(enabled)
    }
}
