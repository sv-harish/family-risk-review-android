package com.familyriskreview.core.data

import com.familyriskreview.core.model.AdvisorReference
import com.familyriskreview.core.model.CustomerSummaryProjection
import com.familyriskreview.core.model.Review

/**
 * Maps domain state to customer-facing summary fields.
 *
 * Advisor-only fields (initials/nickname, CRM reference, private notes) are
 * never projected into customer summaries or PDFs.
 */
object CustomerSummaryMapper {
    @Suppress("UNUSED_PARAMETER")
    fun project(
        review: Review,
        advisorReference: AdvisorReference?,
    ): CustomerSummaryProjection = CustomerSummaryProjection(
        reviewId = review.id,
        reviewNumber = review.reviewNumber,
        mode = review.mode,
        language = review.language,
    )
}
