package com.familyriskreview.core.ui.navigation

/**
 * Top-level navigation destinations for the Phase 0 shell.
 * Feature modules own their internal nested graphs from Phase 3 onward.
 */
object FrrRoutes {
    const val SPLASH = "splash"
    const val DASHBOARD = "dashboard"
    const val REVIEW = "review/{reviewId}"
    const val SUMMARY = "summary/{reviewId}"
    const val SETTINGS = "settings"

    fun review(reviewId: String): String = "review/$reviewId"
    fun summary(reviewId: String): String = "summary/$reviewId"
}
