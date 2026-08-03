package com.familyriskreview.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.familyriskreview.core.model.AnnualRateBps
import com.familyriskreview.core.model.AppLanguage
import com.familyriskreview.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-level preferences (not per-review session state).
 * Review language is stored on the Review aggregate; this holds defaults and UI prefs.
 */
@Singleton
class UserPreferencesDataSource
@Inject
constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val preferences: Flow<UserPreferences> =
        dataStore.data.map { prefs ->
            UserPreferences(
                defaultLanguage =
                prefs[Keys.DEFAULT_LANGUAGE]
                    ?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() }
                    ?: AppLanguage.ENGLISH,
                reducedMotion = prefs[Keys.REDUCED_MOTION] ?: false,
                educationInflationBps =
                prefs[Keys.EDUCATION_INFLATION_BPS]
                    ?: AnnualRateBps.EDUCATION_DEFAULT.value,
                marriageInflationBps =
                prefs[Keys.MARRIAGE_INFLATION_BPS]
                    ?: AnnualRateBps.MARRIAGE_DEFAULT.value,
                expenseInflationBps =
                prefs[Keys.EXPENSE_INFLATION_BPS]
                    ?: AnnualRateBps.EXPENSE_DEFAULT.value,
                syncEnabled = prefs[Keys.SYNC_ENABLED] ?: false,
            )
        }

    suspend fun setDefaultLanguage(language: AppLanguage) {
        dataStore.edit { it[Keys.DEFAULT_LANGUAGE] = language.name }
    }

    suspend fun setReducedMotion(enabled: Boolean) {
        dataStore.edit { it[Keys.REDUCED_MOTION] = enabled }
    }

    suspend fun setEducationInflationBps(bps: Int) {
        AnnualRateBps(bps) // validate
        dataStore.edit { it[Keys.EDUCATION_INFLATION_BPS] = bps }
    }

    suspend fun setMarriageInflationBps(bps: Int) {
        AnnualRateBps(bps)
        dataStore.edit { it[Keys.MARRIAGE_INFLATION_BPS] = bps }
    }

    suspend fun setExpenseInflationBps(bps: Int) {
        AnnualRateBps(bps)
        dataStore.edit { it[Keys.EXPENSE_INFLATION_BPS] = bps }
    }

    suspend fun setSyncEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.SYNC_ENABLED] = enabled }
    }

    private object Keys {
        val DEFAULT_LANGUAGE = stringPreferencesKey("default_language")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        val EDUCATION_INFLATION_BPS = intPreferencesKey("education_inflation_bps")
        val MARRIAGE_INFLATION_BPS = intPreferencesKey("marriage_inflation_bps")
        val EXPENSE_INFLATION_BPS = intPreferencesKey("expense_inflation_bps")
        val SYNC_ENABLED = booleanPreferencesKey("sync_enabled")
    }
}
