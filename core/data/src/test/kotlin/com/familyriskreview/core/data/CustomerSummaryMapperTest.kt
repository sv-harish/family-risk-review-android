package com.familyriskreview.core.data

import com.familyriskreview.core.model.AdvisorReference
import com.familyriskreview.core.model.AppLanguage
import com.familyriskreview.core.model.Review
import com.familyriskreview.core.model.ReviewMode
import com.familyriskreview.core.model.ReviewStatus
import com.familyriskreview.core.model.ReviewStep
import com.google.common.truth.Truth.assertThat
import kotlinx.datetime.Instant
import org.junit.Test

class CustomerSummaryMapperTest {
    private val review =
        Review(
            id = "id-1",
            reviewNumber = "FRR-1",
            mode = ReviewMode.QUICK,
            language = AppLanguage.ENGLISH,
            status = ReviewStatus.COMPLETED,
            currentStep = ReviewStep.AWARENESS_SUMMARY,
            createdAt = Instant.fromEpochMilliseconds(1),
            updatedAt = Instant.fromEpochMilliseconds(1),
        )

    @Test
    fun excludesAdvisorOnlyFieldsByDefault() {
        val ref =
            AdvisorReference(
                reviewId = "id-1",
                customerInitialsOrNickname = "RK",
                crmReference = "CRM",
                privateNote = "secret",
                includeInCustomerSummary = false,
            )
        val projection = CustomerSummaryMapper.project(review, ref)
        assertThat(projection.customerDisplayLabel).isNull()
        assertThat(projection.reviewNumber).isEqualTo("FRR-1")
    }

    @Test
    fun includesInitialsOnlyWhenOptedIn() {
        val ref =
            AdvisorReference(
                reviewId = "id-1",
                customerInitialsOrNickname = "RK",
                crmReference = "CRM",
                privateNote = "secret",
                includeInCustomerSummary = true,
            )
        val projection = CustomerSummaryMapper.project(review, ref)
        assertThat(projection.customerDisplayLabel).isEqualTo("RK")
    }
}
