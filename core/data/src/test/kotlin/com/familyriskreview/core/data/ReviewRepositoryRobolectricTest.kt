package com.familyriskreview.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.familyriskreview.core.data.repository.DefaultReviewRepository
import com.familyriskreview.core.database.FamilyRiskReviewDatabase
import com.familyriskreview.core.model.AppLanguage
import com.familyriskreview.core.model.ReviewMode
import com.familyriskreview.core.model.ReviewStep
import com.familyriskreview.core.sync.FakeSyncClient
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReviewRepositoryRobolectricTest {
    private lateinit var db: FamilyRiskReviewDatabase
    private lateinit var repository: DefaultReviewRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db =
            Room
                .inMemoryDatabaseBuilder(context, FamilyRiskReviewDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        repository = DefaultReviewRepository(db.reviewDao(), FakeSyncClient())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun createReview_persistsModeAndStartsAtHouseholdStep() = runBlocking {
        val quick = repository.createReview(ReviewMode.QUICK, AppLanguage.ENGLISH)
        val guided = repository.createReview(ReviewMode.GUIDED, AppLanguage.TAMIL)
        assertThat(quick.mode).isEqualTo(ReviewMode.QUICK)
        assertThat(guided.mode).isEqualTo(ReviewMode.GUIDED)
        assertThat(quick.currentStep).isEqualTo(ReviewStep.HOUSEHOLD_SUPPORT_MAP)
        assertThat(guided.language).isEqualTo(AppLanguage.TAMIL)
        assertThat(repository.getReview(quick.id)?.reviewNumber).isEqualTo(quick.reviewNumber)
        assertThat(quick.reviewNumber).isNotEqualTo(guided.reviewNumber)
    }
}
