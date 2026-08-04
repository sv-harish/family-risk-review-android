package com.familyriskreview.feature.dashboard

import com.familyriskreview.core.model.AppLanguage
import com.familyriskreview.core.model.Review
import com.familyriskreview.core.model.ReviewMode
import com.familyriskreview.core.model.ReviewStatus
import com.familyriskreview.core.model.ReviewStep
import com.familyriskreview.core.model.SyncState
import com.familyriskreview.core.model.result.DomainError
import com.google.common.truth.Truth.assertThat
import kotlinx.datetime.Instant
import org.junit.Test

class DashboardViewModelGroupingTest {
    @Test
    fun group_keepsArchivedSeparateFromActiveSections() {
        val inProgress =
            listOf(
                review(
                    id = "ip-1",
                    number = "FRR-20260101-000001",
                    status = ReviewStatus.IN_PROGRESS,
                    step = ReviewStep.HOUSEHOLD_SUPPORT_MAP,
                    updatedAtMs = 1_000,
                ),
            )
        val completed =
            listOf(
                review(
                    id = "done-1",
                    number = "FRR-20260101-000002",
                    status = ReviewStatus.COMPLETED,
                    step = ReviewStep.AWARENESS_SUMMARY,
                    updatedAtMs = 2_000,
                ),
            )
        val archived =
            listOf(
                review(
                    id = "arch-1",
                    number = "FRR-20260101-000003",
                    status = ReviewStatus.ARCHIVED,
                    step = ReviewStep.RESPONSIBILITIES,
                    updatedAtMs = 3_000,
                ),
            )

        val (progressItems, completedItems, archivedItems) =
            DashboardReviewGrouping.group(inProgress, completed, archived)

        assertThat(progressItems).hasSize(1)
        assertThat(progressItems[0].reviewNumber).isEqualTo("FRR-20260101-000001")
        assertThat(progressItems[0].stage).isEqualTo(ReviewStep.HOUSEHOLD_SUPPORT_MAP)
        assertThat(progressItems.map { it.id }).doesNotContain("arch-1")

        assertThat(completedItems).hasSize(1)
        assertThat(completedItems[0].reviewNumber).isEqualTo("FRR-20260101-000002")
        assertThat(completedItems.map { it.id }).doesNotContain("arch-1")

        assertThat(archivedItems).hasSize(1)
        assertThat(archivedItems[0].reviewNumber).isEqualTo("FRR-20260101-000003")
        assertThat(archivedItems[0].id).isEqualTo("arch-1")
    }

    @Test
    fun toItem_mapsReviewNumberModeAndStage_withoutExposingRawIdInLabels() {
        val item =
            DashboardReviewGrouping.toItem(
                review(
                    id = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
                    number = "FRR-20260804-000042",
                    status = ReviewStatus.IN_PROGRESS,
                    mode = ReviewMode.GUIDED,
                    step = ReviewStep.PRIORITISATION,
                    updatedAtMs = 5_000,
                ),
            )

        assertThat(item.reviewNumber).isEqualTo("FRR-20260804-000042")
        assertThat(item.mode).isEqualTo(ReviewMode.GUIDED)
        assertThat(item.stage).isEqualTo(ReviewStep.PRIORITISATION)
        assertThat(item.updatedAtLabel).isNotEmpty()
        // id is retained for navigation only; UI labels use reviewNumber
        assertThat(item.id).isEqualTo("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee")
        assertThat(item.reviewNumber).doesNotContain("aaaaaaaa")
    }

    @Test
    fun userMessage_surfacesDomainErrorText() {
        val message =
            DashboardReviewGrouping.userMessage(
                DomainError.Persistence("Disk full while creating review"),
            )
        assertThat(message).isEqualTo("Disk full while creating review")
    }

    private fun review(
        id: String,
        number: String,
        status: ReviewStatus,
        step: ReviewStep,
        updatedAtMs: Long,
        mode: ReviewMode = ReviewMode.QUICK,
    ): Review =
        Review(
            id = id,
            reviewNumber = number,
            mode = mode,
            language = AppLanguage.ENGLISH,
            status = status,
            currentStep = step,
            createdAt = Instant.fromEpochMilliseconds(updatedAtMs),
            updatedAt = Instant.fromEpochMilliseconds(updatedAtMs),
            calculationVersion = "test",
            syncState = SyncState.LOCAL_ONLY,
        )
}
