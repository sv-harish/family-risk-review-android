package com.familyriskreview.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.familyriskreview.core.data.repository.DefaultHouseholdRepository
import com.familyriskreview.core.data.repository.DefaultResponsibilityRepository
import com.familyriskreview.core.data.repository.DefaultReviewRepository
import com.familyriskreview.core.database.FamilyRiskReviewDatabase
import com.familyriskreview.core.model.AppLanguage
import com.familyriskreview.core.model.ContributionStatus
import com.familyriskreview.core.model.DependencyStatus
import com.familyriskreview.core.model.FamilyMemberType
import com.familyriskreview.core.model.HouseholdMember
import com.familyriskreview.core.model.Responsibility
import com.familyriskreview.core.model.ResponsibilityCatalogue
import com.familyriskreview.core.model.ReviewMode
import com.familyriskreview.core.model.ReviewStatus
import com.familyriskreview.core.model.ReviewStep
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
class ReviewRepositoryRobolectricTest {
    private lateinit var db: FamilyRiskReviewDatabase
    private lateinit var repository: DefaultReviewRepository
    private lateinit var householdRepository: DefaultHouseholdRepository
    private lateinit var responsibilityRepository: DefaultResponsibilityRepository
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
        repository =
            DefaultReviewRepository(
                db = db,
                reviewDao = db.reviewDao(),
                syncClient = FakeSyncClient(),
                clock = clock,
                idGenerator = ids,
                reviewNumberProvider = numbers,
            )
        householdRepository =
            DefaultHouseholdRepository(
                db = db,
                dao = db.householdMemberDao(),
                reviewDao = db.reviewDao(),
                syncClient = FakeSyncClient(),
                clock = clock,
            )
        responsibilityRepository =
            DefaultResponsibilityRepository(
                db = db,
                dao = db.responsibilityDao(),
                reviewDao = db.reviewDao(),
                syncClient = FakeSyncClient(),
                clock = clock,
            )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun createReview_persistsModeAndStartsAtHouseholdStep() = runBlocking {
        val quick =
            (repository.createReview(ReviewMode.QUICK, AppLanguage.ENGLISH) as DomainResult.Success).value
        val guided =
            (repository.createReview(ReviewMode.GUIDED, AppLanguage.TAMIL) as DomainResult.Success).value
        assertThat(quick.mode).isEqualTo(ReviewMode.QUICK)
        assertThat(guided.mode).isEqualTo(ReviewMode.GUIDED)
        assertThat(quick.currentStep).isEqualTo(ReviewStep.HOUSEHOLD_SUPPORT_MAP)
        assertThat(guided.language).isEqualTo(AppLanguage.TAMIL)
        assertThat(repository.getReview(quick.id)?.reviewNumber).isEqualTo(quick.reviewNumber)
        assertThat(quick.reviewNumber).isNotEqualTo(guided.reviewNumber)
        assertThat(quick.summaryStale).isTrue()
    }

    @Test
    fun casUpdate_conflictsOnStaleRevision() = runBlocking {
        val created =
            (repository.createReview(ReviewMode.QUICK, AppLanguage.ENGLISH) as DomainResult.Success).value
        val first = repository.advanceStep(created.id, created.revision, ReviewStep.RESPONSIBILITIES)
        assertThat(first).isInstanceOf(DomainResult.Success::class.java)
        val conflict = repository.advanceStep(created.id, created.revision, ReviewStep.PRIORITISATION)
        assertThat(conflict).isInstanceOf(DomainResult.Failure::class.java)
        assertThat((conflict as DomainResult.Failure).error).isInstanceOf(DomainError.Conflict::class.java)
    }

    @Test
    fun archiveStoresPriorStatus_andRestoreReturnsIt() = runBlocking {
        val created =
            (repository.createReview(ReviewMode.GUIDED, AppLanguage.ENGLISH) as DomainResult.Success).value
        val completed =
            (
                repository.completeReview(created.id, created.revision, customerAcknowledged = true)
                    as DomainResult.Success
                ).value
        assertThat(completed.status).isEqualTo(ReviewStatus.COMPLETED)
        val archived =
            (repository.archiveReview(completed.id, completed.revision) as DomainResult.Success).value
        assertThat(archived.status).isEqualTo(ReviewStatus.ARCHIVED)
        assertThat(archived.statusBeforeArchive).isEqualTo(ReviewStatus.COMPLETED)
        val restored =
            (repository.restoreReview(archived.id, archived.revision) as DomainResult.Success).value
        assertThat(restored.status).isEqualTo(ReviewStatus.COMPLETED)
    }

    @Test
    fun softDelete_retainsTombstoneAndChildren() = runBlocking {
        val created =
            (repository.createReview(ReviewMode.QUICK, AppLanguage.ENGLISH) as DomainResult.Success).value
        val member =
            HouseholdMember(
                id = "m1",
                reviewId = created.id,
                relationship = FamilyMemberType.SELF,
                age = 40,
                contributionStatus = ContributionStatus.PRIMARY_INCOME,
                dependencyStatus = DependencyStatus.NOT_APPLICABLE,
            )
        val afterMember =
            (
                householdRepository.saveMember(member, focusedIncomeContributorId = "m1", expectedRevision = created.revision)
                    as DomainResult.Success
                ).value
        val deleted =
            (repository.softDeleteReview(created.id, afterMember.revision) as DomainResult.Success).value
        assertThat(deleted.status).isEqualTo(ReviewStatus.DELETED)
        assertThat(repository.getReview(created.id)?.status).isEqualTo(ReviewStatus.DELETED)
        assertThat(householdRepository.getMembers(created.id)).hasSize(1)
    }

    @Test
    fun childWrite_bumpsParentRevisionAndMarksSummaryStale() = runBlocking {
        val created =
            (repository.createReview(ReviewMode.QUICK, AppLanguage.ENGLISH) as DomainResult.Success).value
        val responsibility =
            Responsibility(
                id = "resp-1",
                reviewId = created.id,
                catalogue = ResponsibilityCatalogue.ESSENTIAL_FAMILY_LIVING_EXPENSES,
                isSelected = true,
            )
        val updated =
            (
                responsibilityRepository.saveResponsibility(responsibility, created.revision)
                    as DomainResult.Success
                ).value
        assertThat(updated.revision).isEqualTo(created.revision + 1)
        assertThat(updated.summaryStale).isTrue()
    }
}
