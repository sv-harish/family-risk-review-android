package com.familyriskreview.core.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Root aggregate for a Family Risk Review session.
 *
 * [revision] bumps on every aggregate write (including non-financial status changes).
 * [calculationInputRevision] bumps only when calculation inputs change; snapshots
 * bind to that fingerprint so archive/step advances do not falsely stale a summary.
 *
 * [calculationVersion] has no misleading default — callers must supply the
 * authoritative engine version from `:core:calculation` at construction time.
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
    val calculationVersion: String,
    val syncState: SyncState = SyncState.LOCAL_ONLY,
    val revision: Long = 1L,
    /** Fingerprint of calculation inputs; independent of non-financial revisions. */
    val calculationInputRevision: Long = 1L,
    val customerAcknowledged: Boolean = false,
    val statusBeforeArchive: ReviewStatus? = null,
    val summaryStale: Boolean = true,
    val assumptionsJson: String = CalculationAssumptions.Default.toJson(),
)

@Serializable
data class AdvisorReference(
    val reviewId: String,
    val customerInitialsOrNickname: String? = null,
    val crmReference: String? = null,
    val privateNote: String? = null,
)

object AdvisorIdentity {
    const val DISPLAY_NAME: String = "S V Harish"
    const val TITLE: String = "Certified Insurance Planner"
    const val CREDENTIAL: String = "LUGI CIP Completed"

    // TODO(production): Verify final formal credential wording before public release.
}

@Serializable
data class CustomerSummaryProjection(
    val reviewId: String,
    val reviewNumber: String,
    val mode: ReviewMode,
    val language: AppLanguage,
)

/** How a household write updates the focused income contributor. */
sealed interface FocusUpdate {
    data object Unchanged : FocusUpdate

    data object Clear : FocusUpdate

    data class Set(val memberId: String) : FocusUpdate
}
