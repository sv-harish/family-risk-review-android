package com.familyriskreview.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.familyriskreview.core.model.AppLanguage
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UserPreferencesDataSourceTest {
    private lateinit var dataSource: UserPreferencesDataSource

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store =
            PreferenceDataStoreFactory.create(
                produceFile = { context.preferencesDataStoreFile("test_prefs_${System.nanoTime()}") },
            )
        dataSource = UserPreferencesDataSource(store)
    }

    @Test
    fun languageAndReducedMotion_persist() = runTest {
        dataSource.preferences.test {
            awaitItem() // defaults
            dataSource.setDefaultLanguage(AppLanguage.HINDI)
            dataSource.setReducedMotion(true)
            val updated = awaitItem()
            // May receive intermediate emissions; drain until both applied
            var latest = updated
            while (latest.defaultLanguage != AppLanguage.HINDI || !latest.reducedMotion) {
                latest = awaitItem()
            }
            assertThat(latest.defaultLanguage).isEqualTo(AppLanguage.HINDI)
            assertThat(latest.reducedMotion).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
