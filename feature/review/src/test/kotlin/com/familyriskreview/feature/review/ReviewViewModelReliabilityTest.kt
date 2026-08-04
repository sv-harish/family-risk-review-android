package com.familyriskreview.feature.review

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.familyriskreview.core.data.repository.HouseholdRepository
import com.familyriskreview.core.data.repository.ResponsibilityRepository
import com.familyriskreview.core.data.repository.ReviewReader
import com.familyriskreview.core.data.usecase.AdvanceReviewStepUseCase
import com.familyriskreview.core.data.usecase.PrioritiseResponsibilityUseCase
import com.familyriskreview.core.data.usecase.RemoveHouseholdMemberUseCase
import com.familyriskreview.core.data.usecase.SaveHouseholdMemberUseCase
import com.familyriskreview.core.data.usecase.SaveResponsibilityDetailsUseCase
import com.familyriskreview.core.data.usecase.SelectResponsibilityUseCase
import com.familyriskreview.core.data.usecase.SuggestResponsibilitiesUseCase
import com.familyriskreview.core.model.AppLanguage
import com.familyriskreview.core.model.FocusUpdate
import com.familyriskreview.core.model.HouseholdMember
import com.familyriskreview.core.model.Responsibility
import com.familyriskreview.core.model.ResponsibilityCatalogue
import com.familyriskreview.core.model.ResponsibilityPriority
import com.familyriskreview.core.model.Review
import com.familyriskreview.core.model.ReviewMode
import com.familyriskreview.core.model.ReviewStatus
import com.familyriskreview.core.model.ReviewStep
import com.familyriskreview.core.model.SyncState
import com.familyriskreview.core.model.result.DomainError
import com.familyriskreview.core.model.result.DomainResult
import com.familyriskreview.core.model.service.IdGenerator
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Instant
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Reliability tests for [ReviewViewModel] mutation serialization, draft flush-before-advance,
 * Other-identity stability, and conflict reload — using in-memory fakes (plus one mockk for
 * [AdvanceReviewStepUseCase], whose constructor is module-internal).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class ReviewViewModelReliabilityTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var store: FakeReviewStore
    private lateinit var advanceUseCase: AdvanceReviewStepUseCase
    private lateinit var viewModel: ReviewViewModel
    private val writeLog = mutableListOf<String>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        store = FakeReviewStore(REVIEW_ID)
        advanceUseCase = mockk()
        coEvery { advanceUseCase(any(), any(), any()) } coAnswers {
            val expectedRevision = secondArg<Long>()
            writeLog += "advance@$expectedRevision"
            store.bumpRevision(expectedRevision)
        }
        viewModel = createViewModel()
        // Keep WhileSubscribed uiState hot so draft→domain mapping sees responsibilities.
        viewModel.viewModelScope.launch { viewModel.uiState.collect {} }
        dispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun serialMutationCoordinator_rapidPriorityChanges_allAppliedInOrder() = runTest(dispatcher) {
        seedSelectedResponsibility("living", ResponsibilityCatalogue.ESSENTIAL_FAMILY_LIVING_EXPENSES)
        advanceUntilIdle()

        viewModel.setPriority("living", ResponsibilityPriority.MUST_CONTINUE)
        viewModel.setPriority("living", ResponsibilityPriority.IMPORTANT_BUT_ADJUSTABLE)
        viewModel.setPriority("living", ResponsibilityPriority.CAN_BE_POSTPONED_OR_REDUCED)
        advanceUntilIdle()

        val saved = store.responsibilities.value.single { it.id == "living" }
        assertThat(saved.priority)
            .isEqualTo(ResponsibilityPriority.CAN_BE_POSTPONED_OR_REDUCED)
        assertThat(writeLog.filter { it.startsWith("resp@") })
            .containsExactly("resp@1", "resp@2", "resp@3")
            .inOrder()
        assertThat(store.review.value!!.revision).isEqualTo(4L)
    }

    @Test
    fun rapidResponsibilityToggles_allApplied_noneDiscarded() = runTest(dispatcher) {
        advanceUntilIdle()

        viewModel.toggleResponsibility(ResponsibilityCatalogue.PARENT_SUPPORT, selected = true)
        viewModel.toggleResponsibility(ResponsibilityCatalogue.PARENT_SUPPORT, selected = false)
        viewModel.toggleResponsibility(ResponsibilityCatalogue.PARENT_SUPPORT, selected = true)
        advanceUntilIdle()

        val item =
            store.responsibilities.value.single {
                it.catalogue == ResponsibilityCatalogue.PARENT_SUPPORT
            }
        assertThat(item.isSelected).isTrue()
        assertThat(writeLog.count { it.startsWith("resp@") }).isEqualTo(3)
        assertThat(store.review.value!!.revision).isEqualTo(4L)
    }

    @Test
    fun otherLabelEdits_keepStableId_noDuplicates_addAnotherCreatesSecondId() = runTest(dispatcher) {
        advanceUntilIdle()

        // OTHER selection requires a non-blank label (RESP_CUSTOM_LABEL_REQUIRED).
        viewModel.updateOtherLabelDraft(TextDraft.of("School van"))
        viewModel.toggleResponsibility(ResponsibilityCatalogue.OTHER, selected = true)
        advanceUntilIdle()
        val firstId = store.responsibilities.value.single { it.catalogue == ResponsibilityCatalogue.OTHER }.id

        viewModel.updateOtherLabelDraft(TextDraft.of("School van + meals"))
        viewModel.saveActiveOtherLabel()
        advanceUntilIdle()

        val others =
            store.responsibilities.value.filter { it.catalogue == ResponsibilityCatalogue.OTHER }
        assertThat(others).hasSize(1)
        assertThat(others.single().id).isEqualTo(firstId)
        assertThat(others.single().customLabel).isEqualTo("School van + meals")

        viewModel.addAnotherCustomResponsibility()
        advanceUntilIdle()

        val afterAdd =
            store.responsibilities.value.filter { it.catalogue == ResponsibilityCatalogue.OTHER }
        assertThat(afterAdd).hasSize(2)
        assertThat(afterAdd.map { it.id }.toSet()).hasSize(2)
        assertThat(afterAdd.map { it.id }).contains(firstId)
        assertThat(viewModel.uiState.value.activeOtherResponsibilityId)
            .isNotEqualTo(firstId)
    }

    @Test
    fun fieldSaveImmediatelyFollowedByContinue_flushesDraftBeforeAdvance() = runTest(dispatcher) {
        seedSelectedResponsibility("living", ResponsibilityCatalogue.ESSENTIAL_FAMILY_LIVING_EXPENSES)
        advanceUntilIdle()

        val pending =
            store.responsibilities.value.single { it.id == "living" }.copy(
                monthlyAmount = com.familyriskreview.core.model.MoneyAmount(15_000),
                timing =
                com.familyriskreview.core.model.ResponsibilityTiming(
                    com.familyriskreview.core.model.TimingKind.RECURRING_DURATION,
                    durationYears = 12,
                ),
            )

        // Simulate focused field save (sets pendingDetailsFlush) then Continue.
        viewModel.saveDetailsDraft(pending)
        viewModel.advance()
        advanceUntilIdle()

        assertThat(writeLog).containsExactly("resp@1", "advance@2").inOrder()
        val saved = store.responsibilities.value.single { it.id == "living" }
        assertThat(saved.monthlyAmount?.amountRupees).isEqualTo(15_000L)
        assertThat(saved.timing?.durationYears).isEqualTo(12)
    }

    @Test
    fun updateDetailsDraftThenAdvance_flushesPendingBeforeStepChange() = runTest(dispatcher) {
        seedSelectedResponsibility("living", ResponsibilityCatalogue.ESSENTIAL_FAMILY_LIVING_EXPENSES)
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.responsibilities).isNotEmpty()

        viewModel.updateDetailsDraft(
            ResponsibilityDetailsDraft(
                responsibilityId = "living",
                monthlyAmount = TextDraft.of("25000"),
                years = TextDraft.of("8"),
                dirty = true,
            ),
        )
        viewModel.advance()
        advanceUntilIdle()

        assertThat(writeLog.first()).isEqualTo("resp@1")
        assertThat(writeLog).contains("advance@2")
        val saved = store.responsibilities.value.single { it.id == "living" }
        assertThat(saved.monthlyAmount?.amountRupees).isEqualTo(25_000L)
        assertThat(saved.timing?.durationYears).isEqualTo(8)
    }

    @Test
    fun conflictReload_clearsConflict_andAllowsNextMutationWithLatestRevision() = runTest(dispatcher) {
        seedSelectedResponsibility("living", ResponsibilityCatalogue.ESSENTIAL_FAMILY_LIVING_EXPENSES)
        advanceUntilIdle()

        store.forceConflictOnce = true
        viewModel.setPriority("living", ResponsibilityPriority.MUST_CONTINUE)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.conflictActive).isTrue()
        assertThat(viewModel.uiState.value.saveState).isInstanceOf(ReviewSaveState.Conflict::class.java)
        assertThat(store.review.value!!.revision).isEqualTo(5L)

        viewModel.reloadConflict()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.conflictActive).isFalse()
        assertThat(viewModel.uiState.value.saveState).isEqualTo(ReviewSaveState.Idle)

        writeLog.clear()
        viewModel.setPriority("living", ResponsibilityPriority.IMPORTANT_BUT_ADJUSTABLE)
        advanceUntilIdle()

        assertThat(writeLog).containsExactly("resp@5")
        assertThat(
            store.responsibilities.value.single { it.id == "living" }.priority,
        ).isEqualTo(ResponsibilityPriority.IMPORTANT_BUT_ADJUSTABLE)
        assertThat(viewModel.uiState.value.conflictActive).isFalse()
    }

    private fun seedSelectedResponsibility(
        id: String,
        catalogue: ResponsibilityCatalogue,
    ) {
        store.responsibilities.value =
            listOf(
                Responsibility(
                    id = id,
                    reviewId = REVIEW_ID,
                    catalogue = catalogue,
                    isSelected = true,
                    sortOrder = 0,
                ),
            )
    }

    private fun createViewModel(): ReviewViewModel {
        val reader = store.asReader()
        val householdRepo = store.asHouseholdRepository()
        val responsibilityRepo = store.asResponsibilityRepository(writeLog)
        return ReviewViewModel(
            savedStateHandle = SavedStateHandle(mapOf("reviewId" to REVIEW_ID)),
            reviewReader = reader,
            householdRepository = householdRepo,
            responsibilityRepository = responsibilityRepo,
            saveHouseholdMember = SaveHouseholdMemberUseCase(householdRepo),
            removeHouseholdMember = RemoveHouseholdMemberUseCase(householdRepo),
            selectResponsibility = SelectResponsibilityUseCase(responsibilityRepo),
            prioritiseResponsibility = PrioritiseResponsibilityUseCase(responsibilityRepo, reader),
            saveResponsibilityDetails = SaveResponsibilityDetailsUseCase(responsibilityRepo, reader),
            suggestResponsibilities = SuggestResponsibilitiesUseCase(householdRepo),
            advanceReviewStep = advanceUseCase,
            idGenerator =
            object : IdGenerator {
                private var seq = 0

                override fun newId(): String = "gen-${++seq}"
            },
        )
    }

    companion object {
        private const val REVIEW_ID = "review-1"
    }
}

