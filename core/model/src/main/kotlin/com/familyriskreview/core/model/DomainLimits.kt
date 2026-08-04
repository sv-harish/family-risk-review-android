package com.familyriskreview.core.model

/**
 * Supported calculation input bounds. Enforced in domain validation before
 * the calculator runs — not only in UI.
 */
object DomainLimits {
    /** Max one-time amount: ₹1,00,000 crore (1e12). */
    const val MAX_ONE_TIME_RUPEES: Long = 1_000_000_000_000L

    /** Max monthly amount: ₹10 crore. */
    const val MAX_MONTHLY_RUPEES: Long = 100_000_000L

    const val MIN_YEARS: Int = 0
    const val MAX_YEARS_UNTIL_REQUIRED: Int = 80
    const val MAX_DURATION_YEARS: Int = 80
    const val MAX_MODELLING_HORIZON_YEARS: Int = 80
}
