package com.familyriskreview.core.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Root aggregate for a Family Risk Review session.
 *
 * Customer PII is intentionally excluded. Advisor-only references must never
 * appear in the customer-facing summary.
 *
 * A Review is created only after Quick or Guided mode is selected.
 * Splash / Welcome are app-shell destinations, not [currentStep] values.
 *
 * [revision] is the optimistic-concurrency token for the aggregate. Child
 * mutations must bump it transactionally with [updatedAt], sync pending, and
 * [summaryStale]=true when calculation inputs change.
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
    /** Authoritative value is [com.familyriskreview.core.calculation.ResponsibilityCalculator.CALCULATION_VERSION]. */
    val calculationVersion: String = CalculationAssumptions.CURRENT_VERSION,
    val syncState: SyncState = SyncState.LOCAL_ONLY,
    val revision: Long = 1L,
    val customerAcknowledged: Boolean = false,
    /** Prior meaningful status captured when archiving (IN_PROGRESS or COMPLETED). */
    val statusBeforeArchive: ReviewStatus? = null,
    /** True when inputs changed since the last persisted calculation snapshot. */
    val summaryStale: Boolean = true,
    /** Serialised [CalculationAssumptions] snapshot bound to this review. */
    val assumptionsJson: String = CalculationAssumptions.Default.toJson(),
)

/**
 * Advisor-only metadata. Never included in customer PDFs/summaries.
 */
@Serializable
data class AdvisorReference(
    val reviewId: String,
    val customerInitialsOrNickname: String? = null,
    val crmReference: String? = null,
    val privateNote: String? = null,
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

/**
 * Customer-summary projection. Advisor-only fields are never projected here.
 */
@Serializable
data class CustomerSummaryProjection(
    val reviewId: String,
    val reviewNumber: String,
    val mode: ReviewMode,
    val language: AppLanguage,
)
