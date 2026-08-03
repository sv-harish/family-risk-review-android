package com.familyriskreview.core.model

import kotlinx.serialization.Serializable

/**
 * Preferred language for the customer-facing review and generated summary.
 * Persisted for the lifetime of a review session.
 */
@Serializable
enum class AppLanguage {
    ENGLISH,
    TAMIL,
    HINDI,
}

/**
 * Review pacing mode. Durations are targets for facilitation, not hard limits.
 */
@Serializable
enum class ReviewMode {
    /** Target facilitation window: 5–8 minutes. */
    QUICK,

    /** Target facilitation window: 10–15 minutes. */
    GUIDED,
}

@Serializable
enum class ReviewStatus {
    IN_PROGRESS,
    COMPLETED,
    ARCHIVED,
    DELETED,
}

@Serializable
enum class SyncState {
    LOCAL_ONLY,
    PENDING,
    SYNCING,
    SYNCED,
    CONFLICT,
    ERROR,
}

/**
 * Persisted customer-journey steps for an in-progress review.
 *
 * App-shell destinations (Splash, Welcome) are NOT review progress and must not
 * appear here. A Review is created only after Quick/Guided mode is selected.
 */
@Serializable
enum class ReviewStep {
    HOUSEHOLD_SUPPORT_MAP,
    RESPONSIBILITIES,
    PRIORITISATION,
    RESPONSIBILITY_DETAILS,
    TIMELINE,
    INCOME_RISK_EDUCATION,
    KEY_REALIZATION,
    GROSS_RESPONSIBILITY,
    EDUCATIONAL_COMPARISON,
    AWARENESS_SUMMARY,
    ADVISOR_HANDOFF,
}
