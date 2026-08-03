package com.familyriskreview.core.data

import com.familyriskreview.core.model.AdvisorReference
import com.familyriskreview.core.model.CustomerSummaryProjection
import com.familyriskreview.core.model.Review

/**
 * Maps domain state to customer-facing summary fields.
 * Advisor-only CRM references and private notes are never included.
 * Initials/nickname appear only when explicitly opted in.
 */
object CustomerSummaryMapper {
    fun project(
        review: Review,
        advisorReference: AdvisorReference?,
    ): CustomerSummaryProjection {
        val label =
            advisorReference
                ?.takeIf { it.includeInCustomerSummary }
                ?.customerInitialsOrNickname
        return CustomerSummaryProjection(
            reviewId = review.id,
            reviewNumber = review.reviewNumber,
            mode = review.mode,
            language = review.language,
            customerDisplayLabel = label,
        )
    }
}
