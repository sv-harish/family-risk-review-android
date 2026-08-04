package com.familyriskreview.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.familyriskreview.core.calculation.ResponsibilityCalculator
import com.familyriskreview.core.data.repository.DefaultCalculationSnapshotRepository
import com.familyriskreview.core.data.repository.DefaultHouseholdRepository
import com.familyriskreview.core.data.repository.DefaultResponsibilityRepository
import com.familyriskreview.core.data.repository.DefaultReviewRepository
import com.familyriskreview.core.data.usecase.AdvanceReviewStepUseCase
import com.familyriskreview.core.database.FamilyRiskReviewDatabase
import com.familyriskreview.core.model.AppLanguage
import com.familyriskreview.core.model.CalculationAssumptions
import com.familyriskreview.core.model.CalculationSnapshot
import com.familyriskreview.core.model.ContributionStatus
import com.familyriskreview.core.model.DependencyStatus
import com.familyriskreview.core.model.FamilyMemberType
import com.familyriskreview.core.model.FocusUpdate
import com.familyriskreview.core.model.HouseholdMember
import com.familyriskreview.core.model.Responsibility
import com.familyriskreview.core.model.ResponsibilityCatalogue
import com.familyriskreview.core.model.ReviewMode
import com.familyriskreview.core.model.ReviewStep
import com.familyriskreview.core.model.ScenarioKind
import com.familyriskreview.core.model.result.DomainError
import com.familyriskreview.core.model.result.DomainResult
import com.familyriskreview.core.model.service.Clock
import com.familyriskreview.core.model.service.IdGenerator
import com.familyriskreview.core.model.service.ReviewNumberProvider
import com.familyriskreview.core.sync.FakeSyncClient
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Phase11HardeningRobolectricTest {
    private lateinit var db: FamilyRiskReviewDatabase
    private lateinit var syncClient: FakeSyncClient
    private lateinit var reviews: DefaultReviewRepository
    private lateinit var household: DefaultHouseholdRepository
    private lateinit var responsibilities: DefaultResponsibilityRepository
    private lateinit var snapshots: DefaultCalculationSnapshotRepository
    private lateinit var advance: AdvanceReviewStepUseCase
    private var nowMs = 1_700_000_000_000L
    private var idSeq = 0
    private var numberSeq = 0

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db =
            Room
                .inMemoryDatabaseBuilder(context, FamilyRiskReviewDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        syncClient = FakeSyncClient()
        val clock =
            object : Clock {
                override fun now(): Instant = Instant.fromEpochMilliseconds(nowMs)
            }
        val ids =
            object : IdGenerator {
                override fun newId(): String = "id-${++idSeq}"
            }
        val numbers =
            object : ReviewNumberProvider {
                override fun generate(now: Instant): String = "FRR-20260101-${(++numberSeq).toString().padStart(6, '0')}"
            }
        reviews =
            DefaultReviewRepository(
                db = db,
                reviewDao = db.reviewDao(),
                syncClient = syncClient,
                clock = clock,
                idGenerator = ids,
                reviewNumberProvider = numbers,
            )
        household =
            DefaultHouseholdRepository(
                db = db,
                dao = db.householdMemberDao(),
                reviewDao = db.reviewDao(),
                syncClient = syncClient,
                clock = clock,
            )
        responsibilities =
            DefaultResponsibilityRepository(
                db = db,
                dao = db.responsibilityDao(),
                reviewDao = db.reviewDao(),
                syncClient = syncClient,
                clock = clock,
            )
        snapshots =
            DefaultCalculationSnapshotRepository(
                db = db,
                snapshotDao = db.calculationSnapshotDao(),
                reviewDao = db.reviewDao(),
                syncClient = syncClient,
                clock = clock,
            )
        advance =
            AdvanceReviewStepUseCase(
                reviewRepository = reviews,
                reviewWriter = reviews,
                householdRepository = household,
                responsibilityRepository = responsibilities,
                snapshotRepository = snapshots,
            )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun staleHouseholdSave_rollsBackAndDoesNotEnqueueSync() = runBlocking {
        val review = createQuick()
        syncClient.clearIntents()
        val member = selfMember(review.id, "m-new")
        val result =
            household.saveMember(
                member,
                expectedRevision = 99L,
                focusUpdate = FocusUpdate.Unchanged,
            )
        assertThat(result).isInstanceOf(DomainResult.Failure::class.java)
        assertThat((result as DomainResult.Failure).error).isInstanceOf(DomainError.Conflict::class.java)
        assertThat(db.householdMemberDao().getById("m-new")).isNull()
        assertThat(syncClient.pendingUpsertIds()).isEmpty()
        assertThat(reviews.getReview(review.id)!!.revision).isEqualTo(1L)
    }

    @Test
    fun staleHouseholdRemoval_rollsBack() = runBlocking {
        val review = createQuick()
        val member = selfMember(review.id, "m1")
        val after =
            (
                household.saveMember(member, review.revision, FocusUpdate.Set("m1"))
                    as DomainResult.Success
                ).value
        syncClient.clearIntents()
        val result = household.removeMember("m1", review.id, expectedRevision = 1L)
        assertThat(result).isInstanceOf(DomainResult.Failure::class.java)
        assertThat(db.householdMemberDao().getById("m1")).isNotNull()
        assertThat(syncClient.pendingUpsertIds()).isEmpty()
        assertThat(reviews.getReview(review.id)!!.revision).isEqualTo(after.revision)
    }

    @Test
    fun staleResponsibilitySave_rollsBack() = runBlocking {
        val review = createQuick()
        syncClient.clearIntents()
        val result =
            responsibilities.saveResponsibility(
                Responsibility(
                    id = "r-stale",
                    reviewId = review.id,
                    catalogue = ResponsibilityCatalogue.ESSENTIAL_FAMILY_LIVING_EXPENSES,
                    isSelected = true,
                ),
                expectedRevision = 42L,
            )
        assertThat(result).isInstanceOf(DomainResult.Failure::class.java)
        assertThat(db.responsibilityDao().getById("r-stale")).isNull()
        assertThat(syncClient.pendingUpsertIds()).isEmpty()
    }

    @Test
    fun staleResponsibilityRemoval_rollsBack() = runBlocking {
        val review = createQuick()
        val saved =
            (
                responsibilities.saveResponsibility(
                    Responsibility(
                        id = "r1",
                        reviewId = review.id,
                        catalogue = ResponsibilityCatalogue.ESSENTIAL_FAMILY_LIVING_EXPENSES,
                        isSelected = true,
                    ),
                    review.revision,
                ) as DomainResult.Success
                ).value
        syncClient.clearIntents()
        val result = responsibilities.removeResponsibility("r1", review.id, expectedRevision = 1L)
        assertThat(result).isInstanceOf(DomainResult.Failure::class.java)
        assertThat(db.responsibilityDao().getById("r1")).isNotNull()
        assertThat(syncClient.pendingUpsertIds()).isEmpty()
        assertThat(reviews.getReview(review.id)!!.revision).isEqualTo(saved.revision)
    }

    @Test
    fun staleSnapshotSave_rollsBack() = runBlocking {
        val review = createQuick()
        syncClient.clearIntents()
        val result =
            snapshots.saveSnapshot(
                snapshot =
                CalculationSnapshot(
                    id = "snap-1",
                    reviewId = review.id,
                    reviewRevision = 99,
                    calculationInputRevision = review.calculationInputRevision,
                    assumptionVersion = "1.1.0",
                    calculationVersion = "1.1.0",
                    scenarioKind = ScenarioKind.BASE,
                    assumptionsJson = CalculationAssumptions.Default.toJson(),
                    mustContinueTotalRupees = 1,
                    adjustableTotalRupees = 0,
                    postponedTotalRupees = 0,
                    perResponsibilityJson = "[]",
                    generatedAtEpochMs = nowMs,
                ),
                expectedReviewRevision = 99L,
            )
        assertThat(result).isInstanceOf(DomainResult.Failure::class.java)
        assertThat(db.calculationSnapshotDao().getLatest(review.id)).isNull()
        assertThat(syncClient.pendingUpsertIds()).isEmpty()
    }

    @Test
    fun successfulChildWrite_updatesChildAndParentTogether() = runBlocking {
        val review = createQuick()
        syncClient.clearIntents()
        val updated =
            (
                household.saveMember(
                    selfMember(review.id, "m1"),
                    review.revision,
                    FocusUpdate.Set("m1"),
                ) as DomainResult.Success
                ).value
        assertThat(db.householdMemberDao().getById("m1")).isNotNull()
        assertThat(updated.revision).isEqualTo(2L)
        assertThat(updated.calculationInputRevision).isEqualTo(2L)
        assertThat(updated.summaryStale).isTrue()
        assertThat(syncClient.pendingUpsertIds()).contains(review.id)
    }

    @Test
    fun advanceFromEmptyHousehold_blocked() = runBlocking {
        val review = createQuick()
        val result = advance(review.id, review.revision, forward = true)
        assertThat(result).isInstanceOf(DomainResult.Failure::class.java)
        assertThat((result as DomainResult.Failure).error).isInstanceOf(DomainError.Validation::class.java)
        assertThat(reviews.getReview(review.id)!!.currentStep)
            .isEqualTo(ReviewStep.HOUSEHOLD_SUPPORT_MAP)
    }

    @Test
    fun completedReview_rejectsChildEdits_untilReopened() = runBlocking {
        val review = createQuick()
        val completed =
            (
                reviews.completeReview(review.id, review.revision, customerAcknowledged = true)
                    as DomainResult.Success
                ).value
        val save =
            household.saveMember(
                selfMember(review.id, "m1"),
                completed.revision,
                FocusUpdate.Unchanged,
            )
        assertThat(save).isInstanceOf(DomainResult.Failure::class.java)
        assertThat(db.householdMemberDao().getById("m1")).isNull()
        val reopened =
            (reviews.reopenReview(completed.id, completed.revision) as DomainResult.Success).value
        val ok =
            household.saveMember(
                selfMember(review.id, "m1"),
                reopened.revision,
                FocusUpdate.Set("m1"),
            )
        assertThat(ok).isInstanceOf(DomainResult.Success::class.java)
    }

    @Test
    fun archivedReview_rejectsChildEdits() = runBlocking {
        val review = createQuick()
        val archived =
            (reviews.archiveReview(review.id, review.revision) as DomainResult.Success).value
        val result =
            responsibilities.saveResponsibility(
                Responsibility(
                    id = "r1",
                    reviewId = review.id,
                    catalogue = ResponsibilityCatalogue.ESSENTIAL_FAMILY_LIVING_EXPENSES,
                ),
                archived.revision,
            )
        assertThat(result).isInstanceOf(DomainResult.Failure::class.java)
        assertThat(db.responsibilityDao().getById("r1")).isNull()
    }

    @Test
    fun focusCannotBecomeNonContributor() = runBlocking {
        val review = createQuick()
        val after =
            (
                household.saveMember(
                    selfMember(review.id, "m1"),
                    review.revision,
                    FocusUpdate.Set("m1"),
                ) as DomainResult.Success
                ).value
        val result =
            household.saveMember(
                selfMember(review.id, "m1").copy(
                    contributionStatus = ContributionStatus.NON_CONTRIBUTOR,
                ),
                after.revision,
                FocusUpdate.Unchanged,
            )
        assertThat(result).isInstanceOf(DomainResult.Failure::class.java)
        assertThat(
            household.getMembers(review.id).single().contributionStatus,
        ).isEqualTo(ContributionStatus.PRIMARY_INCOME)
    }

    @Test
    fun focusFromAnotherReview_rejected() = runBlocking {
        val review = createQuick()
        val result =
            household.saveMember(
                selfMember(review.id, "m1"),
                review.revision,
                FocusUpdate.Set("foreign-member"),
            )
        assertThat(result).isInstanceOf(DomainResult.Failure::class.java)
        assertThat(db.householdMemberDao().getById("m1")).isNull()
    }

    @Test
    fun clearingFocus_allowedAtomically() = runBlocking {
        val review = createQuick()
        val after =
            (
                household.saveMember(
                    selfMember(review.id, "m1"),
                    review.revision,
                    FocusUpdate.Set("m1"),
                ) as DomainResult.Success
                ).value
        val cleared =
            (
                household.saveMember(
                    selfMember(review.id, "m1"),
                    after.revision,
                    FocusUpdate.Clear,
                ) as DomainResult.Success
                ).value
        assertThat(cleared.focusedIncomeContributorId).isNull()
    }

    @Test
    fun archiveDoesNotStaleFreshSummaryFingerprint() = runBlocking {
        val review = createQuick()
        val afterMember =
            (
                household.saveMember(
                    selfMember(review.id, "m1"),
                    review.revision,
                    FocusUpdate.Set("m1"),
                ) as DomainResult.Success
                ).value
        // Persist a snapshot matching current input revision.
        val snapSaved =
            (
                snapshots.saveSnapshot(
                    CalculationSnapshot(
                        id = "s1",
                        reviewId = review.id,
                        reviewRevision = afterMember.revision + 1,
                        calculationInputRevision = afterMember.calculationInputRevision,
                        assumptionVersion = afterMember.assumptionVersion,
                        calculationVersion = afterMember.calculationVersion,
                        scenarioKind = ScenarioKind.BASE,
                        assumptionsJson = afterMember.assumptionsJson,
                        mustContinueTotalRupees = 0,
                        adjustableTotalRupees = 0,
                        postponedTotalRupees = 0,
                        perResponsibilityJson = "[]",
                        generatedAtEpochMs = nowMs,
                    ),
                    expectedReviewRevision = afterMember.revision,
                ) as DomainResult.Success
                ).value
        assertThat(snapSaved.summaryStale).isFalse()
        assertThat(
            snapshots.isStale(snapSaved, ResponsibilityCalculator.CALCULATION_VERSION),
        ).isFalse()
        val archived =
            (reviews.archiveReview(snapSaved.id, snapSaved.revision) as DomainResult.Success).value
        // Archive bumps aggregate revision but not calculationInputRevision / summaryStale.
        assertThat(archived.calculationInputRevision).isEqualTo(snapSaved.calculationInputRevision)
        assertThat(archived.summaryStale).isFalse()
        assertThat(
            snapshots.isStale(archived, ResponsibilityCalculator.CALCULATION_VERSION),
        ).isFalse()
    }

    @Test
    fun malformedAssumptions_failClosed() {
        val bad = CalculationAssumptions.parseStored("{not-json")
        assertThat(bad).isInstanceOf(DomainResult.Failure::class.java)
        val err = (bad as DomainResult.Failure).error as DomainError.CorruptData
        assertThat(err.code).isEqualTo("INVALID_ASSUMPTION_SNAPSHOT")

        val unsupported =
            CalculationAssumptions.parseStored(
                """{"version":"9.9.9","educationInflation":800,"marriageInflation":600,"expenseInflation":600,"recurringSupportInflation":600}""",
            )
        assertThat(
            ((unsupported as DomainResult.Failure).error as DomainError.CorruptData).code,
        ).isEqualTo("UNSUPPORTED_ASSUMPTION_VERSION")

        val ok = CalculationAssumptions.parseStored(CalculationAssumptions.Default.toJson())
        assertThat(ok).isInstanceOf(DomainResult.Success::class.java)
    }

    @Test
    fun classifyConstraint_distinguishesReviewNumber() {
        val number =
            DefaultReviewRepository.classifyConstraint(
                android.database.sqlite.SQLiteConstraintException(
                    "UNIQUE constraint failed: reviews.reviewNumber",
                ),
            )
        assertThat(number).isEqualTo(DefaultReviewRepository.ConstraintKind.REVIEW_NUMBER)
        val other =
            DefaultReviewRepository.classifyConstraint(
                android.database.sqlite.SQLiteConstraintException(
                    "UNIQUE constraint failed: something_else",
                ),
            )
        assertThat(other).isEqualTo(DefaultReviewRepository.ConstraintKind.OTHER)
    }

    private suspend fun createQuick() = (reviews.createReview(ReviewMode.QUICK, AppLanguage.ENGLISH) as DomainResult.Success).value

    private fun selfMember(
        reviewId: String,
        id: String,
    ) = HouseholdMember(
        id = id,
        reviewId = reviewId,
        relationship = FamilyMemberType.SELF,
        age = 40,
        contributionStatus = ContributionStatus.PRIMARY_INCOME,
        dependencyStatus = DependencyStatus.NOT_APPLICABLE,
    )
}