/** In-memory review + household + responsibility store with CAS-style revision bumps. */
internal class FakeReviewStore(
    private val reviewId: String,
) {
    val review =
        MutableStateFlow(
            Review(
                id = reviewId,
                reviewNumber = "FRR-20260804-000001",
                mode = ReviewMode.QUICK,
                language = AppLanguage.ENGLISH,
                status = ReviewStatus.IN_PROGRESS,
                currentStep = ReviewStep.RESPONSIBILITY_DETAILS,
                createdAt = Instant.fromEpochMilliseconds(1_700_000_000_000L),
                updatedAt = Instant.fromEpochMilliseconds(1_700_000_000_000L),
                calculationVersion = "test",
                syncState = SyncState.LOCAL_ONLY,
                revision = 1L,
            ),
        )
    val members = MutableStateFlow<List<HouseholdMember>>(emptyList())
    val responsibilities = MutableStateFlow<List<Responsibility>>(emptyList())

    /** When true, the next responsibility write fails with Conflict and bumps server revision. */
    var forceConflictOnce: Boolean = false

    fun bumpRevision(expectedRevision: Long): DomainResult<Review> {
        val current = review.value!!
        if (current.revision != expectedRevision) {
            return DomainResult.failure(
                DomainError.Conflict("stale", currentRevision = current.revision),
            )
        }
        val next = current.copy(revision = current.revision + 1)
        review.value = next
        return DomainResult.success(next)
    }

    fun asReader(): ReviewReader = object : ReviewReader {
        override fun observeActiveReviews(): Flow<List<Review>> = review.map { listOfNotNull(it) }

        override fun observeByStatus(status: ReviewStatus): Flow<List<Review>> = review.map { r -> listOfNotNull(r).filter { it.status == status } }

        override fun observeInProgress(): Flow<List<Review>> = observeByStatus(ReviewStatus.IN_PROGRESS)

        override fun observeReview(id: String): Flow<Review?> = review.map { if (it?.id == id) it else null }

        override suspend fun getReview(id: String): Review? = review.value?.takeIf { it.id == id }
    }

    fun asHouseholdRepository(): HouseholdRepository = object : HouseholdRepository {
        override fun observeMembers(reviewId: String): Flow<List<HouseholdMember>> = members

        override suspend fun getMembers(reviewId: String): List<HouseholdMember> = members.value

        override suspend fun saveMember(
            member: HouseholdMember,
            expectedRevision: Long,
            focusUpdate: FocusUpdate,
        ): DomainResult<Review> {
            members.update { list ->
                val without = list.filterNot { it.id == member.id }
                without + member
            }
            return bumpRevision(expectedRevision)
        }

        override suspend fun removeMember(
            memberId: String,
            reviewId: String,
            expectedRevision: Long,
        ): DomainResult<Review> {
            members.update { it.filterNot { m -> m.id == memberId } }
            return bumpRevision(expectedRevision)
        }
    }

    fun asResponsibilityRepository(writeLog: MutableList<String>): ResponsibilityRepository = object : ResponsibilityRepository {
        override fun observeResponsibilities(reviewId: String): Flow<List<Responsibility>> = responsibilities

        override suspend fun getResponsibilities(reviewId: String): List<Responsibility> = responsibilities.value

        override suspend fun saveResponsibility(
            responsibility: Responsibility,
            expectedRevision: Long,
        ): DomainResult<Review> {
            if (forceConflictOnce) {
                forceConflictOnce = false
                val bumped =
                    review.value!!.copy(revision = review.value!!.revision + 4)
                review.value = bumped
                return DomainResult.failure(
                    DomainError.Conflict("concurrent write", currentRevision = bumped.revision),
                )
            }
            writeLog += "resp@$expectedRevision"
            responsibilities.update { list ->
                val without = list.filterNot { it.id == responsibility.id }
                without + responsibility
            }
            return bumpRevision(expectedRevision)
        }

        override suspend fun removeResponsibility(
            responsibilityId: String,
            reviewId: String,
            expectedRevision: Long,
        ): DomainResult<Review> {
            responsibilities.update { it.filterNot { r -> r.id == responsibilityId } }
            return bumpRevision(expectedRevision)
        }
    }
}
