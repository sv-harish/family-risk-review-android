package com.familyriskreview.core.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe Navigation Compose destinations.
 *
 * Architecture rule: UI and editing state must survive expected Activity
 * recreation through saved state and persisted domain state, not by
 * preventing configuration changes.
 *
 * Splash and Welcome are app-shell destinations — they are never persisted
 * as [com.familyriskreview.core.model.ReviewStep].
 *
 * Phase 2: [Summary] remains defined for later phases but must not be entered
 * from the Phase 2 journey boundary.
 */
object FrrRoutes {
    @Serializable
    data object Splash

    @Serializable
    data object Dashboard

    /**
     * Introduction / confirmation before review creation.
     * [mode] is the preferred mode from Dashboard (`QUICK` / `GUIDED`); null shows both equally.
     */
    @Serializable
    data class Welcome(
        val mode: String? = null,
    )

    @Serializable
    data class Review(
        val reviewId: String,
    )

    @Serializable
    data class Summary(
        val reviewId: String,
    )

    @Serializable
    data object Settings
}
