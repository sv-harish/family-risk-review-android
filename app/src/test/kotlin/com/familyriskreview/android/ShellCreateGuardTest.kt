package com.familyriskreview.android

import com.familyriskreview.android.navigation.ShellViewModel
import com.familyriskreview.android.navigation.StartReviewUiResult
import com.familyriskreview.core.data.repository.UserPreferencesRepository
import com.familyriskreview.core.data.usecase.CreateReviewUseCase
import com.familyriskreview.core.model.AppLanguage
import com.familyriskreview.core.model.Review
import com.familyriskreview.core.model.ReviewMode
import com.familyriskreview.core.model.ReviewStatus
import com.familyriskreview.core.model.ReviewStep
import com.familyriskreview.core.model.SyncState
import com.familyriskreview.core.model.UserPreferences
import com.familyriskreview.core.model.result.DomainResult
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Instant
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShellCreateGuardTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var createReview: CreateReviewUseCase
    private lateinit var preferences: UserPreferencesRepository
    private lateinit var viewModel: ShellViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        createReview = mockk()
        preferences = mockk(relaxed = true)
        every { preferences.preferences } returns MutableStateFlow(UserPreferences())
        viewModel = ShellViewModel(createReview, preferences)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun doubleCreate_whileInFlight_secondCallRejectedWithoutSecondUseCaseInvoke() = runTest(dispatcher) {
        coEvery { createReview(any(), any(), any()) } coAnswers {
            delay(50)
            DomainResult.success(sampleReview())
        }

        val first = async { viewModel.startReview(ReviewMode.QUICK) }
        // Unconfined runs until the delay suspension; guard must already be set.
        assertThat(viewModel.isCreating.value).isTrue()

        val second = viewModel.startReview(ReviewMode.GUIDED)
        assertThat(second).isEqualTo(StartReviewUiResult.Failure("CREATING"))

        advanceUntilIdle()
        assertThat(first.await()).isInstanceOf(StartReviewUiResult.Success::class.java)
        coVerify(exactly = 1) { createReview(any(), any(), any()) }
        assertThat(viewModel.isCreating.value).isFalse()
    }

    @Test
    fun sequentialCreates_afterIdle_bothInvokeUseCase() = runTest(dispatcher) {
        coEvery { createReview(any(), any(), any()) } returns DomainResult.success(sampleReview())

        val first = viewModel.startReview(ReviewMode.QUICK)
        val second = viewModel.startReview(ReviewMode.QUICK)

        assertThat(first).isInstanceOf(StartReviewUiResult.Success::class.java)
        assertThat(second).isInstanceOf(StartReviewUiResult.Success::class.java)
        coVerify(exactly = 2) { createReview(any(), any(), any()) }
    }

    private fun sampleReview(): Review = Review(
        id = "new-review",
        reviewNumber = "FRR-20260804-000099",
        mode = ReviewMode.QUICK,
        language = AppLanguage.ENGLISH,
        status = ReviewStatus.IN_PROGRESS,
        currentStep = ReviewStep.HOUSEHOLD_SUPPORT_MAP,
        createdAt = Instant.fromEpochMilliseconds(1L),
        updatedAt = Instant.fromEpochMilliseconds(1L),
        calculationVersion = "test",
        syncState = SyncState.LOCAL_ONLY,
    )
}
