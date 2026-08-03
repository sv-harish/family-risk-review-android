package com.familyriskreview.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.familyriskreview.core.model.AppLanguage
import com.familyriskreview.core.model.CalculationAssumptions
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * App-level preferences (not per-review session state).
 * Review language is stored on the Review aggregate; this holds defaults and UI prefs.
 */
@Singleton
class UserPreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val preferences: Flow<UserPreferences> = dataStore.data.map { prefs ->
        UserPreferences(
            defaultLanguage = prefs[Keys.DEFAULT_LANGUAGE]
                ?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() }
                ?: AppLanguage.ENGLISH,
            reducedMotion = prefs[Keys.REDUCED_MOTION] ?: false,
            educationInflationAnnual = prefs[Keys.EDUCATION_INFLATION]
                ?.toDoubleOrNull()
                ?: CalculationAssumptions.Default.educationInflationAnnual,
            marriageInflationAnnual = prefs[Keys.MARRIAGE_INFLATION]
                ?.toDoubleOrNull()
                ?: CalculationAssumptions.Default.marriageInflationAnnual,
            expenseInflationAnnual = prefs[Keys.EXPENSE_INFLATION]
                ?.toDoubleOrNull()
                ?: CalculationAssumptions.Default.expenseInflationAnnual,
            syncEnabled = prefs[Keys.SYNC_ENABLED] ?: false,
        )
    }

    suspend fun setDefaultLanguage(language: AppLanguage) {
        dataStore.edit { it[Keys.DEFAULT_LANGUAGE] = language.name }
    }

    suspend fun setReducedMotion(enabled: Boolean) {
        dataStore.edit { it[Keys.REDUCED_MOTION] = enabled }
    }

    suspend fun setEducationInflation(rate: Double) {
        dataStore.edit { it[Keys.EDUCATION_INFLATION] = rate.toString() }
    }

    suspend fun setMarriageInflation(rate: Double) {
        dataStore.edit { it[Keys.MARRIAGE_INFLATION] = rate.toString() }
    }

    suspend fun setExpenseInflation(rate: Double) {
        dataStore.edit { it[Keys.EXPENSE_INFLATION] = rate.toString() }
    }

    suspend fun setSyncEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.SYNC_ENABLED] = enabled }
    }

    private object Keys {
        val DEFAULT_LANGUAGE = stringPreferencesKey("default_language")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        val EDUCATION_INFLATION = stringPreferencesKey("education_inflation")
        val MARRIAGE_INFLATION = stringPreferencesKey("marriage_inflation")
        val EXPENSE_INFLATION = stringPreferencesKey("expense_inflation")
        val SYNC_ENABLED = booleanPreferencesKey("sync_enabled")
    }
}

data class UserPreferences(
    val defaultLanguage: AppLanguage = AppLanguage.ENGLISH,
    val reducedMotion: Boolean = false,
    val educationInflationAnnual: Double = CalculationAssumptions.Default.educationInflationAnnual,
    val marriageInflationAnnual: Double = CalculationAssumptions.Default.marriageInflationAnnual,
    val expenseInflationAnnual: Double = CalculationAssumptions.Default.expenseInflationAnnual,
    val syncEnabled: Boolean = false,
) {
    fun toAssumptions(): CalculationAssumptions =
        CalculationAssumptions(
            educationInflationAnnual = educationInflationAnnual,
            marriageInflationAnnual = marriageInflationAnnual,
            expenseInflationAnnual = expenseInflationAnnual,
        )
}
