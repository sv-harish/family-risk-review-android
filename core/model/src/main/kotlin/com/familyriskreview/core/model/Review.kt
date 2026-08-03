package com.familyriskreview.core.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Root aggregate for a Family Risk Review session.
 *
 * Customer PII is intentionally excluded. Advisor-only references must not
 * automatically appear in the customer-facing summary.
 */
@Serializable
data class Review(
    val id: String,
    val reviewNumber: String,
    val mode: ReviewMode,
    val language: AppLanguage,
    val status: ReviewStatus,
    val currentStep: ReviewStep,
    val createdAt: Instant,
    val updatedAt: Instant,
    val completedAt: Instant? = null,
    val focusedIncomeContributorId: String? = null,
    val assumptionVersion: String = CalculationAssumptions.CURRENT_VERSION,
    val calculationVersion: String = "1.0.0",
    val syncState: SyncState = SyncState.LOCAL_ONLY,
    val revision: Long = 1L,
    val customerAcknowledged: Boolean = false,
)

/**
 * Advisor-only metadata. Never auto-include in customer PDFs/summaries.
 */
@Serializable
data class AdvisorReference(
    val reviewId: String,
    val customerInitialsOrNickname: String? = null,
    val crmReference: String? = null,
    val privateNote: String? = null,
    val includeInCustomerSummary: Boolean = false,
)

/**
 * Documented production TODO: verify final formal credential wording before public release.
 * Do not silently change this supplied wording.
 */
object AdvisorIdentity {
    const val DISPLAY_NAME: String = "S V Harish"
    const val TITLE: String = "Certified Insurance Planner"
    const val CREDENTIAL: String = "LUGI CIP Completed"

    // TODO(production): Verify final formal credential wording before public release.
    // Supplied wording is intentionally preserved until product/legal confirmation.
}
