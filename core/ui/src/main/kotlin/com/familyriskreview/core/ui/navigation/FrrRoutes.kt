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
 */
object FrrRoutes {
    @Serializable
    data object Splash

    @Serializable
    data object Dashboard

    @Serializable
    data object Welcome

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
